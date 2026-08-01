const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

// The hash might change, so let's get the first key of decoded.data['1']
const firstKey = Object.keys(decoded.data['1'])[0];
console.log('First key of data[1]:', firstKey);

const mainObj = decoded.data['1'][firstKey];
console.log('Main Object Keys:', Object.keys(mainObj));

// Let's print siteCompanies or any other key
if (mainObj.siteCompanies) {
    console.log('siteCompanies is array of length:', mainObj.siteCompanies.length);
    mainObj.siteCompanies.forEach((company, idx) => {
        console.log(`\nCompany ${idx}:`, company.title);
        console.log(`  siteGames length:`, company.siteGames.length);
        company.siteGames.forEach((siteGame, gIdx) => {
            console.log(`    Game ${gIdx}:`, siteGame.title);
            console.log(`      game sessions:`, siteGame.game.sessions);
            console.log(`      game score_layout:`, JSON.stringify(siteGame.game.score_layout));
        });
    });
}
