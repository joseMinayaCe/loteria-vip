/**
 * Busca los endpoints de API reales usados por el sitio loteriasdominicanas.com
 * para los resultados en tiempo real.
 */
const https = require('https');

function fetchUrl(url, options = {}) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'application/json, */*',
        'Referer': 'https://loteriasdominicanas.com/',
        'Origin': 'https://loteriasdominicanas.com',
        ...options.headers
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, data, headers: res.headers }));
    });
    req.on('error', reject);
    req.setTimeout(10000, () => { req.destroy(); reject(new Error('Timeout')); });
  });
}

async function main() {
  // Endpoints posibles basados en la nueva API
  const endpoints = [
    'https://api.loteriasdominicanas.com/dominicana/feed/game-results',
    'https://api.loteriasdominicanas.com/dominicana/feed/results',
    'https://api.loteriasdominicanas.com/dominicana/results',
    'https://api.loteriasdominicanas.com/dominicana/games/results',
    'https://api.loteriasdominicanas.com/dominicana/sites/games',
    'https://api.loteriasdominicanas.com/dominicana/sites/results',
    'https://api.loteriasdominicanas.com/dominicana/live',
    'https://api.loteriasdominicanas.com/dominicana/scores',
    'https://api.loteriasdominicanas.com/dominicana/sessions',
    'https://api.loteriasdominicanas.com/dominicana/games/sessions',
    // Probar también sin "dominicana"
    'https://api.loteriasdominicanas.com/sites/env',
    'https://api.loteriasdominicanas.com/results',
    'https://api.loteriasdominicanas.com/games',
    // Nuevo estilo de API con versión
    'https://api.loteriasdominicanas.com/v1/results',
    'https://api.loteriasdominicanas.com/v2/results',
    'https://api.loteriasdominicanas.com/api/results',
    // Endpoints de Nuxt/Nitro
    'https://loteriasdominicanas.com/api/results',
    'https://loteriasdominicanas.com/api/games',
    'https://loteriasdominicanas.com/api/scores',
    'https://loteriasdominicanas.com/_nuxt/data.json',
  ];

  for (const url of endpoints) {
    try {
      const result = await fetchUrl(url);
      if (result.status === 200) {
        const preview = result.data.substring(0, 300);
        console.log(`\n✅ ${url} [${result.status}]`);
        console.log('Content-Type:', result.headers['content-type']);
        console.log('Preview:', preview);
      } else {
        console.log(`❌ ${url} [${result.status}]`);
      }
    } catch (e) {
      console.log(`💥 ${url} [ERROR: ${e.message}]`);
    }
  }
}

main().catch(console.error);
