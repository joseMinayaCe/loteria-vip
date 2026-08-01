const fs = require('fs');
const cheerio = require('cheerio');

const htmlPath = 'C:\\Users\\user\\.gemini\\antigravity\\brain\\0ff93f10-e3fc-4bef-806c-4935f7aecc8d\\.system_generated\\steps\\122\\content.md';
const html = fs.readFileSync(htmlPath, 'utf8');
const $ = cheerio.load(html);

console.log("=== Checking if Nuxt state is present in HTML ===");
// Nuxt 3 often embeds state in <script> tags, like id="__NUXT_DATA__" or window.__NUXT__
$('script').each((i, el) => {
    const id = $(el).attr('id');
    const type = $(el).attr('type');
    const text = $(el).text();
    
    if (id) console.log(`Script ID: ${id}, Type: ${type}, length: ${text.length}`);
    if (text.includes('__NUXT') || text.includes('siteCompanies')) {
        console.log(`Found NUXT in script! length: ${text.length}`);
        fs.writeFileSync('embedded_nuxt_data.js', text);
        console.log('Saved embedded_nuxt_data.js');
    }
});

console.log("\n=== Checking DOM elements ===");
// Check for typical CSS classes for results
const classes = ['.game-block', '.company-block', '.results', '.score', '.ball', '.game-ball', '.company-header'];
classes.forEach(cls => {
    const el = $(cls);
    console.log(`Elements matching "${cls}": ${el.length}`);
    if (el.length > 0) {
        console.log(`  Sample text for "${cls}": ${$(el[0]).text().trim().substring(0, 100)}`);
    }
});

// Let's print out all texts from elements that look like company names
console.log("\n=== Searching for company names in DOM ===");
const possibleCompanies = ["Leidsa", "Nacional", "Lotería Real", "Loteka", "La Primera", "La Suerte", "Lotedom", "Anguila", "King Lottery", "Nueva York", "Florida"];
possibleCompanies.forEach(name => {
    const found = [];
    $(`*:contains("${name}")`).each((i, el) => {
        if ($(el).children().length === 0) { // leaf elements
            found.push($(el).text().trim());
        }
    });
    console.log(`Leaf elements containing "${name}":`, found.slice(0, 5));
});
