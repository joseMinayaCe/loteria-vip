const axios = require('axios');

const urls = [
    'https://enloteria.com/resultados-anguilla/fecha/09-07-2026',
    'https://enloteria.com/resultados-anguilla/09-07-2026',
    'https://enloteria.com/resultados-anguilla/fecha/2026-07-09',
    'https://enloteria.com/resultados-anguilla/2026-07-09',
    'https://enloteria.com/resultados-anguilla?fecha=09-07-2026',
    'https://enloteria.com/resultados-anguilla?date=2026-07-09',
    'https://enloteria.com/resultados-anguilla-ayer'
];

async function check() {
    for (const url of urls) {
        try {
            const res = await axios.get(url, {
                headers: { 'User-Agent': 'Mozilla/5.0' },
                timeout: 5000
            });
            console.log(`URL: ${url} -> SUCCESS (${res.status}), Length: ${res.data.length}`);
        } catch (err) {
            console.log(`URL: ${url} -> FAILED: ${err.message}`);
        }
    }
}

check();
