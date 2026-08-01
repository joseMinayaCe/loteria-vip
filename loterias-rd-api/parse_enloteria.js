const fs = require('fs');
const cheerio = require('cheerio');

const html = fs.readFileSync('enloteria_page.html', 'utf8');
const $ = cheerio.load(html);

const firstCard = $('.result-card').first();
console.log('--- First Card HTML ---');
console.log(firstCard.html());

console.log('\n--- Extraction Test ---');
$('.result-card').each((i, el) => {
    const title = $(el).find('h3, h4, .card-title, .result-title, div:first-child').first().text().trim();
    const date = $(el).find('.result-date').text().trim();
    const balls = [];
    $(el).find('.result-ball, .ball, span').each((j, b) => {
        const text = $(b).text().trim();
        // check if it's a 2-digit number
        if (text && /^\d{1,2}$/.test(text)) {
            balls.push(text);
        }
    });
    console.log(`Card ${i}: title="${title}" date="${date}" balls=${JSON.stringify(balls)}`);
});
