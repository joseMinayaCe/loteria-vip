const axios = require('axios');
const cheerio = require('cheerio');
const fs = require('fs');

async function findStructure() {
    try {
        const url = 'https://loteriasdominicanas.com/';
        const { data } = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
        });
        fs.writeFileSync('page.html', data);
        console.log('Saved page to page.html');

        const $ = cheerio.load(data);
        
        // Let's print the title
        console.log('Page Title:', $('title').text());

        // Let's find any elements containing numbers or lotteries
        console.log('Checking for common lottery names...');
        const keywords = ['Gana Más', 'Nacional', 'Leidsa', 'Real', 'Loteka'];
        keywords.forEach(keyword => {
            const elements = $(`*:contains("${keyword}")`);
            console.log(`Keyword "${keyword}" found in ${elements.length} elements.`);
            if (elements.length > 0) {
                // Let's print the tag and class of the smallest element containing the keyword
                let smallestEl = null;
                let minLength = Infinity;
                elements.each((i, el) => {
                    const text = $(el).text().trim();
                    if (text.length < minLength) {
                        minLength = text.length;
                        smallestEl = el;
                    }
                });
                if (smallestEl) {
                    console.log(`  - Smallest tag: <${smallestEl.name}> class: "${$(smallestEl).attr('class')}" id: "${$(smallestEl).attr('id')}"`);
                    console.log(`    Content snippet: ${$(smallestEl).text().trim().substring(0, 100)}`);
                    console.log(`    Parent tag: <${smallestEl.parent.name}> class: "${$(smallestEl.parent).attr('class')}"`);
                    if (smallestEl.parent.parent) {
                        console.log(`    Grandparent tag: <${smallestEl.parent.parent.name}> class: "${$(smallestEl.parent.parent).attr('class')}"`);
                    }
                }
            }
        });

    } catch (error) {
        console.error('Error:', error);
    }
}

findStructure();
