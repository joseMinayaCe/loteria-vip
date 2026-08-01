const fs = require('fs');
const cheerio = require('cheerio');

const html = fs.readFileSync('enloteria_page_ayer.html', 'utf8');
const $ = cheerio.load(html);

console.log('All links in yesterday page:');
$('a').each((i, el) => {
    const href = $(el).attr('href') || '';
    const text = $(el).text().trim();
    if (href.length > 1 && !href.startsWith('#') && !href.includes('whatsapp') && !href.includes('facebook') && !href.includes('twitter')) {
        console.log(`Link: ${text} -> ${href}`);
    }
});
