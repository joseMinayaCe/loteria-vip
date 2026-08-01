const fs = require('fs');

const resolved = JSON.parse(fs.readFileSync('resolved_nuxt.json', 'utf8'));

const urls = new Set();

function findUrls(obj) {
    if (!obj) return;
    if (typeof obj === 'string') {
        if (obj.includes('http') || obj.includes('s3') || obj.includes('.json') || obj.includes('api')) {
            urls.add(obj);
        }
        return;
    }
    if (typeof obj === 'object') {
        for (const k in obj) {
            findUrls(obj[k]);
        }
    }
}

findUrls(resolved);

console.log("All matching strings in resolved Nuxt data:");
console.log(Array.from(urls).slice(0, 100));
