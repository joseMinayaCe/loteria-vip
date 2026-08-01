const axios = require('axios');
const cheerio = require('cheerio');
const fs = require('fs');

async function testFetch() {
    try {
        const baseUrl = 'https://loteriasdominicanas.com';
        const targetUrl = `${baseUrl}/loteria-nacional`;
        console.log('Fetching HTML from:', targetUrl);
        const { data: html } = await axios.get(targetUrl, {
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
            fs.writeFileSync('decoded_nacional_payload.json', JSON.stringify(decoded, null, 2));
            console.log('Saved decoded_nacional_payload.json');
            
            // Search for keys like sessions or scores or numbers in decoded
            const results = [];
            function deepSearch(obj, path = '') {
                if (!obj || typeof obj !== 'object') return;
                if (Array.isArray(obj)) {
                    obj.forEach((item, idx) => deepSearch(item, `${path}[${idx}]`));
                    return;
                }
                const keys = Object.keys(obj);
                const hasNumbers = keys.some(k => k.toLowerCase().includes('number') || k.toLowerCase().includes('score') || k.toLowerCase().includes('ball') || k.toLowerCase().includes('session'));
                if (hasNumbers) {
                    results.push({ path, keys, obj });
                }
                for (const k in obj) {
                    deepSearch(obj[k], `${path}.${k}`);
                }
            }
            deepSearch(decoded);
            console.log(`Found ${results.length} candidate objects.`);
            results.slice(0, 10).forEach(r => {
                console.log(`\nPath: ${r.path}`);
                console.log(`Content: ${JSON.stringify(r.obj).substring(0, 300)}`);
            });
        }
    } catch (e) {
        console.error('Error:', e);
    }
}

testFetch();
