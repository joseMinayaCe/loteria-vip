const fs = require('fs');

const websiteData = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

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

console.log(`Website Companies: ${websiteCompanies.length}`);
if (websiteCompanies.length > 0) {
    const firstCompany = websiteCompanies[0];
    console.log('Company Keys:', Object.keys(firstCompany));
    console.log('Company Title:', firstCompany.title);
    
    const games = firstCompany.siteGames || [];
    console.log(`Games Count: ${games.length}`);
    if (games.length > 0) {
        const firstGame = games[0];
        console.log('Game Keys:', Object.keys(firstGame));
        console.log('Game Title:', firstGame.title);
        
        // Let's print the entire firstGame object
        console.log('Game Object JSON:', JSON.stringify(firstGame, null, 2));
    }
}
