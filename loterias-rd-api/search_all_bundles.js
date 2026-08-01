const axios = require('axios');
const fs = require('fs');

const scripts = [
    'entry.UrIeivEx.1781446402873.js',
    'dominicana.BwD7PbCk.1781446402873.js',
    'index.Csz8GryA.1781446402873.js',
    'index.CRjOk1Wa.1781446402873.js',
    'index.Cly5UHwY.1781446402873.js',
    'index.CLs7nh7g.1781446402873.js',
    'index.-6oELgr_.1781446402873.js',
    'index.DJSjN39O.1781446402873.js'
];

async function testFetch() {
    for (const scriptName of scripts) {
        try {
            const url = `https://loteriasdominicanas.com/_nuxt/${scriptName}`;
            console.log('\n--- Checking script:', scriptName, '---');
            const { data: js } = await axios.get(url, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
                }
            });
            
            // Search for strings containing api, url, s3, fetch, axios, feed, session
            const regexes = [
                /https?:\/\/[^\s"'`]+/g,
                /\/api\/[^\s"'`]+/g,
                /fetch\([^\)]+\)/g,
                /["'`][^"'`]*api[^"'`]*["'`]/gi,
            ];
            
            regexes.forEach(regex => {
                const matches = js.match(regex);
                if (matches) {
                    console.log(`  Matches for ${regex}:`, Array.from(new Set(matches)).slice(0, 10));
                }
            });
        } catch (e) {
            console.error('Error fetching/processing:', scriptName, e.message);
        }
    }
}

testFetch();
