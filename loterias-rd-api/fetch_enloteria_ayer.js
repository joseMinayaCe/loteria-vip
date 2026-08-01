const axios = require('axios');
axios.get('https://enloteria.com/resultados-anguilla-ayer', {
    headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    }
}).then(res => {
    console.log('Status:', res.status);
    console.log('Length:', res.data.length);
    const fs = require('fs');
    fs.writeFileSync('enloteria_page_ayer.html', res.data);
    console.log('Saved to enloteria_page_ayer.html');
}).catch(err => {
    console.error('Error:', err.message);
});
