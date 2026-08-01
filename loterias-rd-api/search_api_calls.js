const fs = require('fs');

const html = fs.readFileSync('page.html', 'utf8');

// Search for any urls, api paths, or javascript code that fetches
const regexes = [
    /https?:\/\/[^\s"'`]+/g,
    /\/api\/[^\s"'`]+/g,
    /fetch\([^\)]+\)/g,
];

regexes.forEach(regex => {
    const matches = html.match(regex);
    if (matches) {
        console.log(`\nMatches for ${regex}:`, Array.from(new Set(matches)).slice(0, 30));
    } else {
        console.log(`No matches for ${regex}`);
    }
});
