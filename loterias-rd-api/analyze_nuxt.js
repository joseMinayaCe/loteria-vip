const fs = require('fs');
const cheerio = require('cheerio');

const html = fs.readFileSync('page.html', 'utf8');
const $ = cheerio.load(html);

// Find the script tag containing __NUXT_DATA__
const nuxtScript = $('#__NUXT_DATA__');
if (nuxtScript.length > 0) {
    console.log('Found __NUXT_DATA__ script!');
    const content = nuxtScript.text();
    try {
        const data = JSON.parse(content);
        console.log('__NUXT_DATA__ is valid JSON of length:', data.length);
        // Let's print the first 100 items or search for lottery related terms in it
        fs.writeFileSync('nuxt_data.json', JSON.stringify(data, null, 2));
        console.log('Saved nuxt_data.json');
        
        // Find strings in the array
        const strings = data.filter(x => typeof x === 'string');
        console.log('Number of strings in data:', strings.length);
        
        // Let's search for keywords in the array
        const keywords = ['gana-mas', 'nacional', 'leidsa', 'real', 'loteka', 'score', 'game-block'];
        keywords.forEach(kw => {
            const indices = [];
            data.forEach((val, idx) => {
                if (typeof val === 'string' && val.toLowerCase().includes(kw)) {
                    indices.push(idx);
                }
            });
            console.log(`Keyword "${kw}" found at indices:`, indices.slice(0, 10), `(Total: ${indices.length})`);
        });

    } catch (e) {
        console.error('Failed to parse JSON:', e);
    }
} else {
    console.log('__NUXT_DATA__ not found');
}

// Let's look for any other script tags or structure in the page
console.log('All script IDs:', $('script').map((i, el) => $(el).attr('id') || $(el).attr('src') || 'no-id').get());
