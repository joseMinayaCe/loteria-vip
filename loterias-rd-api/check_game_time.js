const fs = require('fs');

const data = JSON.parse(fs.readFileSync('real_api_response.json', 'utf8'));

if (data && data.siteCompanies) {
    const game = data.siteCompanies[0].siteGames[0];
    console.log('Game keys:', Object.keys(game));
    console.log('Game content:', JSON.stringify(game.content));
    console.log('Game display:', JSON.stringify(game.display));
}
