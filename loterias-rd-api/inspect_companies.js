const axios = require('axios');
const cheerio = require('cheerio');

async function inspect() {
    try {
        const url = 'https://loteriasdominicanas.com/';
        const { data } = await axios.get(url, {
            headers: {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            }
        });
        const $ = cheerio.load(data);
        
        console.log("=== GAME BLOCKS AND COMPANIES ===");
        $('.game-block').each((i, el) => {
            const name = $(el).find('.game-title span').text().trim() || $(el).find('.game-title').text().trim();
            const classes = $(el).attr('class');
            const companyBlockClass = classes.split(' ').find(c => c.startsWith('company-block-'));
            
            // Get the text from the company title div specifically (just the text inside the div child of company-title, or the direct text)
            const companyTitleDiv = $(el).find('.company-title');
            // Remove the votedForSession stuff to just get the company name
            const companyText = companyTitleDiv.find('div.d-flex > div').first().text().trim();
            
            console.log(`Game: "${name}" | Class: "${companyBlockClass}" | Company Text: "${companyText}"`);
        });

    } catch (error) {
        console.error('Error:', error);
    }
}

inspect();
