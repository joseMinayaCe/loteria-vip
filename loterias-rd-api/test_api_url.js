const axios = require('axios');

async function test() {
    try {
        const url = 'https://api.loteriasdominicanas.com/dominicana/sites/env?date=2026-06-14T00:00:00.000Z';
        console.log('Fetching:', url);
        const res = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
            },
            timeout: 8000
        });
        console.log('Status:', res.status);
        console.log('Data sample:', JSON.stringify(res.data).substring(0, 500));
    } catch (err) {
        console.error('Error fetching API URL:', err.message);
        if (err.response) {
            console.error('Status:', err.response.status);
            console.error('Body:', err.response.data);
        }
    }
}

test();
