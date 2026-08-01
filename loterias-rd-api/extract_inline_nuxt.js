/**
 * Analiza el payload inline en el HTML del sitio para encontrar los datos de resultados
 */
const https = require('https');
const fs = require('fs');

function fetchUrl(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
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
  console.log('Cargando página principal...');
  const mainPage = await fetchUrl('https://loteriasdominicanas.com/');
  
  // Guardar el HTML completo
  fs.writeFileSync('main_page.html', mainPage.data);
  console.log('HTML guardado en main_page.html');
  
  // Extraer el contenido inline del NUXT_DATA
  const match = mainPage.data.match(/<script id="__NUXT_DATA__"[^>]*>([\s\S]*?)<\/script>/);
  if (match) {
    const inlineData = match[1];
    console.log('\nEncontrado NUXT_DATA inline, longitud:', inlineData.length);
    fs.writeFileSync('nuxt_inline_data.json', inlineData);
    console.log('Guardado en nuxt_inline_data.json');
    
    try {
      const parsed = JSON.parse(inlineData);
      console.log('\nTipo de dato raíz:', Array.isArray(parsed) ? 'Array' : typeof parsed);
      if (Array.isArray(parsed)) {
        console.log('Longitud del array:', parsed.length);
        console.log('Primeros 10 elementos:', JSON.stringify(parsed.slice(0, 10)));
        
        // Buscar el índice 9 (pinia) y sus datos
        console.log('\nElemento 9 (pinia):', JSON.stringify(parsed[9]));
        
        // Buscar referencias a feed, siteForDate
        for (let i = 0; i < Math.min(parsed.length, 3600); i++) {
          const el = parsed[i];
          if (el === 'feed' || el === 'siteForDate' || el === 'sessions' || el === 'scores') {
            console.log(`\nElemento [${i}] = "${el}", siguiente:`, JSON.stringify(parsed[i+1]));
          }
          if (typeof el === 'object' && el !== null && !Array.isArray(el)) {
            const keys = Object.keys(el);
            if (keys.includes('sessions') || keys.includes('scores') || keys.includes('numbers')) {
              console.log(`\nObjeto en [${i}] con keys interesantes:`, keys.join(', '));
              console.log('Contenido:', JSON.stringify(el).substring(0, 500));
            }
          }
        }
      }
    } catch(e) {
      console.log('Error parseando JSON:', e.message);
      console.log('Primeros 500 chars:', inlineData.substring(0, 500));
    }
  } else {
    console.log('\nNo se encontró NUXT_DATA inline. Buscando en HTML...');
    // Buscar cualquier script con datos
    const scripts = mainPage.data.match(/<script[^>]*>([\s\S]*?)<\/script>/g) || [];
    console.log('Scripts encontrados:', scripts.length);
    scripts.forEach((s, i) => {
      if (s.length > 100 && (s.includes('sessions') || s.includes('scores') || s.includes('numbers') || s.includes('feed'))) {
        console.log(`\nScript ${i} (${s.length} chars):`, s.substring(0, 300));
      }
    });
  }
}

main().catch(console.error);
