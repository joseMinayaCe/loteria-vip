const fs = require('fs');

const resolved = JSON.parse(fs.readFileSync('resolved_nuxt.json', 'utf8'));

if (resolved.pinia) {
    console.log("Pinia store keys:", Object.keys(resolved.pinia));
    for (const key in resolved.pinia) {
        console.log(`\nStore "${key}" keys:`, Object.keys(resolved.pinia[key]));
        // Let's print a sample if it has a state/data
        if (resolved.pinia[key].site) {
            console.log(`site data type:`, typeof resolved.pinia[key].site);
            if (Array.isArray(resolved.pinia[key].site)) {
                console.log(`site is array of length:`, resolved.pinia[key].site.length);
            }
        }
        
        // Let's search inside the store for any key that contains "session" or "results"
        const store = resolved.pinia[key];
        for (const k in store) {
            const val = store[k];
            if (val && typeof val === 'object') {
                console.log(`  - key "${k}" has type ${typeof val}`);
            } else {
                console.log(`  - key "${k}" = ${val}`);
            }
        }
    }
} else {
    console.log("No pinia in resolved");
}
