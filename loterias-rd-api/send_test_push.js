const axios = require('axios');

async function getAccessToken() {
    const refreshToken = "1//05q1gFV5WscBACgYIARAAGAUSNwF-L9Irxd_C4aHVGAmk0sX9gdbraxD1y8jJnXR4TguWOCFvJHHtG6qdG_yH6nX0jb3g5Po2UiM";
    const clientId = "563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com";
    
    try {
        const response = await axios.post('https://oauth2.googleapis.com/token', {
            client_id: clientId,
            client_secret: "j9iVZfS8kkCEFUPaAeJV0sAi",
            grant_type: 'refresh_token',
            refresh_token: refreshToken
        });
        return response.data.access_token;
    } catch (error) {
        console.error('Error refreshing token:', error.response ? error.response.data : error.message);
        throw error;
    }
}

async function sendPushNotification(accessToken) {
    const projectId = "web-to-app-loteria";
    const topic = "loteria-gana-mas";
    const url = `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`;
    
    const message = {
        message: {
            topic: topic,
            notification: {
                title: "¡Prueba Gana Más exitosa! 🔔",
                body: "Felicidades, las notificaciones están funcionando bien."
            },
            data: {
                lotteryId: "gana-mas",
                title: "¡Prueba Gana Más exitosa! 🔔",
                body: "Felicidades, las notificaciones están funcionando bien.",
                numbers: "[12, 34, 56]"
            }
        }
    };

    try {
        const response = await axios.post(url, message, {
            headers: {
                'Authorization': `Bearer ${accessToken}`,
                'Content-Type': 'application/json'
            }
        });
        console.log('FCM Send Success:', response.data);
    } catch (error) {
        console.error('FCM Send Error:', error.response ? error.response.data : error.message);
    }
}

async function main() {
    try {
        console.log('Refreshing OAuth2 token...');
        const token = await getAccessToken();
        console.log('Successfully obtained access token.');
        console.log('Sending test push notification to topic "loteria-gana-mas"...');
        await sendPushNotification(token);
    } catch (e) {
        console.error('Main function failed:', e);
    }
}

main();
