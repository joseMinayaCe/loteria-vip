const fs = require('fs');

const resolved = JSON.parse(fs.readFileSync('resolved_nuxt.json', 'utf8'));

const keys = new Set();

function collectKeys(obj) {
    if (!obj || typeof obj !== 'object') return;
    if (Array.isArray(obj)) {
        obj.forEach(item => collectKeys(item));
        return;
    }
    for (const k in obj) {
        keys.add(k);
        collectKeys(obj[k]);
    }
}

collectKeys(resolved);
console.log('Total unique keys:', keys.size);
console.log('All unique keys:', Array.from(keys).sort().join(', '));
