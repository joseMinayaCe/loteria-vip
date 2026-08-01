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
        
        console.log("=== FIRST GAME BLOCK INNER HTML ===");
        const firstBlock = $('.game-block').first();
        console.log(firstBlock.html());
        
        console.log("\n=== SECOND GAME BLOCK INNER HTML ===");
        const secondBlock = $('.game-block').eq(1);
        console.log(secondBlock.html());

        console.log("\n=== THIRD GAME BLOCK INNER HTML ===");
        const thirdBlock = $('.game-block').eq(2);
        console.log(thirdBlock.html());

    } catch (error) {
        console.error('Error:', error);
    }
}

inspect();
