const fs = require('fs');

const raw = JSON.parse(fs.readFileSync('payload.json', 'utf8'));
console.log('Raw payload length:', raw.length);

// Let's search for "Gana Más" in raw payload
const index = raw.indexOf("Gana Más");
console.log('Index of "Gana Más":', index);

// Let's search for other common lottery names
const names = ["Nacional", "Leidsa", "Lotería Real", "Loteka", "La Primera", "Gana Más"];
names.forEach(name => {
    const idx = raw.indexOf(name);
    console.log(`Index of "${name}":`, idx);
});

// Let's find any numbers or arrays that might represent session data
// Typically, a session object has keys and values.
// Let's search for occurrences of "session" or "score" in raw strings
raw.forEach((val, idx) => {
    if (typeof val === 'string' && (val.includes('session') || val.includes('score') || val.includes('feed'))) {
        console.log(`String "${val}" at index ${idx}`);
    }
});
