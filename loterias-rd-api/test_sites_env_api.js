/**
 * Obtiene los datos reales del sitio con parámetros correctos y analiza la estructura
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
  const base = 'https://api.loteriasdominicanas.com/dominicana';
  
  // Obtener fecha de hoy y de ayer (zona dominicana = UTC-4)
  const now = new Date();
  const todayISO = now.toISOString();
  
  // Probar con limit=1 y limit=3
  const urls = [
    `${base}/sites/env?date=${encodeURIComponent(todayISO)}&limit=1`,
    `${base}/sites/env?date=${encodeURIComponent(todayISO)}&limit=3`,
  ];
  
  for (const url of urls) {
    console.log(`\n📡 GET ${url}`);
    const result = await fetchUrl(url);
    console.log('Status:', result.status);
    
    if (result.status === 200) {
      const json = JSON.parse(result.data);
      console.log('Claves principales:', Object.keys(json).join(', '));
      
      // Guardar para análisis
      const filename = url.includes('limit=1') ? 'sites_env_limit1.json' : 'sites_env_limit3.json';
      fs.writeFileSync(filename, JSON.stringify(json, null, 2));
      console.log(`Guardado en ${filename}`);
      
      // Analizar siteCompanies -> siteGames -> game
      if (json.siteCompanies) {
        console.log('\nsiteCompanies count:', json.siteCompanies.length);
        
        // Revisar el primer siteCompany y sus juegos
        const firstCompany = json.siteCompanies[0];
        if (firstCompany) {
          console.log('Primera empresa:', firstCompany.company?.name || firstCompany._id);
          
          if (firstCompany.siteGames) {
            console.log('Juegos de la empresa:', firstCompany.siteGames.length);
            
            // Mostrar los primeros 3 juegos con sus resultados
            firstCompany.siteGames.slice(0, 3).forEach((sg, i) => {
              console.log(`\n  Juego [${i}]:`, sg.game?.name || sg._id);
              if (sg.game) {
                const gameKeys = Object.keys(sg.game);
                console.log('  Game keys:', gameKeys.join(', '));
                
                // Buscar resultados/sesiones en el juego
                if (sg.game.sessions) {
                  console.log('  sessions:', JSON.stringify(sg.game.sessions).substring(0, 300));
                }
                if (sg.game.stats) {
                  console.log('  stats:', JSON.stringify(sg.game.stats).substring(0, 300));
                }
                if (sg.game.scores) {
                  console.log('  scores:', JSON.stringify(sg.game.scores).substring(0, 300));
                }
                if (sg.game.results) {
                  console.log('  results:', JSON.stringify(sg.game.results).substring(0, 300));
                }
              }
            });
          }
        }
      }
      
      // Buscar si hay last_session_date o datos de sesiones en algún nivel
      const jsonStr = JSON.stringify(json);
      
      // Buscar arrays con exactamente 2-4 números de 2 dígitos (resultado típico)
      const scorePattern = /"score":"(\d{2})"/g;
      const scores = [];
      let sm;
      while ((sm = scorePattern.exec(jsonStr)) !== null && scores.length < 20) {
        scores.push(sm[1]);
      }
      console.log('\n\nScores encontrados:', scores.slice(0, 20).join(', '));
      
      // Buscar "draw_time" o "session_name"
      if (jsonStr.includes('draw_time')) {
        console.log('\nEncontrado draw_time!');
        const dtPos = jsonStr.indexOf('draw_time');
        console.log('Context:', jsonStr.substring(Math.max(0, dtPos - 50), dtPos + 200));
      }
      
      if (jsonStr.includes('"session"')) {
        console.log('\nEncontrado "session"!');
        const sPos = jsonStr.indexOf('"session"');
        console.log('Context:', jsonStr.substring(Math.max(0, sPos - 50), sPos + 200));
      }
      
    } else {
      console.log('Error:', result.data.substring(0, 200));
    }
  }
}

main().catch(console.error);
