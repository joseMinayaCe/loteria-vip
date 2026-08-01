const axios = require('axios');
const cheerio = require('cheerio');
const fs = require('fs');

async function testFetch() {
    try {
        const baseUrl = 'https://loteriasdominicanas.com';
        const { data: html } = await axios.get(baseUrl, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
        });
        
        const $ = cheerio.load(html);
        const link = $('link[rel="preload"][as="fetch"]').attr('href');
        console.log('Found payload link:', link);
        
        if (link) {
            const payloadUrl = link.startsWith('http') ? link : `${baseUrl}${link}`;
            console.log('Fetching payload from:', payloadUrl);
            const { data: payload } = await axios.get(payloadUrl, {
                headers: {
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
                }
            });
            fs.writeFileSync('payload.json', JSON.stringify(payload, null, 2));
            console.log('Saved payload.json');
            
            // Let's decode it
            function unflatten(data) {
                if (!Array.isArray(data)) return data;
                const cache = new Map();
                function walk(index) {
                    if (index === null || index === undefined) return null;
                    if (typeof index !== 'number') return index;
                    if (index < 0 || index >= data.length) return index;
                    if (cache.has(index)) return cache.get(index);
                    const val = data[index];
                    if (val === null || val === undefined) return val;
                    if (typeof val === 'object') {
                        if (Array.isArray(val)) {
                            const arr = [];
                            cache.set(index, arr);
                            for (const item of val) arr.push(walk(item));
                            return arr;
                        } else {
                            const obj = {};
                            cache.set(index, obj);
                            for (const k in val) obj[k] = walk(val[k]);
                            return obj;
                        }
                    }
                    return val;
                }
                return walk(0);
            }
            
            const decoded = unflatten(payload);
            fs.writeFileSync('decoded_payload.json', JSON.stringify(decoded, null, 2));
            console.log('Saved decoded_payload.json');
            
            // Let's inspect the feed inside pinia
            if (decoded && decoded.state) {
                console.log('State keys:', Object.keys(decoded.state));
            }
            if (decoded && decoded.pinia && decoded.pinia.main) {
                console.log('Pinia main keys:', Object.keys(decoded.pinia.main));
                const feedState = decoded.pinia.main.feed;
                if (feedState) {
                    console.log('Feed state:', JSON.stringify(feedState).substring(0, 500));
                }
            }
        }
    } catch (e) {
        console.error('Error:', e);
    }
}

testFetch();
