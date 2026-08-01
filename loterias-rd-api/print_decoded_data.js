const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));

console.log('data[0] type:', typeof decoded.data['0']);
if (decoded.data['0']) {
    console.log('data[0] keys:', Object.keys(decoded.data['0']));
}
console.log('data[1] type:', typeof decoded.data['1']);
if (decoded.data['1']) {
    console.log('data[1] keys:', Object.keys(decoded.data['1']));
    // Print a sample of data[1]
    console.log('data[1] snippet:', JSON.stringify(decoded.data['1']).substring(0, 1000));
}
