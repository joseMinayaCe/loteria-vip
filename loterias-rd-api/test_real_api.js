const axios = require('axios');
const fs = require('fs');

async function testFetch() {
    try {
        const url = 'https://api.loteriasdominicanas.com/dominicana/sites/env';
        console.log('Fetching from real API:', url);
        const { data } = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
        });
        fs.writeFileSync('real_api_response.json', JSON.stringify(data, null, 2));
        console.log('Saved response to real_api_response.json');
        
        console.log('Response type:', typeof data);
        if (data) {
            console.log('Keys:', Object.keys(data));
            if (data.feed) {
                console.log('Feed length:', data.feed.length);
                if (data.feed.length > 0) {
                    console.log('First feed item:', JSON.stringify(data.feed[0]));
                }
            }
        }
    } catch (e) {
        console.error('Error fetching real API:', e.message);
        if (e.response) {
            console.error('Response status:', e.response.status);
            console.error('Response data:', e.response.data);
        }
    }
}

testFetch();
