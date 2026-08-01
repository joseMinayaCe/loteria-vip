const fs = require('fs');

const resolved = JSON.parse(fs.readFileSync('resolved_nuxt.json', 'utf8'));

const found = [];

function search(obj, path = '') {
    if (!obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj)) {
        obj.forEach((item, idx) => search(item, `${path}[${idx}]`));
        return;
    }
    
    // Look for non-empty sessions, scores, or results
    if ((obj.sessions && Array.isArray(obj.sessions) && obj.sessions.length > 0) || 
        obj.score !== undefined || 
        obj.scores !== undefined ||
        obj.numbers !== undefined) {
        found.push({ path, keys: Object.keys(obj), obj });
    }
    
    for (const k in obj) {
        search(obj[k], `${path}.${k}`);
    }
}

search(resolved);

console.log(`Found ${found.length} objects containing data related to sessions/scores/numbers.`);
found.slice(0, 15).forEach(c => {
    console.log(`\nPath: ${c.path}`);
    console.log(`Keys: ${c.keys.join(', ')}`);
    console.log(`Snippet:`, JSON.stringify(c.obj).substring(0, 300));
});
