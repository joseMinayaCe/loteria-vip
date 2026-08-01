const fs = require('fs');

const js = fs.readFileSync('entry.js', 'utf8');

let pos = 0;
while (true) {
    pos = js.indexOf('sites/env', pos);
    if (pos === -1) break;
    console.log(`\n--- Match at position ${pos} ---`);
    console.log(js.substring(pos - 150, pos + 250));
    pos += 'sites/env'.length;
}

// Let's also search for other /site/ or site/ endpoints
pos = 0;
while (true) {
    pos = js.indexOf('feed/', pos);
    if (pos === -1) break;
    console.log(`\n--- Feed match at position ${pos} ---`);
    console.log(js.substring(pos - 150, pos + 250));
    pos += 'feed/'.length;
}
