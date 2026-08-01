/**
 * Analiza el bundle JS de Nuxt para encontrar los endpoints de API
 * que se usan para cargar datos de lotería (feed, gameStats, sessions, etc.)
 */
const fs = require('fs');
const https = require('https');

function fetchUrl(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': '*/*',
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

async function main() {
  const entryUrl = 'https://loteriasdominicanas.com/_nuxt/entry.UrIeivEx.1781446402873.js';
  
  let jsContent;
  if (fs.existsSync('main_chunk.js')) {
    console.log('Usando main_chunk.js existente...');
    jsContent = fs.readFileSync('main_chunk.js', 'utf8');
  } else {
    console.log('Descargando entry JS...');
    const result = await fetchUrl(entryUrl);
    jsContent = result.data;
    fs.writeFileSync('main_chunk.js', jsContent);
  }
  
  console.log('JS size:', jsContent.length, 'chars');
  
  // Buscar patrones de URL de API en el JS
  const patterns = [
    // Strings literales con paths de API
    /"\/dominicana\/[^"]+"/g,
    /"\/api\/[^"]+"/g,
    /'\/dominicana\/[^']+'/g,
    /'\/api\/[^']+'/g,
    // Template strings con paths
    /`\/dominicana\/[^`]+`/g,
    /`\/api\/[^`]+`/g,
    // URLs completas de la API
    /api\.loteriasdominicanas\.com[^"'\s]+/g,
    // Cualquier path con palabras clave de lotería
    /"\/[^"]*(?:session|score|result|game|feed|stat|number)[^"]*"/gi,
    /'\/[^']*(?:session|score|result|game|feed|stat|number)[^']*'/gi,
  ];
  
  const allMatches = new Set();
  for (const pat of patterns) {
    const matches = jsContent.match(pat) || [];
    matches.forEach(m => allMatches.add(m.trim()));
  }
  
  console.log('\n=== Endpoints/Paths encontrados en JS ===');
  [...allMatches].sort().forEach(m => console.log(m));
  
  // Buscar funciones que hacen fetch/axios
  console.log('\n=== Contexto de llamadas HTTP ===');
  const fetchPatterns = [
    /\$fetch\([^)]{1,200}\)/g,
    /useFetch\([^)]{1,200}\)/g,
    /useAsyncData\([^)]{1,200}\)/g,
    /axios\.[a-z]+\([^)]{1,200}\)/g,
    /fetch\([^)]{1,200}\)/g,
  ];
  
  for (const pat of fetchPatterns) {
    const matches = jsContent.match(pat) || [];
    if (matches.length > 0) {
      console.log(`\n--- ${pat.source.split('\\')[0]} ---`);
      matches.slice(0, 10).forEach(m => console.log(' ', m.substring(0, 200)));
    }
  }
  
  // Buscar baseURL o apiBase en el JS
  console.log('\n=== Configuración de API ===');
  const apiBasePatterns = [
    /baseURL[^;,]{1,100}/g,
    /apiBase[^;,]{1,100}/g,
    /BASE_URL[^;,]{1,100}/g,
    /API_URL[^;,]{1,100}/g,
    /"https?:\/\/api\.[^"]+"/g,
  ];
  
  for (const pat of apiBasePatterns) {
    const matches = jsContent.match(pat) || [];
    if (matches.length > 0) {
      const uniqueMatches = [...new Set(matches)];
      console.log(`\n${pat.source.substring(0, 30)}...`);
      uniqueMatches.forEach(m => console.log(' ', m.substring(0, 200)));
    }
  }
  
  // Buscar "game-stats" o "game_stats" en el JS
  console.log('\n=== Buscando game-stats/game_stats ===');
  const gsIdx = jsContent.indexOf('game-stats');
  if (gsIdx >= 0) {
    console.log('Context:', jsContent.substring(Math.max(0, gsIdx - 100), gsIdx + 200));
  }
  
  const gsIdx2 = jsContent.indexOf('game_stats');
  if (gsIdx2 >= 0) {
    console.log('Context:', jsContent.substring(Math.max(0, gsIdx2 - 100), gsIdx2 + 200));
  }
  
  // Buscar "dominicana" en el JS para ver endpoints
  console.log('\n=== Buscando "dominicana" en JS ===');
  let pos = 0;
  let count = 0;
  while ((pos = jsContent.indexOf('dominicana', pos)) >= 0 && count < 20) {
    console.log(`[pos ${pos}]:`, jsContent.substring(Math.max(0, pos - 30), pos + 100).replace(/\n/g, ' '));
    pos += 10;
    count++;
  }
}

main().catch(console.error);
