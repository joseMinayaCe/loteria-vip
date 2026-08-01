const fs = require('fs');

const resolved = JSON.parse(fs.readFileSync('resolved_nuxt.json', 'utf8'));
const main = resolved.pinia.main;

const company = main.site[1][1].siteCompanies[0];
console.log('Company Name:', company.title);
console.log('Number of Games:', company.siteGames.length);

console.log('First Game Keys:', Object.keys(company.siteGames[0]));
console.log('First Game title:', company.siteGames[0].title);
console.log('First Game game keys:', Object.keys(company.siteGames[0].game));
console.log('First Game game validation:', company.siteGames[0].game.validation);
console.log('First Game game sessions:', company.siteGames[0].game.sessions);
console.log('First Game game display:', company.siteGames[0].game.display);
console.log('First Game game score_layout:', company.siteGames[0].game.score_layout);
