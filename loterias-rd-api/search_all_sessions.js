const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

const results = [];

function deepSearch(obj, path = '') {
    if (!obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj)) {
        obj.forEach((item, idx) => deepSearch(item, `${path}[${idx}]`));
        return;
    }
    
    // Check if the object has any session or draw data
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
results.slice(0, 30).forEach(r => {
    // Check if it has actual data or is just schema
    const str = JSON.stringify(r.obj);
    if (str.includes('[') || str.match(/\d+/)) {
        console.log(`\nPath: ${r.path}`);
        console.log(`Keys: ${r.keys.join(', ')}`);
        console.log(`Content: ${str.substring(0, 400)}`);
    }
});
