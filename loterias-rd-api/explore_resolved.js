const fs = require('fs');

const resolved = JSON.parse(fs.readFileSync('resolved_nuxt.json', 'utf8'));

console.log('State[0] keys/type:', typeof resolved.state['0'], Object.keys(resolved.state['0'] || {}));
console.log('State[1] keys/type:', typeof resolved.state['1'], Object.keys(resolved.state['1'] || {}));

// Let's print the structure of state deep
function printDeep(obj, depth = 0, path = '') {
    if (depth > 4) return;
    if (obj === null || obj === undefined) return;
    if (typeof obj !== 'object') {
        console.log(`${path}: ${obj} (${typeof obj})`);
        return;
    }
    if (Array.isArray(obj)) {
        console.log(`${path}: Array(${obj.length})`);
        if (obj.length > 0) {
            printDeep(obj[0], depth + 1, `${path}[0]`);
        }
    } else {
        console.log(`${path}: Object`);
        for (const k in obj) {
            printDeep(obj[k], depth + 1, `${path}.${k}`);
        }
    }
}

printDeep(resolved.state, 0, 'state');
