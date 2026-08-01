const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

console.log('decoded.data type:', typeof decoded.data);
console.log('decoded.data keys:', Object.keys(decoded.data));
console.log('decoded.data[0] type:', typeof decoded.data['0']);
console.log('decoded.data[0]:', JSON.stringify(decoded.data['0']).substring(0, 1000));
