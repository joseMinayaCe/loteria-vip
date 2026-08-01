const fs = require('fs');

const data = JSON.parse(fs.readFileSync('nuxt_data.json', 'utf8'));

function unflatten(data) {
    const cache = new Map();
    
    function walk(index) {
        if (index === null || index === undefined) return null;
        if (typeof index !== 'number') return index;
        if (index < 0 || index >= data.length) return index;
        
        if (cache.has(index)) {
            return cache.get(index);
        }
        
        const val = data[index];
        if (val === null || val === undefined) {
            return val;
        }
        
        if (typeof val === 'object') {
            if (Array.isArray(val)) {
                const arr = [];
                cache.set(index, arr);
                for (const item of val) {
                    arr.push(walk(item));
                }
                return arr;
            } else {
                // Check if it represents a date or map or set
                // Nuxt sometimes serializes Date, Map, Set, BigInt etc.
                const obj = {};
                cache.set(index, obj);
                for (const k in val) {
                    obj[k] = walk(val[k]);
                }
                return obj;
            }
        }
        
        return val;
    }
    
    return walk(0);
}

try {
    const resolved = unflatten(data);
    fs.writeFileSync('resolved_nuxt.json', JSON.stringify(resolved, null, 2));
    console.log('Successfully wrote resolved_nuxt.json');
    // Let's print some of the top-level keys
    console.log('Top level keys:', Object.keys(resolved));
    if (resolved.state) {
        console.log('State keys:', Object.keys(resolved.state));
    }
} catch (err) {
    console.error('Error resolving Nuxt:', err);
}
