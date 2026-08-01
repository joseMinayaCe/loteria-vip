const fs = require('fs');

const js = fs.readFileSync('entry.js', 'utf8');

// Find all occurrences of "api("
const regex = /api\([^\)]+\)/g;
const matches = js.match(regex) || [];
console.log('API calls found:', Array.from(new Set(matches)));

// Let's search for strings containing /site, /feed, /game, /results
const strRegex = /["'`]\/[^"'`]+["'`]/g;
const strMatches = js.match(strRegex) || [];
const filteredStrMatches = strMatches.filter(s => s.includes('feed') || s.includes('site') || s.includes('game') || s.includes('results') || s.includes('stats'));
console.log('Filtered string matches:', Array.from(new Set(filteredStrMatches)));

// Search for apiBase in entry.js
const idx = js.indexOf('apiBase');
if (idx !== -1) {
    console.log('Found apiBase around:', js.substring(idx - 100, idx + 100));
} else {
    console.log('apiBase not found as literal word');
}
