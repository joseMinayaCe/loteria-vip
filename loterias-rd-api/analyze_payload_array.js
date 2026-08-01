/**
 * Analiza el payload.json real que es un array en la raíz
 */
const fs = require('fs');
const https = require('https');

function fetchUrl(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'application/json, */*',
        'Referer': 'https://loteriasdominicanas.com/',
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => resolve({ status: res.statusCode, data, headers: res.headers }));
    });
    req.on('error', reject);
    req.setTimeout(30000, () => { req.destroy(); reject(new Error('Timeout')); });
  });
}

async function fetchFreshPayload() {
  console.log('Descargando página para obtener URL del payload...');
  const mainPage = await fetchUrl('https://loteriasdominicanas.com/');
  const payloadUrlMatch = mainPage.data.match(/_payload\.json\?([a-zA-Z0-9\-]+)/);
  if (!payloadUrlMatch) {
    throw new Error('No se encontró URL del payload');
  }
  const payloadUrl = `https://loteriasdominicanas.com/_payload.json?${payloadUrlMatch[1]}`;
  console.log('Descargando payload desde:', payloadUrl);
  const payloadResponse = await fetchUrl(payloadUrl);
  return JSON.parse(payloadResponse.data);
}

async function main() {
  // Usar payload.json existente (es el array real)
  console.log('Cargando payload.json...');
  const arr = JSON.parse(fs.readFileSync('payload.json', 'utf8'));
  console.log('Total elementos:', Object.keys(arr).length);
  
  // Convertir a array real si es objeto con claves numéricas
  const data = Array.isArray(arr) ? arr : Object.values(arr);
  console.log('Array length:', data.length);
  console.log('\nPrimeros 15 elementos:');
  data.slice(0, 15).forEach((el, i) => console.log(`  [${i}]:`, JSON.stringify(el).substring(0, 100)));
  
  // Buscar strings con claves de resultados
  const keysToFind = ['sessions', 'scores', 'numbers', 'draws', 'results', 'feed', 'siteForDate', 'game_stats', 'stats'];
  console.log('\n=== Buscando claves relevantes ===');
  
  for (let i = 0; i < data.length; i++) {
    const el = data[i];
    if (typeof el === 'string' && keysToFind.includes(el)) {
      const nextEl = data[i+1];
      console.log(`[${i}] "${el}" -> siguiente: ${JSON.stringify(nextEl).substring(0, 200)}`);
      if (typeof nextEl === 'number' && nextEl < data.length) {
        console.log(`  -> data[${nextEl}]:`, JSON.stringify(data[nextEl]).substring(0, 300));
      }
    }
  }
  
  // Buscar objetos con propiedades de resultados
  console.log('\n=== Buscando objetos con sessions/scores/numbers ===');
  let found = 0;
  for (let i = 0; i < data.length; i++) {
    const el = data[i];
    if (typeof el === 'object' && el !== null && !Array.isArray(el)) {
      const keys = Object.keys(el);
      if (keys.some(k => ['sessions', 'scores', 'numbers', 'draws', 'results'].includes(k))) {
        found++;
        if (found <= 10) {
          console.log(`\n[${i}] keys: ${keys.join(', ')}`);
          console.log(JSON.stringify(el).substring(0, 400));
        }
      }
    }
  }
  console.log(`Total objetos encontrados: ${found}`);
  
  // Buscar el objeto "pinia" que tiene el estado de la aplicación
  console.log('\n=== Buscando estado de Pinia ===');
  // El elemento [0] debería ser el estado inicial
  console.log('Elemento 0:', JSON.stringify(data[0]).substring(0, 500));
  
  // El formato Nuxt es: [state_descriptor, state_values...]
  // Donde state_descriptor es un objeto que indica qué es cada índice
  if (typeof data[0] === 'object' && !Array.isArray(data[0])) {
    console.log('\nDescriptor del estado:');
    const desc = data[0];
    for (const [key, val] of Object.entries(desc)) {
      console.log(`  "${key}" -> índice ${val}`);
      if (typeof val === 'number' && val < data.length) {
        const pointed = data[val];
        console.log(`    data[${val}]:`, JSON.stringify(pointed).substring(0, 200));
      }
    }
  }
}

main().catch(console.error);
