/**
 * Busca en el HTML del sitio los links a los JS chunks y los analiza para 
 * encontrar endpoints de API usados para cargar resultados.
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
  
  // Buscar URLs de JS en el HTML
  const jsMatches = mainPage.data.match(/src="([^"]*\.js[^"]*)"/g) || [];
  const jsUrls = jsMatches.map(m => {
    const url = m.replace('src="', '').replace('"', '');
    if (url.startsWith('http')) return url;
    if (url.startsWith('/')) return `https://loteriasdominicanas.com${url}`;
    return `https://loteriasdominicanas.com/${url}`;
  });

  console.log(`\nEncontradas ${jsUrls.length} URLs de JS:`);
  jsUrls.forEach(u => console.log(' -', u));

  // También buscar en el payload de Nuxt
  const payloadMatch = mainPage.data.match(/__NUXT_DATA__[^<]*|window\.__NUXT__[^<]*/);
  if (payloadMatch) {
    console.log('\nEncontrado NUXT data en HTML (primeros 500 chars):', payloadMatch[0].substring(0, 500));
  }

  // Buscar patrones de API endpoints en el HTML mismo
  const apiPatterns = mainPage.data.match(/["'](\/api\/[^"']+)["']|["'](https?:\/\/api\.[^"']+)["']/g) || [];
  console.log('\nAPI endpoints encontrados en HTML:', apiPatterns);

  // Descargar el JS principal de Nuxt (app chunk)
  const appChunk = jsUrls.find(u => u.includes('app') || u.includes('entry') || u.includes('nuxt'));
  if (appChunk) {
    console.log('\n\nDescargando chunk principal:', appChunk);
    try {
      const jsContent = await fetchUrl(appChunk);
      if (jsContent.status === 200) {
        // Guardar para análisis
        fs.writeFileSync('main_chunk.js', jsContent.data);
        console.log('Guardado main_chunk.js');
        
        // Buscar patrones de API
        const patterns = [
          /\/api\/[a-zA-Z0-9\-_\/]+/g,
          /\/feed\/[a-zA-Z0-9\-_\/]+/g,
          /\/games\/[a-zA-Z0-9\-_\/]+/g,
          /\/results[a-zA-Z0-9\-_\/]*/g,
          /\/sessions[a-zA-Z0-9\-_\/]*/g,
          /\/scores[a-zA-Z0-9\-_\/]*/g,
          /api\.[a-zA-Z0-9\-_.]+\.[a-zA-Z]{2,}/g,
        ];
        
        const found = new Set();
        for (const pat of patterns) {
          const matches = jsContent.data.match(pat) || [];
          matches.forEach(m => found.add(m));
        }
        
        console.log('\nEndpoints encontrados en chunk JS:');
        [...found].sort().forEach(e => console.log(' -', e));
      }
    } catch(e) {
      console.log('Error descargando chunk:', e.message);
    }
  }
}

main().catch(console.error);
