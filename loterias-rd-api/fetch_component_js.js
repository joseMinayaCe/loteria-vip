const axios = require('axios');
const fs = require('fs');

async function testFetch() {
    try {
        const url = 'https://loteriasdominicanas.com/_nuxt/feed.vue.Ba9V797X.1781446402873.js';
        console.log('Fetching JS from:', url);
        const { data: js } = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
        });
        fs.writeFileSync('feed.js', js);
        console.log('Saved feed.js');
        
        // Search for any fetch/axios/api endpoints
        const regexes = [
            /https?:\/\/[^\s"'`]+/g,
            /\/api\/[^\s"'`]+/g,
            /fetch\([^\)]+\)/g,
            /useFetch\([^\)]+\)/g,
            /useAsyncData\([^\)]+\)/g,
            /["'`][^"'`]*api[^"'`]*["'`]/gi,
        ];
        
        regexes.forEach(regex => {
            const matches = js.match(regex);
            if (matches) {
                console.log(`\nMatches for ${regex}:`, Array.from(new Set(matches)).slice(0, 30));
            } else {
                console.log(`No matches for ${regex}`);
            }
        });
    } catch (e) {
        console.error('Error:', e);
    }
}

testFetch();
