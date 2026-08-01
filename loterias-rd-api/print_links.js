const fs = require('fs');
const cheerio = require('cheerio');

const html = fs.readFileSync('page.html', 'utf8');
const $ = cheerio.load(html);

console.log("=== LINK TAGS ===");
$('link').each((i, el) => {
    console.log($(el).toString());
});
