const fs = require('fs');

const data = JSON.parse(fs.readFileSync('date_api_response.json', 'utf8'));

if (data && data.siteCompanies) {
    data.siteCompanies.forEach(company => {
        company.siteGames.forEach(siteGame => {
            if (siteGame.game && siteGame.game.sessions && siteGame.game.sessions.length > 0) {
                const session = siteGame.game.sessions[0];
                console.log(`${company.title} - ${siteGame.title}: score = ${JSON.stringify(session.score)}`);
            }
        });
    });
}
