const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

console.log('Type of decoded:', typeof decoded);
console.log('Is array:', Array.isArray(decoded));
if (decoded) {
    console.log('Keys of decoded:', Object.keys(decoded));
    if (decoded.data) {
        console.log('Keys of decoded.data:', Object.keys(decoded.data));
    }
}
