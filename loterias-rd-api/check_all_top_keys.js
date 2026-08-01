const fs = require('fs');

const decoded = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));
const mainKey = Object.keys(decoded.data['1'])[0];
const mainData = decoded.data['1'][mainKey];

console.log('Main data keys:', Object.keys(mainData));
if (mainData.feed) {
    console.log('feed found! Type:', typeof mainData.feed);
    if (Array.isArray(mainData.feed)) {
        console.log('feed length:', mainData.feed.length);
        if (mainData.feed.length > 0) {
            console.log('feed[0]:', JSON.stringify(mainData.feed[0]));
        }
    }
} else {
    console.log('feed NOT found!');
}
