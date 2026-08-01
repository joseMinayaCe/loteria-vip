const axios = require('axios');

async function test(dateParam) {
    try {
        const url = `https://api.loteriasdominicanas.com/dominicana/sites/env?date=${dateParam}`;
        const { data } = await axios.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        let sessionCount = 0;
        if (data && data.siteCompanies) {
            data.siteCompanies.forEach(c => {
                c.siteGames.forEach(g => {
                    if (g.game && g.game.sessions && g.game.sessions.length > 0) {
                        sessionCount += g.game.sessions.length;
                    }
                });
            });
        }
        console.log(`Date parameter "${dateParam}" -> Found ${sessionCount} sessions.`);
    } catch (e) {
        console.log(`Error for "${dateParam}": ${e.message}`);
    }
}

test('2026-06-14T00:00:00.000Z');
test('2026-06-14T12:00:00.000Z');
test('2026-06-14T23:59:59.999Z');
