const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

// Helper function to search for keys or string values recursively in an object
function findPaths(obj, targetKey, currentPath = 'decoded') {
    if (obj === null || obj === undefined) return;
    
    if (typeof obj === 'object') {
        if (Array.isArray(obj)) {
            obj.forEach((item, index) => {
                findPaths(item, targetKey, `${currentPath}[${index}]`);
            });
        } else {
            Object.keys(obj).forEach(key => {
                if (key.toLowerCase().includes(targetKey.toLowerCase())) {
                    console.log(`FOUND KEY MATCH: "${key}" at path: ${currentPath}.${key}`);
                }
                findPaths(obj[key], targetKey, `${currentPath}.${key}`);
            });
        }
    } else if (typeof obj === 'string') {
        if (obj.toLowerCase().includes(targetKey.toLowerCase())) {
            console.log(`FOUND STRING VALUE MATCH: "${obj}" at path: ${currentPath}`);
        }
    }
}

console.log("--- Searching for 'Nacional' ---");
findPaths(decoded, 'Nacional');

console.log("\n--- Searching for 'site' ---");
findPaths(decoded, 'site');

console.log("\n--- Searching for 'session' ---");
findPaths(decoded, 'session');

console.log("\n--- Searching for 'game' ---");
findPaths(decoded, 'game');
