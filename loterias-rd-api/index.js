const express = require('express');
const axios = require('axios');
const cors = require('cors');
const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');
const cron = require('node-cron');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

const OAUTH_REFRESH_TOKEN = "1//05q1gFV5WscBACgYIARAAGAUSNwF-L9Irxd_C4aHVGAmk0sX9gdbraxD1y8jJnXR4TguWOCFvJHHtG6qdG_yH6nX0jb3g5Po2UiM";
const OAUTH_CLIENT_ID = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com";
const OAUTH_CLIENT_SECRET = "j9iVZfS8kkCEFUPaAeJV0sAi";
const FIREBASE_PROJECT_ID = "web-to-app-loteria";

let cachedAccessToken = null;
let tokenExpiryTime = 0;

async function getAccessToken() {
    if (cachedAccessToken && Date.now() < tokenExpiryTime - 60000) {
        return cachedAccessToken;
    }
    try {
        const response = await axios.post('https://oauth2.googleapis.com/token', {
            client_id: OAUTH_CLIENT_ID,
            client_secret: OAUTH_CLIENT_SECRET,
            grant_type: 'refresh_token',
            refresh_token: OAUTH_REFRESH_TOKEN
        });
        cachedAccessToken = response.data.access_token;
        tokenExpiryTime = Date.now() + (response.data.expires_in * 1000);
        return cachedAccessToken;
    } catch (error) {
        console.error('Error refreshing FCM OAuth2 token:', error.response ? error.response.data : error.message);
        throw error;
    }
}

function getNotificationTopic(lotteryId, lotteryName) {
    const id = (lotteryId || '').toLowerCase().replace(/í/g, 'i').replace(/á/g, 'a');
    const name = (lotteryName || '').toLowerCase().replace(/í/g, 'i').replace(/á/g, 'a');
    if (id.includes('gana') || id.includes('nacional') || name.includes('nacional') || name.includes('gana m')) return 'loteria-gana-mas';
    if (id.includes('leidsa') || name.includes('leidsa')) return 'loteria-leidsa';
    if (id.includes('real') || name.includes('real')) return 'loteria-real';
    if (id.includes('primera') || name.includes('primera')) return 'loteria-la-primera';
    if (id.includes('loteka') || name.includes('loteka')) return 'loteria-loteka';
    if (id.includes('anguil') || name.includes('anguil')) return 'loteria-anguila';
    if (id.includes('king') || name.includes('king')) return 'loteria-king-lottery';
    if (id.includes('suerte') || name.includes('suerte')) return 'loteria-la-suerte';
    if (id.includes('lotedom') || name.includes('lotedom')) return 'loteria-lotedom';
    if (id.includes('york') || id.includes('ny') || name.includes('york')) return 'loteria-new-york';
    if (id.includes('florida') || name.includes('florida')) return 'loteria-florida';
    if (id.includes('haiti') || id.includes('bolet') || name.includes('haiti') || name.includes('bolet')) return 'loteria-haiti-bolet';
    
    const cleanId = id.replace(/[^a-zA-Z0-9-_.~%]/g, '-').replace(/-+/g, '-');
    return `loteria-${cleanId}`;
}

async function sendPushNotification(lotteryId, lotteryName, numbers) {
    const firstNumber = (numbers && numbers.length > 0) ? numbers[0] : '?';
    const topic = getNotificationTopic(lotteryId, lotteryName);

    const topicsToSend = [topic];
    if (topic !== 'loteria-all') {
        topicsToSend.push('loteria-all');
    }

    try {
        const token = await getAccessToken();
        const url = `https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`;
        
        const title = `¡Resultado de ${lotteryName}! 🔔`;
        const body = `Primer número: ${firstNumber}. ¡Entra para ver el resultado completo!`;

        for (const t of topicsToSend) {
            try {
                const message = {
                    message: {
                        topic: t,
                        notification: {
                            title: title,
                            body: body
                        },
                        data: {
                            lotteryId: lotteryId || '',
                            title: title,
                            body: body,
                            numbers: JSON.stringify(numbers || [])
                        }
                    }
                };

                const response = await axios.post(url, message, {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                });
                console.log(`[FCM SUCCESS] Sent push for ${lotteryName} to topic '${t}':`, response.data.name);
            } catch (err) {
                console.error(`[FCM ERROR] Failed push for ${lotteryName} to topic '${t}':`, err.response ? err.response.data : err.message);
            }
        }
    } catch (error) {
        console.error(`[FCM ERROR] Failed push for ${lotteryName}:`, error.message || error);
    }
}

// Company titles that map to the AMERICANA category
const AMERICANA_COMPANIES = ['americanas', 'nueva york', 'florida', 'king lottery'];

/**
 * Converts a date string from dd-MM-yyyy format to ISO-8601 UTC string.
 * Returns current date ISO string if no date is provided or parsing fails.
 */
function parseInputDate(dateStr) {
    if (!dateStr) return new Date().toISOString();
    const parts = dateStr.split('-');
    if (parts.length !== 3) return new Date().toISOString();
    const [day, month, year] = parts;
    const parsed = new Date(`${year}-${month}-${day}T00:00:00Z`);
    if (isNaN(parsed.getTime())) return new Date().toISOString();
    return parsed.toISOString();
}

/**
 * Formats an ISO date string to dd-MM-yyyy.
 */
