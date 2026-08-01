const axios = require('axios');
const fs = require('fs');

async function test() {
    try {
        const url = 'https://api.loteriasdominicanas.com/dominicana/feed/game-stats';
        console.log('Fetching:', url);
        const res = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
            }
        });
        fs.writeFileSync('feed_stats_response.json', JSON.stringify(res.data, null, 2));
        console.log('Saved response to feed_stats_response.json');
        console.log('Top level keys:', Object.keys(res.data));
        
        if (res.data.gameStats) {
            console.log('gameStats count:', res.data.gameStats.length);
            if (res.data.gameStats.length > 0) {
                console.log('First gameStat sample:', JSON.stringify(res.data.gameStats[0], null, 2));
            }
        }
        if (res.data.feed) {
            console.log('feed count:', res.data.feed.length);
            if (res.data.feed.length > 0) {
                console.log('First feed sample:', JSON.stringify(res.data.feed[0], null, 2));
            }
        }
    } catch (err) {
        console.error('Error:', err.message);
        if (err.response) {
            console.error('Status:', err.response.status);
            console.error('Body:', err.response.data);
        }
    }
}

test();
