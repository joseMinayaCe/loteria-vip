const axios = require('axios');
const fs = require('fs');

async function test() {
    try {
        const url = 'https://api.loteriasdominicanas.com/dominicana/sites/env?date=2026-06-14T00:00:00.000Z';
        console.log('Fetching:', url);
        const res = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
            }
        });
        fs.writeFileSync('api_response.json', JSON.stringify(res.data, null, 2));
        console.log('Saved response to api_response.json');
        
        console.log('Top level keys:', Object.keys(res.data));
        if (res.data.siteCompanies) {
            console.log('siteCompanies found! Length:', res.data.siteCompanies.length);
        } else {
            console.log('WARNING: siteCompanies NOT found at top level!');
        }
    } catch (err) {
        console.error('Error:', err.message);
    }
}

test();
