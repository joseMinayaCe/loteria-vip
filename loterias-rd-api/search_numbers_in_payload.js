const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

// Let's search for a string or number "09" or "36" or 19 in the entire object recursively
const targets = ["09", "36", "19", 9, 36, 19];

function search(obj, path = '') {
    if (obj === null || obj === undefined) return;
    
    if (typeof obj === 'object') {
        if (Array.isArray(obj)) {
            obj.forEach((item, idx) => search(item, `${path}[${idx}]`));
        } else {
            Object.keys(obj).forEach(k => {
                search(obj[k], `${path}.${k}`);
            });
        }
    } else {
        targets.forEach(t => {
            if (obj === t) {
                console.log(`Found target ${t} at path: ${path}`);
            }
        });
    }
}

search(decoded);
