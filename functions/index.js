const { onRequest } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

// Company and Category Mapping
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

/**
 * Scrapes loteriasdominicanas.com HTML and extracts current draws & numbers
 */
async function scrapeCurrentResults() {
  const url = "https://loteriasdominicanas.com/";
  const response = await axios.get(url, {
    headers: {
      "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
      "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      "Accept-Language": "es-ES,es;q=0.9,en;q=0.8"
    },
    timeout: 15000
  });

  const html = response.data;
  const results = [];

  // Match company blocks
  const companyBlockRegex = /<div[^>]*class="[^"]*company-block-(\d+)[^"]*"[^>]*>([\s\S]*?)<\/div>\s*<\/div>\s*<\/div>/gi;
  let blockMatch;

  while ((blockMatch = companyBlockRegex.exec(html)) !== null) {
    const companyId = blockMatch[1];
    const blockHtml = blockMatch[2];

    // Extract company title
    const titleMatch = /<a[^>]*class="[^"]*company-title[^"]*"[^>]*>([\s\S]*?)<\/a>/i.exec(blockHtml);
    const companyTitle = titleMatch ? titleMatch[1].replace(/<[^>]+>/g, "").trim() : `Lotería ${companyId}`;

    // Extract game blocks within company
    const gameRegex = /<div[^>]*class="[^"]*game-block[^"]*"[^>]*>([\s\S]*?)<\/div>\s*<\/div>\s*<\/div>/gi;
    let gameMatch;

    while ((gameMatch = gameRegex.exec(blockHtml)) !== null) {
      const gameHtml = gameMatch[1];

      // Draw title
      const drawTitleMatch = /<a[^>]*class="[^"]*game-title[^"]*"[^>]*>([\s\S]*?)<\/a>|<span[^>]*class="[^"]*game-title[^"]*"[^>]*>([\s\S]*?)<\/span>/i.exec(gameHtml);
      const drawTitle = drawTitleMatch ? (drawTitleMatch[1] || drawTitleMatch[2]).replace(/<[^>]+>/g, "").trim() : companyTitle;

      // Date
      const dateMatch = /<span[^>]*class="[^"]*game-date[^"]*"[^>]*>([\s\S]*?)<\/span>/i.exec(gameHtml);
      const drawDate = dateMatch ? dateMatch[1].replace(/<[^>]+>/g, "").trim() : "";

      // Scores / Numbers
      const scoresMatch = /<div[^>]*class="[^"]*game-scores[^"]*"[^>]*>([\s\S]*?)<\/div>/i.exec(gameHtml);
      const numbers = [];
      if (scoresMatch) {
        const scoreHtml = scoresMatch[1];
        const numRegex = /<span[^>]*class="[^"]*score[^"]*"[^>]*>([\s\S]*?)<\/span>/gi;
        let numMatch;
        while ((numMatch = numRegex.exec(scoreHtml)) !== null) {
          const numStr = numMatch[1].replace(/<[^>]+>/g, "").trim();
          if (numStr && !isNaN(parseInt(numStr, 10))) {
            numbers.push(parseInt(numStr, 10).toString().padStart(2, "0"));
          }
        }
      }

      if (numbers.length > 0) {
        results.push({
          companyId,
          companyTitle,
          drawTitle,
          drawDate,
          numbers,
          // Unique key for deduplication
          uniqueId: `${companyId}_${drawTitle}_${drawDate}_${numbers.join("-")}`
            .toLowerCase()
            .replace(/[^a-z0-9_]/g, "_")
        });
      }
    }
  }

  return results;
}

/**
 * Main execution function: Checks for new lottery results and sends FCM notifications
 */
async function processLotteryNotifications() {
  console.log("Checking lottery results for notifications...");
  const results = await scrapeCurrentResults();
  console.log(`Found ${results.length} total lottery results on page.`);

  let newNotificationsSent = 0;

  for (const item of results) {
    const docRef = db.collection("sent_notifications").doc(item.uniqueId);
    const docSnap = await docRef.get();

    if (!docSnap.exists) {
      const categoryKey = getCategoryKey(item.companyTitle + " " + item.drawTitle);
      const topicName = `loteria-${categoryKey}`;

      const title = `🎰 ${item.drawTitle || item.companyTitle}`;
      const body = `Números ganadores: ${item.numbers.join(" - ")}`;
      const numbersJson = JSON.stringify(item.numbers.map(n => parseInt(n, 10)));

      console.log(`New result detected for [${item.drawTitle}]: ${item.numbers.join(" - ")}. Sending FCM...`);

      // 1. Send FCM message to 'loteria-all' and topic-specific
      const message = {
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
          drawDate: item.drawDate || ""
        },
        android: {
          priority: "high"
        }
      };

      try {
        const response = await admin.messaging().send(message);
        console.log(`FCM notification sent successfully: ${response}`);

        // Mark as sent in Firestore
        await docRef.set({
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          companyTitle: item.companyTitle,
          drawTitle: item.drawTitle,
          numbers: item.numbers,
          categoryKey: categoryKey,
          fcmMessageId: response
        });

        newNotificationsSent++;
      } catch (err) {
        console.error(`Failed to send FCM message for ${item.uniqueId}:`, err);
      }
    }
  }

  return { totalScraped: results.length, newNotificationsSent };
}

// Scheduled Function: Runs every 5 minutes automatically
exports.checkLotteryResults = onSchedule("every 5 minutes", async (event) => {
  try {
    const res = await processLotteryNotifications();
    console.log("Scheduled check complete:", res);
  } catch (err) {
    console.error("Scheduled check error:", err);
  }
});

// HTTP Function: Manual trigger for testing
exports.checkResultsNow = onRequest(async (req, res) => {
  try {
    const result = await processLotteryNotifications();
    res.status(200).json({
      success: true,
      message: "Check completed successfully",
      data: result
    });
  } catch (err) {
    res.status(500).json({
      success: false,
      error: err.message
    });
  }
});
