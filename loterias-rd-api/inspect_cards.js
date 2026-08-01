const fs = require('fs');
const cheerio = require('cheerio');

const html = fs.readFileSync('page.html', 'utf8');
const $ = cheerio.load(html);

console.log("Number of cards:", $('.p-card').length);

$('.p-card').each((i, el) => {
    if (i < 3) {
        console.log(`\n--- CARD ${i} ---`);
        // Let's print the structure
        console.log("Title (e.g. text/header):", $(el).find('.p-card-title, h1, h2, h3, h4, [class*="title"]').text().trim());
        // Let's see what classes the text is
        console.log("All text contents in card (spaced):");
        console.log($(el).text().replace(/\s+/g, ' ').trim().substring(0, 300));
        
        // Let's print the entire outer HTML of the first card to see the structure
        if (i === 0) {
            console.log("\nFULL HTML OF CARD 0:");
            console.log($(el).html());
        }
    }
});
