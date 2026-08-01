/**
 * Extrae y analiza el payload inline del HTML (el que está en el tag script)
 * y también busca la nueva URL del payload para descargar datos frescos
 */
const https = require('https');
const fs = require('fs');

function fetchUrl(url) {
  return new Promise((resolve, reject) => {
    const req = https.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,*/*',
        'Accept-Language': 'es-ES,es;q=0.9,en;q=0.8',
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
  // Si ya tenemos main_page.html, úsalo
  let html;
  if (fs.existsSync('main_page.html')) {
    console.log('Usando main_page.html existente...');
    html = fs.readFileSync('main_page.html', 'utf8');
  } else {
    console.log('Descargando página principal...');
    const result = await fetchUrl('https://loteriasdominicanas.com/');
    html = result.data;
    fs.writeFileSync('main_page.html', html);
  }

  // Buscar el script con NUXT_DATA (puede tener data-src o contenido inline)
  // El regex más flexible:
  const nuxtScripts = [];
  const scriptRegex = /<script[^>]+id="__NUXT_DATA__"[^>]*>([\s\S]*?)<\/script>/gi;
  let match;
  while ((match = scriptRegex.exec(html)) !== null) {
    nuxtScripts.push({ attrs: match[0].substring(0, 200), content: match[1] });
  }
  
  console.log(`Encontrados ${nuxtScripts.length} scripts NUXT_DATA`);
  
  for (const script of nuxtScripts) {
    console.log('\nAtributos:', script.attrs.substring(0, 200));
    console.log('Longitud del contenido:', script.content.length);
    
    if (script.content.trim().length > 10) {
      // Tiene contenido inline
      try {
        const arr = JSON.parse(script.content.trim());
        console.log('Tipo:', Array.isArray(arr) ? `Array[${arr.length}]` : typeof arr);
        if (Array.isArray(arr)) {
          console.log('\nPrimeros 20 elementos:');
          arr.slice(0, 20).forEach((el, i) => {
            console.log(`  [${i}]:`, JSON.stringify(el).substring(0, 100));
          });
          
          // Buscar "feed" en el array
          console.log('\n--- Buscando "feed" ---');
          for (let i = 0; i < arr.length; i++) {
            if (arr[i] === 'feed') {
              console.log(`[${i}] = "feed", [${i+1}] = ${JSON.stringify(arr[i+1]).substring(0, 200)}`);
              if (typeof arr[i+1] === 'number') {
                const fv = arr[arr[i+1]];
                console.log(`  data[${arr[i+1]}] =`, JSON.stringify(fv).substring(0, 500));
                if (typeof fv === 'number') {
                  console.log(`  data[${fv}] =`, JSON.stringify(arr[fv]).substring(0, 500));
                }
              }
            }
            if (arr[i] === 'siteForDate') {
              console.log(`[${i}] = "siteForDate", [${i+1}] = ${JSON.stringify(arr[i+1]).substring(0, 200)}`);
            }
            if (arr[i] === 'sessions' || arr[i] === 'scores' || arr[i] === 'game_stats') {
              console.log(`[${i}] = "${arr[i]}", [${i+1}] = ${JSON.stringify(arr[i+1]).substring(0, 300)}`);
            }
          }
          
          // Buscar el índice de pinia
          const header = arr[0];
          console.log('\n--- Header/descriptor ---');
          console.log(JSON.stringify(header));
          
          if (typeof header === 'object' && header.pinia !== undefined) {
            const piniaIdx = header.pinia;
            console.log('\nPinia en índice:', piniaIdx);
            const piniaData = arr[piniaIdx];
            console.log('Pinia data:', JSON.stringify(piniaData).substring(0, 500));
            
            // Resolver el contenido de pinia
            if (typeof piniaData === 'number') {
              const piniaObj = arr[piniaData];
              console.log('Pinia object:', JSON.stringify(piniaObj).substring(0, 500));
            }
          }
        }
      } catch(e) {
        console.log('Error parseando JSON:', e.message);
        console.log('Primeros 200 chars del contenido:', script.content.substring(0, 200));
      }
    }
  }
  
  // Si no hay contenido inline, buscar la URL del payload y descargar
  const payloadUrlMatch = html.match(/data-src="(\/[^"]*_payload\.json[^"]*)"/);
  if (payloadUrlMatch) {
    const payloadPath = payloadUrlMatch[1];
    console.log('\n\nURL del payload:', payloadPath);
    
    // Descargar el payload fresco
    const payloadUrl = `https://loteriasdominicanas.com${payloadPath}`;
    console.log('Descargando payload fresco...');
    
    const payloadResp = await fetchUrl(payloadUrl);
    if (payloadResp.status === 200) {
      const payloadArr = JSON.parse(payloadResp.data);
      console.log('Payload descargado. Tipo:', Array.isArray(payloadArr) ? `Array[${payloadArr.length}]` : typeof payloadArr);
      fs.writeFileSync('fresh_payload.json', JSON.stringify(payloadArr, null, 2));
      console.log('Guardado en fresh_payload.json');
      
      const arr = Array.isArray(payloadArr) ? payloadArr : Object.values(payloadArr);
      console.log('\nPrimeros 25 elementos:');
      arr.slice(0, 25).forEach((el, i) => {
        console.log(`  [${i}]:`, JSON.stringify(el).substring(0, 150));
      });
      
      // El descriptor está en el índice 0
      const header = arr[0];
      if (typeof header === 'object' && !Array.isArray(header)) {
        console.log('\nHeader:', JSON.stringify(header));
        
        // Recorrer las claves del header para ver qué apuntan
        for (const [key, idx] of Object.entries(header)) {
          if (typeof idx === 'number') {
            console.log(`\n"${key}" (idx=${idx}) ->`, JSON.stringify(arr[idx]).substring(0, 300));
          }
        }
      }
      
      // Buscar el feed
      console.log('\n=== Buscando clave "feed" ===');
      for (let i = 0; i < arr.length; i++) {
        if (arr[i] === 'feed') {
          console.log(`[${i}] "feed" -> [${i+1}] = ${JSON.stringify(arr[i+1]).substring(0,100)}`);
        }
        if (arr[i] === 'siteForDate') {
          const nextIdx = arr[i+1];
          console.log(`[${i}] "siteForDate" -> [${i+1}] = ${JSON.stringify(arr[nextIdx]).substring(0,300)}`);
        }
      }
    } else {
      console.log('Error descargando payload:', payloadResp.status);
    }
  }
}

main().catch(console.error);
