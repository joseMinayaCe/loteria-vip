package com.loteriavip.app.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.loteriavip.app.MainActivity
import com.loteriavip.app.R
import com.loteriavip.app.data.local.AppDatabase
import com.loteriavip.app.data.local.entity.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        NotificationChannelManager.subscribeToAllDefaultTopics(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "========== FCM MESSAGE RECEIVED ==========")
        Log.d("FCM", "From: ${remoteMessage.from}")
        Log.d("FCM", "Data payload: ${remoteMessage.data}")
        Log.d("FCM", "Notification payload: ${remoteMessage.notification?.title} / ${remoteMessage.notification?.body}")

        // Priority 1: data payload (works in foreground AND background via onMessageReceived)
        if (remoteMessage.data.isNotEmpty()) {
            val lotteryId = remoteMessage.data["lotteryId"] ?: ""
            val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Resultado de Lotería"
            val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: "¡Nuevos números disponibles!"
            val numbersJson = remoteMessage.data["numbers"] ?: "[]"
            Log.d("FCM", "Processing data payload: title=$title, lotteryId=$lotteryId, numbers=$numbersJson")
            sendNotification(title, body, lotteryId, numbersJson)
        } else if (remoteMessage.notification != null) {
            val title = remoteMessage.notification?.title ?: "Resultado de Lotería"
            val body = remoteMessage.notification?.body ?: "¡Nuevos números!"
            Log.d("FCM", "Processing notification-only payload (foreground): title=$title")
            sendNotification(title, body, "", "[]")
        } else {
            Log.w("FCM", "Received message with no data and no notification payload")
        }
    }

    private fun getCategoryKey(lotteryId: String): String {
        val lower = lotteryId.lowercase().replace("í", "i").replace("á", "a")
        return when {
            lower.contains("gana") || lower.contains("nacional") -> "gana-mas"
            lower.contains("leidsa") -> "leidsa"
            lower.contains("real") -> "real"
            lower.contains("primera") -> "la-primera"
            lower.contains("loteka") -> "loteka"
            lower.contains("anguila") || lower.contains("anguilla") -> "anguila"
            lower.contains("king") -> "king-lottery"
            lower.contains("suerte") -> "la-suerte"
            lower.contains("lotedom") -> "lotedom"
            lower.contains("york") || lower.contains("ny") -> "new-york"
            lower.contains("florida") -> "florida"
            lower.contains("haiti") || lower.contains("bolet") -> "haiti-bolet"
            else -> {
                val clean = lower.removePrefix("loteria-")
                if (clean.isNotEmpty()) clean else "gana-mas"
            }
        }
    }

    private fun isNotificationEnabledForLottery(lotteryId: String): Boolean {
        val prefs = getSharedPreferences("loteria_vip_prefs", Context.MODE_PRIVATE)
        val isGlobalEnabled = prefs.getBoolean("notification_global_enabled", true)
        if (!isGlobalEnabled) return false
        if (lotteryId.isEmpty()) return true
        val categoryKey = getCategoryKey(lotteryId)
        return prefs.getBoolean("notification_$categoryKey", true)
    }

    private fun sendNotification(title: String, messageBody: String, lotteryId: String, numbersJson: String) {
        if (!isNotificationEnabledForLottery(lotteryId)) {
            Log.d("FCM", "Notification suppressed by user preferences: lotteryId=$lotteryId")
            return
        }
        // Save to Room DB so it appears in notification history
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@MyFirebaseMessagingService)
                db.notificationDao().insert(
                    NotificationEntity(
                        lotteryId = lotteryId,
                        title = title,
                        body = messageBody,
                        numbersJson = numbersJson
                    )
                )
                Log.d("FCM", "Notification saved to DB: title=$title")
            } catch (e: Exception) {
                Log.e("FCM", "Failed to save notification to DB", e)
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("LOTTERY_ID", lotteryId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationChannelManager.createChannels(this)
        val channelId = NotificationChannelManager.getChannelForLottery(lotteryId)
        
        // Parse numbers to build a clean display string
        var collapsedText = messageBody  // short text (collapsed bar)
        var expandedText = messageBody   // long text (expanded notification)
        
        if (numbersJson.isNotBlank() && numbersJson != "[]" && numbersJson != "null") {
            try {
                val cleaned = numbersJson.trim()
                val allNums: List<String> = when {
                    cleaned.startsWith("[") -> {
                        // JSON array: [1,2,3]
                        cleaned.removePrefix("[").removeSuffix("]")
                            .split(",")
                            .map { it.trim().removeSurrounding("\"") }
                            .filter { it.isNotEmpty() }
                    }
                    else -> cleaned.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                // Format each number with 2 digits and take first 2 for collapsed view
                val formattedNums = allNums.mapNotNull { 
                    it.toIntOrNull()?.let { n -> n.toString().padStart(2, '0') }
                }

                if (formattedNums.isNotEmpty()) {
                    collapsedText = "🎰 ${formattedNums.take(2).joinToString(" - ")}"
                    expandedText  = if (formattedNums.size > 2) {
                        "🎰 Números ganadores: ${formattedNums.joinToString(" - ")}"
                    } else {
                        "🎰 Números ganadores: ${formattedNums.joinToString(" - ")}"
                    }
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error parsing numbersJson: $numbersJson", e)
            }
        }

        val notificationId = System.currentTimeMillis().toInt()
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(collapsedText)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(expandedText)
                .setBigContentTitle(title))
            .setAutoCancel(true)
            .setColor(Color.parseColor("#005943"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d("FCM", "System notification shown: id=$notificationId, channel=$channelId")
    }
}
