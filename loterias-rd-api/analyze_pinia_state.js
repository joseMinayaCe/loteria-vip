/**
 * Analiza el array inline del HTML en los índices de siteForDate y feed
 * para encontrar los resultados de lotería
 */
const fs = require('fs');

// Cargar y parsear el HTML para extraer el array NUXT_DATA inline
const html = fs.readFileSync('main_page.html', 'utf8');
const scriptMatch = html.match(/<script[^>]+id="__NUXT_DATA__"[^>]*>([\s\S]*?)<\/script>/i);
if (!scriptMatch) {
  console.error('No se encontró __NUXT_DATA__');
  process.exit(1);
}

const arr = JSON.parse(scriptMatch[1]);
console.log('Array length:', arr.length);

// El header del array:
// [0]: {"state":1,"once":3,"_errors":4,"serverRendered":7,"path":8,"pinia":9,"prerenderedAt":3502}
// [9]: {"main":10}
// [10]: {"site":11,"siteForDate":3492,"feed":3494}

// Resolver referencias en el array
function resolveRef(idx, depth = 0) {
  if (depth > 20) return '[MAX_DEPTH]';
  if (idx === null || idx === undefined || typeof idx !== 'number') return idx;
  const val = arr[idx];
  if (val === null || val === undefined) return val;
  if (typeof val !== 'object') return val;
  
  if (Array.isArray(val)) {
    // Nuxt special types
    if (val.length >= 1 && typeof val[0] === 'string') {
      const type = val[0];
      if (['Ref', 'ShallowRef', 'Reactive', 'ShallowReactive', 'SiteModel', 'FeedModel', 'GameModel', 'SessionModel'].includes(type)) {
        return resolveRef(val[1], depth + 1);
      }
      if (type === 'Set') {
        return val.slice(1).map(v => typeof v === 'number' ? resolveRef(v, depth+1) : v);
      }
    }
    return val.map((v, i) => typeof v === 'number' ? resolveRef(v, depth+1) : v);
  }
  
  // Es un objeto, resolver cada propiedad
  const result = {};
  for (const [k, v] of Object.entries(val)) {
    result[k] = typeof v === 'number' ? resolveRef(v, depth + 1) : v;
  }
  return result;
}

// Analizar los índices clave
console.log('\n=== Elemento [10] (estado de Pinia main) ===');
console.log(JSON.stringify(arr[10]));

// siteForDate está en índice 3492
const siteForDateIdx = 3492;
console.log(`\n=== arr[${siteForDateIdx}] = siteForDate ===`);
console.log(JSON.stringify(arr[siteForDateIdx]));
if (typeof arr[siteForDateIdx] === 'number') {
  console.log(`-> arr[${arr[siteForDateIdx]}] =`, JSON.stringify(arr[arr[siteForDateIdx]]).substring(0, 500));
}

// feed está en índice 3494
const feedIdx = 3494;
console.log(`\n=== arr[${feedIdx}] = feed ===`);
console.log(JSON.stringify(arr[feedIdx]));
if (typeof arr[feedIdx] === 'number') {
  const feedVal = arr[arr[feedIdx]];
  console.log(`-> arr[${arr[feedIdx]}] =`, JSON.stringify(feedVal).substring(0, 500));
}

// Ver elementos alrededor de 3490-3503
console.log('\n=== Elementos 3488-3503 ===');
for (let i = 3488; i <= 3503; i++) {
  console.log(`[${i}]:`, JSON.stringify(arr[i]).substring(0, 200));
}

// Buscar datos de sesiones en toda la parte final del array (índices > 3400)
console.log('\n=== Buscando datos relevantes en índices 3400+ ===');
for (let i = 3400; i < arr.length; i++) {
  const el = arr[i];
  if (typeof el === 'string' && ['sessions', 'scores', 'numbers', 'draws', 'game_stats', 'siteForDate', 'feed'].includes(el)) {
    console.log(`[${i}] = "${el}" | [${i+1}] = ${JSON.stringify(arr[i+1]).substring(0,200)}`);
  }
  if (typeof el === 'object' && el !== null && !Array.isArray(el)) {
    const keys = Object.keys(el);
    if (keys.some(k => ['sessions', 'scores', 'numbers', 'draws'].includes(k))) {
      console.log(`[${i}] obj keys: ${keys.join(', ')}`);
      console.log(JSON.stringify(el).substring(0, 500));
    }
  }
}

// Buscar todas las claves únicas de tipo string en el array
console.log('\n=== Todas las claves string únicas en el array ===');
const stringSet = new Set();
for (let i = 0; i < arr.length; i++) {
  if (typeof arr[i] === 'string' && arr[i].length < 50 && /^[a-z_]+$/i.test(arr[i])) {
    stringSet.add(arr[i]);
  }
}
console.log([...stringSet].sort().join(', '));
