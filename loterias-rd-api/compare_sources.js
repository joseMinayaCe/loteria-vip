const fs = require('fs');

// Load API response (from api.loteriasdominicanas.com)
const apiData = JSON.parse(fs.readFileSync('api_response.json', 'utf8'));

// Load decoded payload from live website (loteriasdominicanas.com)
const websiteData = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

console.log("=== API Source Info ===");
const apiCompanies = apiData.siteCompanies || [];
console.log(`API Companies Count: ${apiCompanies.length}`);

// Get a few games and check their session dates and scores
function printSample(sourceName, companies) {
    console.log(`\nSample from ${sourceName}:`);
    let printed = 0;
    for (const company of companies) {
        if (printed >= 3) break;
        const games = company.siteGames || [];
        for (const game of games) {
            if (printed >= 3) break;
            const sessions = game.game ? (game.game.sessions || []) : [];
            if (sessions.length > 0) {
                console.log(`  - Company: ${company.title}, Game: ${game.title}`);
                console.log(`    Session Date: ${sessions[0].date}`);
                console.log(`    Score:`, JSON.stringify(sessions[0].score));
                printed++;
            }
        }
    }
}

printSample("REST API", apiCompanies);

// Find siteCompanies in websiteData
let websiteCompanies = [];
if (websiteData && websiteData.data) {
    for (const key in websiteData.data) {
        const item = websiteData.data[key];
        for (const subKey in item) {
            if (item[subKey] && item[subKey].siteCompanies) {
                websiteCompanies = item[subKey].siteCompanies;
                break;
            }
        }
        if (websiteCompanies.length > 0) break;
    }
}

console.log("\n=== Website Source Info ===");
console.log(`Website Companies Count: ${websiteCompanies.length}`);
if (websiteCompanies.length > 0) {
    printSample("Website Payload", websiteCompanies);
} else {
    console.log("No siteCompanies found in Website Payload!");
}
