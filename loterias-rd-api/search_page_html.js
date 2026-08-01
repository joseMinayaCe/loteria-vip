const fs = require('fs');
const cheerio = require('cheerio');

const html = fs.readFileSync('page.html', 'utf8');
const $ = cheerio.load(html);

// Print all divs or elements with their classes in body, up to 100 elements
console.log("=== Divs in HTML body ===");
let count = 0;
$('body div, body section, body main, body ul, body li, body span').each((i, el) => {
    const className = $(el).attr('class');
    const id = $(el).attr('id');
    const text = $(el).text().trim().substring(0, 40).replace(/\s+/g, ' ');
    if (className && count < 80) {
        console.log(`<${el.name}> id="${id || ''}" class="${className}" text="${text}"`);
        count++;
    }
});
