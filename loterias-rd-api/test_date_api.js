const axios = require('axios');
const fs = require('fs');

async function testFetch() {
    try {
        // Let's use today's or yesterday's date in ISO format
        const todayIso = new Date().toISOString();
        const url = `https://api.loteriasdominicanas.com/dominicana/sites/env?date=${todayIso}`;
        console.log('Fetching from date API:', url);
        const { data } = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
        });
        fs.writeFileSync('date_api_response.json', JSON.stringify(data, null, 2));
        console.log('Saved response to date_api_response.json');
        
        // Let's search inside the response for non-empty sessions
        let foundSessions = 0;
        if (data && data.siteCompanies) {
            data.siteCompanies.forEach(company => {
                company.siteGames.forEach(siteGame => {
                    if (siteGame.game && siteGame.game.sessions && siteGame.game.sessions.length > 0) {
                        foundSessions++;
                        console.log(`\nGame: ${siteGame.title} (Company: ${company.title})`);
                        console.log(`Sessions count: ${siteGame.game.sessions.length}`);
                        console.log(`First Session:`, JSON.stringify(siteGame.game.sessions[0]));
                    }
                });
            });
        }
        console.log(`\nFound ${foundSessions} games with non-empty sessions.`);
    } catch (e) {
        console.error('Error fetching date API:', e.message);
    }
}

testFetch();
