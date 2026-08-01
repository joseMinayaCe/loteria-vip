const fs = require('fs');

const data = JSON.parse(fs.readFileSync('api_response.json', 'utf8'));

const companies = data.siteCompanies || [];

console.log(`Number of companies: ${companies.length}`);

for (const company of companies) {
    const companyTitle = company.title;
    const siteGames = company.siteGames || [];
    console.log(`\nCompany: ${companyTitle} (${siteGames.length} games)`);
    
    for (const siteGame of siteGames) {
        const gameTitle = siteGame.title;
        const game = siteGame.game || {};
        const sessions = game.sessions || [];
        console.log(`  - Game: ${gameTitle} (${sessions.length} sessions)`);
        
        for (const session of sessions) {
            console.log(`    - Session date: ${session.date}`);
            console.log(`    - Session score:`, JSON.stringify(session.score));
            console.log(`    - Session status:`, session.status);
        }
    }
}
