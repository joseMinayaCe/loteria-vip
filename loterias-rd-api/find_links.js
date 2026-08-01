const fs = require('fs');
const cheerio = require('cheerio');

const html = fs.readFileSync('enloteria_page.html', 'utf8');
const $ = cheerio.load(html);

console.log('All links containing /fecha or date or previous:');
$('a').each((i, el) => {
    const href = $(el).attr('href') || '';
    const text = $(el).text().trim();
    if (href.includes('anguilla') || href.includes('fecha') || href.includes('2026')) {
        console.log(`Link: ${text} -> ${href}`);
    }
});
