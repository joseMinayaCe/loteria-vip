const fs = require('fs');
const path = require('path');

const androidDir = 'c:\\Users\\user\\Documents\\loteria vip\\app';

function walk(dir, results = []) {
    const list = fs.readdirSync(dir);
    list.forEach(file => {
        const filePath = path.join(dir, file);
        const stat = fs.statSync(filePath);
        if (stat && stat.isDirectory()) {
            walk(filePath, results);
        } else {
            results.push(filePath);
        }
    });
    return results;
}

const files = walk(androidDir);
console.log(`Searching through ${files.length} Android files...`);

const keywords = ['localhost', '3000', '10.0.2.2', 'api/results', 'loterias-rd-api'];
files.forEach(file => {
    if (file.endsWith('.kt') || file.endsWith('.java') || file.endsWith('.xml') || file.endsWith('.json')) {
        const content = fs.readFileSync(file, 'utf8');
        keywords.forEach(kw => {
            if (content.includes(kw)) {
                console.log(`Keyword "${kw}" found in: ${file}`);
            }
        });
    }
});
