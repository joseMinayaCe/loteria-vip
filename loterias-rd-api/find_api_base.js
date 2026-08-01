/**
 * Encuentra el baseURL del $api y los endpoints exactos usados
 */
const fs = require('fs');

const jsContent = fs.readFileSync('main_chunk.js', 'utf8');

// Buscar el contexto completo de apiBase
console.log('=== Contexto de apiBase ===');
let pos = jsContent.indexOf('apiBase');
let count = 0;
while (pos >= 0 && count < 10) {
  console.log(`\n[pos ${pos}]:`);
  console.log(jsContent.substring(Math.max(0, pos - 100), pos + 300));
  pos = jsContent.indexOf('apiBase', pos + 7);
  count++;
}

// Buscar contexto de Pw()
console.log('\n\n=== Función Pw() - baseURL getter ===');
const pwPos = jsContent.indexOf('function Pw(');
if (pwPos >= 0) {
  console.log(jsContent.substring(pwPos, pwPos + 500));
}

// Buscar baseURL:Pw()
const pwUsagePos = jsContent.indexOf('baseURL:Pw()');
if (pwUsagePos >= 0) {
  console.log('\n=== Contexto de baseURL:Pw() ===');
  console.log(jsContent.substring(Math.max(0, pwUsagePos - 200), pwUsagePos + 500));
}

// Buscar Nn().public
console.log('\n\n=== Nn().public (runtime config) ===');
let nnPos = jsContent.indexOf('Nn().public');
count = 0;
while (nnPos >= 0 && count < 5) {
  console.log(`\n[pos ${nnPos}]:`);
  console.log(jsContent.substring(Math.max(0, nnPos - 30), nnPos + 200));
  nnPos = jsContent.indexOf('Nn().public', nnPos + 11);
  count++;
}

// Buscar "feed/game-stats" y el contexto completo
console.log('\n\n=== Contexto completo de feed/game-stats ===');
const feedGSPos = jsContent.indexOf('feed/game-stats');
if (feedGSPos >= 0) {
  console.log(jsContent.substring(Math.max(0, feedGSPos - 500), feedGSPos + 800));
}

// Buscar otros endpoints en $api
console.log('\n\n=== Todos los usos de .$api( ===');
const apiCallRegex = /\.\$api\([^)]{1,100}\)/g;
const apiCalls = jsContent.match(apiCallRegex) || [];
apiCalls.forEach(c => console.log(' ', c));

// Buscar otros endpoints del tipo "feed/..."
console.log('\n\n=== Endpoints con "feed/" ===');
const feedRegex = /"feed\/[^"]+"/g;
const feedEndpoints = jsContent.match(feedRegex) || [];
feedEndpoints.forEach(e => console.log(' ', e));

// Buscar la configuración de runtimeConfig
console.log('\n\n=== runtimeConfig / public ===');
const rtPos = jsContent.indexOf('runtimeConfig');
if (rtPos >= 0) {
  console.log(jsContent.substring(rtPos, rtPos + 500));
}
