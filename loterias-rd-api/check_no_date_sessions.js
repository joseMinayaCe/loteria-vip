const fs = require('fs');

const data = JSON.parse(fs.readFileSync('real_api_response.json', 'utf8'));

let sessionCount = 0;
if (data && data.siteCompanies) {
    data.siteCompanies.forEach(c => {
        c.siteGames.forEach(g => {
            if (g.game && g.game.sessions && g.game.sessions.length > 0) {
                sessionCount += g.game.sessions.length;
            }
        });
    });
}
console.log(`No date parameter -> Found ${sessionCount} sessions.`);
