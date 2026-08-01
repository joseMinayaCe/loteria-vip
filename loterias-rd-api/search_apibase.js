const fs = require('fs');

const html = fs.readFileSync('page.html', 'utf8');

const idx = html.indexOf('apiBase');
if (idx !== -1) {
    console.log('Found apiBase in page.html:', html.substring(idx - 100, idx + 100));
} else {
    console.log('apiBase not found in page.html');
}

// Let's search for "api" in window.__NUXT__ or similar
const nuxtIdx = html.indexOf('__NUXT__');
if (nuxtIdx !== -1) {
    console.log('Found __NUXT__ in page.html:', html.substring(nuxtIdx - 50, nuxtIdx + 500));
}
