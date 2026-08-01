const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

const found = [];

function search(obj, path = '') {
    if (!obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj)) {
        obj.forEach((item, idx) => search(item, `${path}[${idx}]`));
        return;
    }
    
    if (obj.sessions !== undefined || obj.score !== undefined || obj.numbers !== undefined) {
        found.push({ path, keys: Object.keys(obj), obj });
    }
    
    for (const k in obj) {
        // Prevent infinite loops if there are circular refs (should not be in JSON, but just in case)
        if (Object.prototype.hasOwnProperty.call(obj, k)) {
            search(obj[k], `${path}.${k}`);
        }
    }
}

search(decoded);

console.log(`Found ${found.length} objects related to sessions/scores.`);
found.slice(0, 30).forEach(c => {
    console.log(`\nPath: ${c.path}`);
    console.log(`Keys: ${c.keys.join(', ')}`);
    console.log(`Snippet:`, JSON.stringify(c.obj).substring(0, 300));
});
