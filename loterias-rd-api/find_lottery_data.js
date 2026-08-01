const fs = require('fs');

const resolved = JSON.parse(fs.readFileSync('resolved_nuxt.json', 'utf8'));

// We want to find where the games list/results list is inside the resolved structure.
// Let's do a recursive search for objects with typical properties like "numbers", "score", "draw", "drawDate" etc.
const foundObjects = [];

function search(obj, path = '') {
    if (!obj || typeof obj !== 'object') return;
    
    if (Array.isArray(obj)) {
        obj.forEach((item, idx) => search(item, `${path}[${idx}]`));
        return;
    }
    
    // Check if it's a candidate
    if (obj.title || obj.name || obj.game || obj.company_id || obj.numbers || obj.score || obj.score_layout) {
        foundObjects.push({ path, keys: Object.keys(obj), obj });
    }
    
    for (const k in obj) {
        search(obj[k], `${path}.${k}`);
    }
}

search(resolved);

console.log(`Found ${foundObjects.length} candidate objects.`);
foundObjects.slice(0, 15).forEach(c => {
    console.log(`\nPath: ${c.path}`);
    console.log(`Keys: ${c.keys.join(', ')}`);
    console.log(`Snippet:`, JSON.stringify(c.obj).substring(0, 300));
});
