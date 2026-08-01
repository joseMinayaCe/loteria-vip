/**
 * Analiza el payload.json completo para encontrar los datos de resultados/sesiones.
 * Usa el archivo ya descargado o descarga uno nuevo.
 */
const https = require('https');
const fs = require('fs');

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

// Función para resolver el formato de Nuxt payload (array de referencias)
function resolveNuxtPayload(arr) {
  const resolved = {};
  
  function resolve(idx) {
    if (idx === undefined || idx === null) return idx;
    const val = arr[idx];
    
    if (typeof val !== 'object' || val === null) return val;
    
    if (Array.isArray(val)) {
      // Puede ser un array especial de Nuxt como ["Ref", 5] o ["Reactive", 2]
      if (val.length >= 2 && typeof val[0] === 'string') {
        const type = val[0];
        if (['Ref', 'Reactive', 'ShallowReactive', 'ShallowRef', 'SiteModel', 'FeedModel', 'GameModel'].includes(type)) {
          return resolve(val[1]);
        }
        if (type === 'Set') {
          return new Set(val.slice(1).map(resolve));
        }
        if (type === 'Map') {
          const map = {};
          for (let i = 1; i < val.length; i += 2) {
            map[resolve(val[i])] = resolve(val[i+1]);
          }
          return map;
        }
      }
      return val.map((v, i) => typeof v === 'number' ? resolve(v) : v);
    }
    
    if (typeof val === 'object') {
      const result = {};
      for (const [k, v] of Object.entries(val)) {
        result[k] = typeof v === 'number' ? resolve(v) : v;
      }
      return result;
    }
    
    return val;
  }
  
  return resolve(0);
}

async function main() {
  // Cargar el payload desde el archivo ya descargado
  let payloadData;
  if (fs.existsSync('decoded_payload.json')) {
    console.log('Usando decoded_payload.json existente...');
    payloadData = JSON.parse(fs.readFileSync('decoded_payload.json', 'utf8'));
  } else {
    console.log('Descargando nuevo payload...');
    const mainPage = await fetchUrl('https://loteriasdominicanas.com/');
    const payloadUrlMatch = mainPage.data.match(/_payload\.json\?([a-zA-Z0-9\-]+)/);
    if (!payloadUrlMatch) {
      console.error('No se encontró URL del payload');
      return;
    }
    const payloadUrl = `https://loteriasdominicanas.com/_payload.json?${payloadUrlMatch[1]}`;
    console.log('URL payload:', payloadUrl);
    const payloadResponse = await fetchUrl(payloadUrl);
    payloadData = JSON.parse(payloadResponse.data);
    fs.writeFileSync('decoded_payload.json', JSON.stringify(payloadData, null, 2));
  }
  
  const data = payloadData.data;
  console.log('Total elementos en data array:', data.length);
  
  // Buscar elementos que pueden ser resultados de loterías
  // Los números de lotería típicamente son strings de 2 dígitos o números
  let scoreCount = 0;
  let sessionCount = 0;
  
  for (let i = 0; i < data.length; i++) {
    const el = data[i];
    
    // Buscar strings con claves relevantes
    if (el === 'sessions' || el === 'scores' || el === 'numbers' || el === 'draws' || el === 'results') {
      console.log(`\n[${i}] KEY = "${el}", siguiente valor (idx):`, data[i+1]);
      if (typeof data[i+1] === 'number') {
        console.log('  -> dato en:', JSON.stringify(data[data[i+1]]).substring(0, 200));
      }
    }
    
    // Buscar objetos con propiedades típicas de sesiones
    if (typeof el === 'object' && el !== null && !Array.isArray(el)) {
      const keys = Object.keys(el);
      if (keys.includes('scores') || keys.includes('sessions') || keys.includes('draws')) {
        sessionCount++;
        if (sessionCount <= 5) {
          console.log(`\n[${i}] Objeto con session/scores:`, JSON.stringify(el).substring(0, 400));
        }
      }
      if (keys.includes('score') && keys.includes('date')) {
        scoreCount++;
        if (scoreCount <= 3) {
          console.log(`\n[${i}] Posible resultado:`, JSON.stringify(el).substring(0, 300));
        }
      }
    }
    
    // Buscar arrays de tamaño 3-4 que podrían ser números de lotería (2 dígitos cada uno)
    if (Array.isArray(el) && el.length >= 3 && el.length <= 5) {
      const allStringsOrNums = el.every(v => (typeof v === 'string' && /^\d{1,2}$/.test(v)) || typeof v === 'number');
      if (allStringsOrNums && scoreCount < 5) {
        console.log(`\n[${i}] Posible array de números: ${JSON.stringify(el)}`);
        scoreCount++;
      }
    }
  }
  
  console.log(`\n\nResumen: ${sessionCount} objetos con sessions/scores, ${scoreCount} posibles resultados`);
  
  // Buscar la clave "feed" en el payload
  console.log('\n=== Buscando "feed" ===');
  for (let i = 0; i < data.length; i++) {
    if (data[i] === 'feed') {
      const nextIdx = data[i+1];
      console.log(`[${i}] "feed" -> [${nextIdx}] = ${JSON.stringify(data[nextIdx]).substring(0, 500)}`);
      if (typeof nextIdx === 'number' && typeof data[nextIdx] === 'number') {
        const feedVal = data[data[nextIdx]];
        console.log('Feed data (2 niveles):', JSON.stringify(feedVal).substring(0, 500));
      }
    }
  }
  
  // Buscar la clave "siteForDate"
  console.log('\n=== Buscando "siteForDate" ===');
  for (let i = 0; i < data.length; i++) {
    if (data[i] === 'siteForDate') {
      const nextIdx = data[i+1];
      console.log(`[${i}] "siteForDate" -> [${nextIdx}] = ${JSON.stringify(data[nextIdx]).substring(0, 500)}`);
    }
  }
}

main().catch(console.error);
