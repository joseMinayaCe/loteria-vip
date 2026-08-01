const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_nacional_payload.json', 'utf8'));

const links = new Set();
function search(obj) {
    if (!obj) return;
    if (typeof obj === 'string') {
        if (obj.startsWith('/') || obj.includes('payload') || obj.includes('api') || obj.includes('.json')) {
            links.add(obj);
        }
        return;
    }
    if (typeof obj === 'object') {
        for (const k in obj) {
            search(obj[k]);
        }
    }
}

search(decoded);

console.log("All matching paths/links in Nacional payload:");
console.log(Array.from(links));
