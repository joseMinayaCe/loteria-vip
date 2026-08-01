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
        
        console.log("=== HEADER / BODY STRUCTURE ===");
        // Let's find some parents or containers of game-block
        $('.game-block').each((i, el) => {
            if (i < 5) {
                console.log(`\nGame Block ${i}:`);
                console.log("Class list:", $(el).attr('class'));
                console.log("Parent Tag:", el.parent.name);
                console.log("Parent Class:", $(el.parent).attr('class'));
                console.log("Parent id:", $(el.parent).attr('id'));
                
                // Let's print the logo image source if any
                const img = $(el).find('img');
                if (img.length > 0) {
                    console.log("Images inside block:");
                    img.each((j, im) => {
                        console.log(`  - src: ${$(im).attr('src')}, class: ${$(im).attr('class')}, alt: ${$(im).attr('alt')}`);
                    });
                } else {
                    console.log("No images inside block");
                }
                
                // Let's print details
                console.log("Title text:", $(el).find('.game-title').text().trim());
                console.log("Time text:", $(el).find('.game-time').text().trim());
                console.log("Balls count:", $(el).find('.game-ball, .score').length);
            }
        });
        
        // Check if there are headings or company groups
        console.log("\n=== HEADINGS AND CONTAINERS ===");
        $('h2, h3, h4, .company-header, .company-title').each((i, el) => {
            console.log(`${el.name}: class="${$(el).attr('class')}" text="${$(el).text().trim()}"`);
        });

    } catch (error) {
        console.error('Error:', error);
    }
}

inspect();
