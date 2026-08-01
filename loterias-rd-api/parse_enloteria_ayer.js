const fs = require('fs');
const cheerio = require('cheerio');

const html = fs.readFileSync('enloteria_page_ayer.html', 'utf8');
const $ = cheerio.load(html);

console.log('Title of page:', $('title').text());

const results = [];

$('.result-card').each((i, el) => {
    const title = $(el).find('.lottery-name').text().trim();
    const dateText = $(el).find('.result-date').text().trim();
    const closingTime = $(el).find('.lottery-closing-time').text().trim();
    
    // Extrar los números de los result-balls
    const numbers = [];
    $(el).find('.result-ball').each((j, ballEl) => {
        // Enloteria suele tener los números como texto dentro del div.result-ball o una clase hija, o en un span
        // Veamos la estructura exacta
        const text = $(ballEl).text().trim();
        if (text) {
            numbers.push(text);
        }
    });
    
    results.push({
        title,
        dateText,
        closingTime,
        numbers,
        htmlSnippet: $(el).find('.numbers').html() ? $(el).find('.numbers').html().trim().replace(/\s+/g, ' ') : null
    });
});

console.log('Parsed', results.length, 'results:');
results.forEach((r, idx) => {
    console.log(`\nDraw #${idx}: Title: "${r.title}", Time: "${r.closingTime}", Date: "${r.dateText}"`);
    console.log(`  Numbers extracted:`, r.numbers);
    console.log(`  HTML Snippet:`, r.htmlSnippet);
});
