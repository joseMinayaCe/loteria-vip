const axios = require('axios');

async function test(dateParam) {
    try {
        const url = `https://api.loteriasdominicanas.com/dominicana/sites/env?date=${dateParam}`;
        const { data } = await axios.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } });
        const dates = new Set();
        if (data && data.siteCompanies) {
            data.siteCompanies.forEach(c => {
                c.siteGames.forEach(g => {
                    if (g.game && g.game.sessions) {
                        g.game.sessions.forEach(s => {
                            if (s.date) {
                                dates.add(s.date);
                            }
                        });
                    }
                });
            });
        }
        console.log(`Date parameter "${dateParam}":`);
        console.log(`Unique session dates:`, Array.from(dates).sort());
    } catch (e) {
        console.log(`Error for "${dateParam}": ${e.message}`);
    }
}

async function run() {
    await test('2026-06-14T00:00:00.000Z');
    await test('2026-06-14T04:00:00.000Z');
    await test('2026-06-14T12:00:00.000Z');
    await test('2026-06-14T18:00:00.000Z');
    await test('2026-06-14T23:59:59.999Z');
}

run();
