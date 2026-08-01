const fs = require('fs');

const data = JSON.parse(fs.readFileSync('nuxt_data.json', 'utf8'));

// Nuxt data format is:
// [ {state: 1, once: 3, ...}, value1, value2, ... ]
// The first element is the root index/map.
// Let's print the first 150 elements of the array.
console.log("First 150 elements:");
for (let i = 0; i < Math.min(150, data.length); i++) {
    console.log(`${i}:`, JSON.stringify(data[i]));
}
