const fs = require('fs');

const resolved = JSON.parse(fs.readFileSync('resolved_nuxt.json', 'utf8'));
const main = resolved.pinia.main;

console.log('siteForDate:', JSON.stringify(main.siteForDate).substring(0, 500));
console.log('\nfeed keys/type:', typeof main.feed, Array.isArray(main.feed) ? `array length ${main.feed.length}` : 'not array');
if (main.feed) {
    if (Array.isArray(main.feed)) {
        console.log('first feed items:', JSON.stringify(main.feed.slice(0, 3)).substring(0, 500));
    } else {
        console.log('feed keys:', Object.keys(main.feed));
        console.log('feed sample:', JSON.stringify(main.feed).substring(0, 500));
    }
}
