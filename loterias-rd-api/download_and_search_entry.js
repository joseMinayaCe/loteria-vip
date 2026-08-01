const axios = require('axios');
const fs = require('fs');

async function testFetch() {
    try {
        const url = 'https://loteriasdominicanas.com/_nuxt/entry.UrIeivEx.1781446402873.js';
        console.log('Downloading entry JS...');
        const { data: js } = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
        });
        fs.writeFileSync('entry.js', js);
        console.log('Saved entry.js');

        // Let's find all calls to $api
        const matches = [];
        const regex = /api\(\"[^\"]+\"/g;
        let match;
        while ((match = regex.exec(js)) !== null) {
            matches.push(match[0]);
        }
        console.log('API calls in entry.js:', Array.from(new Set(matches)));
        
        // Let's also look for public.apiBase or apiBase
        const apiBaseRegex = /apiBase:[^,}]+/g;
        console.log('apiBase definitions:', js.match(apiBaseRegex));

    } catch (e) {
        console.error('Error:', e);
    }
}

testFetch();
