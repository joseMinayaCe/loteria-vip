const fs = require("fs");
const path = require("path");
const axios = require("axios");
const { GoogleAuth } = require("google-auth-library");

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || "web-to-app-loteria";
const STATE_FILE = path.join(__dirname, "sent_draws.json");

// Category mapping helper
const CATEGORY_MAP = [
  { keywords: ["gana mas", "nacional"], key: "gana-mas" },
  { keywords: ["leidsa"], key: "leidsa" },
  { keywords: ["real"], key: "real" },
  { keywords: ["primera"], key: "la-primera" },
  { keywords: ["loteka"], key: "loteka" },
  { keywords: ["anguila", "anguilla"], key: "anguila" },
  { keywords: ["king"], key: "king-lottery" },
  { keywords: ["suerte"], key: "la-suerte" },
  { keywords: ["lotedom"], key: "lotedom" },
  { keywords: ["york", "ny"], key: "new-york" },
  { keywords: ["florida"], key: "florida" },
  { keywords: ["haiti", "bolet"], key: "haiti-bolet" }
];

function getCategoryKey(title) {
  const lower = title.toLowerCase();
  for (const item of CATEGORY_MAP) {
    if (item.keywords.some(kw => lower.includes(kw))) {
      return item.key;
    }
  }
  return "gana-mas";
}

function loadSentDraws() {
  if (fs.existsSync(STATE_FILE)) {
    try {
      const data = fs.readFileSync(STATE_FILE, "utf8");
      return new Set(JSON.parse(data));
    } catch (e) {
      console.warn("Could not parse state file, initializing fresh set.");
    }
  }
  return new Set();
}

function saveSentDraws(sentSet) {
  try {
    const list = Array.from(sentSet);
    // Keep only last 300 items
    const trimmed = list.slice(Math.max(0, list.length - 300));
    fs.writeFileSync(STATE_FILE, JSON.stringify(trimmed, null, 2), "utf8");
  } catch (e) {
    console.error("Failed to save state file:", e.message);
  }
}

/**
 * Gets FCM OAuth Access Token using Google Auth Library or Refresh Token
 */
async function getAccessToken() {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_JSON) {
    const auth = new GoogleAuth({
      credentials: JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON),
      scopes: ["https://www.googleapis.com/auth/firebase.messaging"]
    });
    const client = await auth.getClient();
    const tokenResponse = await client.getAccessToken();
    return tokenResponse.token;
  }

  if (process.env.FIREBASE_REFRESH_TOKEN && process.env.FIREBASE_CLIENT_ID && process.env.FIREBASE_CLIENT_SECRET) {
    const resp = await axios.post("https://oauth2.googleapis.com/token", new URLSearchParams({
      client_id: process.env.FIREBASE_CLIENT_ID,
      client_secret: process.env.FIREBASE_CLIENT_SECRET,
      refresh_token: process.env.FIREBASE_REFRESH_TOKEN,
      grant_type: "refresh_token"
    }).toString(), {
      headers: { "Content-Type": "application/x-www-form-urlencoded" }
    });
    return resp.data.access_token;
  }

  const auth = new GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"]
  });
  const client = await auth.getClient();
  const tokenResponse = await client.getAccessToken();
  return tokenResponse.token;
}

/**
 * Fetches current draws from LoteriasDominicanas API
 */
async function fetchLotteries() {
  const dateStr = new Date().toISOString();
  const apiUrl = `https://api.loteriasdominicanas.com/dominicana/sites/env?date=${encodeURIComponent(dateStr)}`;
  
  const response = await axios.get(apiUrl, {
    headers: {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
      "Accept": "application/json"
    },
    timeout: 15000
  });

  const results = [];
  const root = response.data;
  const companies = root.siteCompanies || [];

  for (const company of companies) {
    const companyTitle = company.title || "";
    const siteGames = company.siteGames || [];

    for (const siteGame of siteGames) {
      const gameTitle = siteGame.title || companyTitle;
      const game = siteGame.game || {};
      const sessions = game.sessions || [];

      for (const session of sessions) {
        const scoreArr = session.score || [];
        const numbers = [];

        for (const inner of scoreArr) {
          if (Array.isArray(inner)) {
            for (const val of inner) {
              const parsed = parseInt(val, 10);
              if (!isNaN(parsed)) {
                numbers.push(parsed.toString().padStart(2, "0"));
              }
            }
          }
        }

        if (numbers.length > 0) {
          const sessionDate = session.date ? session.date.substring(0, 10) : "";
          const uniqueId = `${companyTitle}_${gameTitle}_${sessionDate}_${numbers.join("-")}`
            .toLowerCase()
            .replace(/[^a-z0-9_]/g, "_");

          results.push({
            companyTitle,
            drawTitle: gameTitle,
            date: sessionDate,
            numbers,
            uniqueId
          });
        }
      }
    }
  }

  return results;
}

/**
 * Main Execution
 */
async function main() {
  console.log(`[${new Date().toISOString()}] Checking lottery results...`);
  const sentDraws = loadSentDraws();
  const results = await fetchLotteries();
  console.log(`Parsed ${results.length} draws with numbers available.`);

  let newNotifications = 0;
  let accessToken = null;

  for (const item of results) {
    if (!sentDraws.has(item.uniqueId)) {
      if (!accessToken) {
        try {
          accessToken = await getAccessToken();
        } catch (err) {
          console.error("Could not obtain access token:", err.message);
          process.exit(1);
        }
      }

      const categoryKey = getCategoryKey(item.companyTitle + " " + item.drawTitle);
      const title = `🎰 ${item.drawTitle || item.companyTitle}`;
      const body = `Números ganadores: ${item.numbers.join(" - ")}`;
      const numbersJson = JSON.stringify(item.numbers.map(n => parseInt(n, 10)));

      console.log(`🔔 Sending notification for: ${item.drawTitle} [${item.numbers.join(" - ")}]`);

      const payload = {
        message: {
          topic: "loteria-all",
          notification: {
            title: title,
            body: body
          },
          data: {
            title: title,
            body: body,
            lotteryId: categoryKey,
            numbers: numbersJson,
            drawDate: item.date || ""
          },
          android: {
            priority: "high"
          }
        }
      };

      try {
        const resp = await axios.post(
          `https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send`,
          payload,
          {
            headers: {
              "Authorization": `Bearer ${accessToken}`,
              "Content-Type": "application/json"
            }
          }
        );
        console.log(`✅ FCM Response:`, resp.data.name);

        sentDraws.add(item.uniqueId);
        newNotifications++;
      } catch (fcmErr) {
        console.error(`❌ FCM error for ${item.uniqueId}:`, fcmErr.response ? fcmErr.response.data : fcmErr.message);
      }
    }
  }

  saveSentDraws(sentDraws);
  console.log(`Done. Total new notifications sent: ${newNotifications}`);
}

main().catch(err => {
  console.error("Unhandled error:", err);
  process.exit(1);
});
