/**
 * Prueba los endpoints encontrados en el JS bundle del sitio
 */
const https = require('https');
const fs = require('fs');

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
    req.setTimeout(15000, () => { req.destroy(); reject(new Error('Timeout')); });
  });
}

async function main() {
  // La apiBase es Nn().public.apiBase 
  // Del output anterior vimos: api.loteriasdominicanas.com/dominicana
  const apiBase = 'https://api.loteriasdominicanas.com/dominicana';
  
  // Endpoints encontrados
  const todayISO = new Date().toISOString();
  const endpoints = [
    // feed/game-stats - el endpoint principal
    `${apiBase}/feed/game-stats`,
    // sites/env con fecha actual
    `${apiBase}/sites/env?date=${encodeURIComponent(todayISO)}&limit=15`,
    // sites/env sin fecha
    `${apiBase}/sites/env`,
    // Variantes
    `${apiBase}/feed/game-stats?date=${encodeURIComponent(todayISO)}`,
  ];

  for (const url of endpoints) {
    console.log(`\n📡 GET ${url}`);
    try {
      const result = await fetchUrl(url);
      console.log(`Status: ${result.status}`);
      console.log(`Content-Type: ${result.headers['content-type']}`);
      
      if (result.status === 200) {
        try {
          const json = JSON.parse(result.data);
          // Ver la estructura
          if (Array.isArray(json)) {
            console.log(`✅ Array de ${json.length} elementos`);
            if (json.length > 0) {
              console.log('Primer elemento:', JSON.stringify(json[0]).substring(0, 300));
            }
          } else {
            console.log('✅ Objeto con claves:', Object.keys(json).join(', '));
            console.log('Contenido:', JSON.stringify(json).substring(0, 500));
          }
          
          // Guardar si tiene datos relevantes
          if (url.includes('game-stats')) {
            fs.writeFileSync('game_stats_response.json', JSON.stringify(json, null, 2));
            console.log('💾 Guardado en game_stats_response.json');
          } else if (url.includes('sites/env') && url.includes('date')) {
            fs.writeFileSync('sites_env_dated_response.json', JSON.stringify(json, null, 2));
            console.log('💾 Guardado en sites_env_dated_response.json');
          }
        } catch(e) {
          console.log('❌ No es JSON válido:', result.data.substring(0, 200));
        }
      } else {
        console.log('Respuesta:', result.data.substring(0, 300));
      }
    } catch(e) {
      console.log(`💥 Error: ${e.message}`);
    }
  }
  
  // Ahora buscar cuál es el apiBase real en el JS
  console.log('\n\n=== Buscando apiBase en JS ===');
  const jsContent = fs.readFileSync('main_chunk.js', 'utf8');
  
  // Buscar el objeto de configuración de Nuxt
  const nuxtConfigPattern = /\{apiBase:[^}]{1,200}\}/g;
  const nuxtConfigs = jsContent.match(nuxtConfigPattern) || [];
  nuxtConfigs.forEach(c => console.log('Config:', c));
  
  // Buscar Pw() que es la función de baseURL
  const pwIdx = jsContent.indexOf('Pw=');
  if (pwIdx >= 0) {
    console.log('\nPw function:', jsContent.substring(pwIdx, pwIdx + 200));
  }
  
  // Buscar el useRuntimeConfig que devuelve apiBase
  const rtConfigPos = jsContent.indexOf('"apiBase"');
  if (rtConfigPos >= 0) {
    console.log('\napiBase config context:', jsContent.substring(Math.max(0, rtConfigPos - 100), rtConfigPos + 300));
  }
  
  // El apiBase puede estar hardcodeado en el bundle
  const apiBaseInJs = jsContent.match(/apiBase:\s*["']([^"']+)["']/g) || [];
  console.log('\napiBase values:', apiBaseInJs);
  
  // Buscar la URL de api.loteriasdominicanas en el JS
  const apiUrlPos = jsContent.indexOf('api.loteriasdominicanas');
  if (apiUrlPos >= 0) {
    console.log('\nURL de API en JS:', jsContent.substring(Math.max(0, apiUrlPos - 50), apiUrlPos + 200));
  }
}

main().catch(console.error);
