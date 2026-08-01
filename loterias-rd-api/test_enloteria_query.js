const axios = require('axios');
const cheerio = require('cheerio');

async function check() {
    try {
        const res = await axios.get('https://enloteria.com/resultados-anguilla?fecha=09-07-2026', {
            headers: { 'User-Agent': 'Mozilla/5.0' }
        });
        const $ = cheerio.load(res.data);
        console.log('Date on page for ?fecha=09-07-2026:', $('.result-date').first().text().trim());
        
        const res2 = await axios.get('https://enloteria.com/resultados-anguilla', {
            headers: { 'User-Agent': 'Mozilla/5.0' }
        });
        const $2 = cheerio.load(res2.data);
        console.log('Date on page for main URL:', $2('.result-date').first().text().trim());
    } catch (err) {
        console.error(err);
    }
}

check();