function formatDateDDMMYYYY(isoDate) {
    const d = new Date(isoDate);
    if (isNaN(d.getTime())) return isoDate;
    const day = String(d.getUTCDate()).padStart(2, '0');
    const month = String(d.getUTCMonth() + 1).padStart(2, '0');
    const year = d.getUTCFullYear();
    return `${day}-${month}-${year}`;
}

/**
 * Extracts a time string (e.g. "12:30") from a game title, if present.
 */
function extractTimeFromTitle(title) {
    const match = title.match(/(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?)/);
    return match ? match[1].trim() : null;
}

/**
 * Determines the category for a game based on game title and company title.
 */
function getCategory(gameTitle, companyTitle) {
    const gameLower = gameTitle.toLowerCase();
    if (gameLower.includes('loto') || gameLower.includes('lotto')) {
        return 'LOTTO';
    }
    const companyLower = companyTitle.toLowerCase();
    if (AMERICANA_COMPANIES.includes(companyLower)) {
        return 'AMERICANA';
    }
    return 'LOTERIA';
}

async function scrapeLoterias(date) {
    try {
        const isoDate = parseInputDate(date);
        const url = `https://api.loteriasdominicanas.com/dominicana/sites/env?date=${encodeURIComponent(isoDate)}`;

        const { data } = await axios.get(url, {
            headers: {
                'Accept': 'application/json',
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
            }
        });

        const results = [];
        const companies = (data && data.siteCompanies) || [];

        for (const company of companies) {
            const companyTitle = company.title || '';
            const siteGames = company.siteGames || [];

            for (const siteGame of siteGames) {
                const gameTitle = siteGame.title || '';
                const game = siteGame.game || {};
                const sessions = (game.sessions) || [];

                for (const session of sessions) {
                    const scoreArrays = session.score || [];
                    // Flatten all score arrays and parse to integers
                    const numbers = [];
                    for (const scoreGroup of scoreArrays) {
                        if (Array.isArray(scoreGroup)) {
                            for (const val of scoreGroup) {
                                const parsed = parseInt(val, 10);
                                if (!isNaN(parsed)) {
                                    numbers.push(parsed);
                                }
                            }
                        }
                    }

                    // Skip games with no valid numbers
                    if (numbers.length === 0) continue;

                    const id = `${companyTitle}-${gameTitle}`.toLowerCase().replace(/\s+/g, '-');
                    const drawTime = extractTimeFromTitle(gameTitle) || formatDateDDMMYYYY(session.date || isoDate);
                    const drawDate = formatDateDDMMYYYY(session.date || isoDate);
                    const category = getCategory(gameTitle, companyTitle);

                    results.push({
                        id,
                        name: gameTitle,
                        drawTime,
                        drawDate,
                        numbers,
                        category,
                        isVerified: true
                    });
                }
            }
        }

        const uniqueResults = [];
        const seenKeys = new Set();
        for (const res of results) {
            const key = `${res.id}-${res.drawTime}-${res.drawDate}-${res.numbers.join(',')}`;
            if (!seenKeys.has(key)) {
                seenKeys.add(key);
                uniqueResults.push(res);
            }
        }

        return uniqueResults;
    } catch (error) {
        console.error('API fetch error:', error.message || error);
        return [];
    }
}

app.get('/api/results', async (req, res) => {
    const { date } = req.query;
    const results = await scrapeLoterias(date);
    if (results.length === 0) {
        // Fallback to some mock data if scraping fails
        return res.json([
            { id: "gana-mas", name: "Gana Más (Mock)", drawTime: "2:30 PM", drawDate: date || "Hoy", numbers: [34, 12, 89], category: "LOTERIA", isVerified: true },
            { id: "nacional", name: "Nacional (Mock)", drawTime: "9:00 PM", drawDate: date || "Hoy", numbers: [5, 76, 21], category: "LOTERIA", isVerified: false }
        ]);
    }
    res.json(results);
});

app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});

// Cache and background check logic for push notifications
const resultsCache = {};
let cachePopulated = false;

async function checkNewResults() {
    console.log(`[${new Date().toLocaleTimeString()}] Running background scraping check...`);
    const results = await scrapeLoterias();
    if (!results || results.length === 0) return;

    results.forEach(result => {
        const prev = resultsCache[result.id];
        if (result.numbers && result.numbers.length > 0) {
            const numbersStr = result.numbers.join(',');
            if (!prev) {
                if (cachePopulated) {
                    console.log(`[NEW RESULT DETECTED] ${result.name} (${result.id}) -> ${numbersStr}`);
                    sendPushNotification(result.id, result.name, result.numbers);
                }
                resultsCache[result.id] = result;
            } else if (prev.numbers && prev.numbers.join(',') !== numbersStr) {
                console.log(`[UPDATED RESULT DETECTED] ${result.name} (${result.id}) -> ${numbersStr}`);
                sendPushNotification(result.id, result.name, result.numbers);
                resultsCache[result.id] = result;
            }
        }
    });
}

// Populate cache on startup to prevent spamming notifications for old results
scrapeLoterias().then(initialResults => {
    initialResults.forEach(r => {
        resultsCache[r.id] = r;
    });
    cachePopulated = true;
    console.log(`Initial cache populated with ${initialResults.length} lottery results. Push notifications active.`);
}).catch(err => {
    console.error('Failed to populate initial cache:', err);
    cachePopulated = true;
});

// Run background checker every 30 seconds
cron.schedule('*/30 * * * * *', checkNewResults);
