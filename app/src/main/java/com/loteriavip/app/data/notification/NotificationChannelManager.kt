package com.loteriavip.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build

object NotificationChannelManager {

    const val CHANNEL_LOTERIAS_RD = "loteria_vip_loterias_rd"
    const val CHANNEL_LOTTOS = "loteria_vip_lottos"
    const val CHANNEL_AMERICANAS = "loteria_vip_americanas"

    private val americanIds = setOf("new-york", "florida", "new-york-tarde", "florida-dia", "florida-noche")
    private val lottoIds = setOf("loto", "loto-mas", "mega-millions", "powerball", "super-kino-tv")

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelLoteriaRd = NotificationChannel(
            CHANNEL_LOTERIAS_RD,
            "Loterías RD",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Resultados de loterías dominicanas (Gana Más, Leidsa, Real, La Primera, Loteka)"
            enableLights(true)
            lightColor = Color.parseColor("#00E676") // green
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val channelLottos = NotificationChannel(
            CHANNEL_LOTTOS,
            "Lottos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Resultados de Loto, Loto Más y otros sorteos de Lotto"
            enableLights(true)
            lightColor = Color.parseColor("#FFD700") // gold
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val channelAmericanas = NotificationChannel(
            CHANNEL_AMERICANAS,
            "Sorteos Americanos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Resultados de sorteos de New York y Florida"
            enableLights(true)
            lightColor = Color.parseColor("#2196F3") // blue
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(channelLoteriaRd)
        notificationManager.createNotificationChannel(channelLottos)
        notificationManager.createNotificationChannel(channelAmericanas)
    }

    fun getChannelForLottery(lotteryId: String): String {
        val id = lotteryId.lowercase()
        return when {
            americanIds.any { id.contains(it) } -> CHANNEL_AMERICANAS
            lottoIds.any { id.contains(it) } -> CHANNEL_LOTTOS
            else -> CHANNEL_LOTERIAS_RD
        }
    }

    fun subscribeToAllDefaultTopics(context: Context) {
        val sharedPrefs = context.getSharedPreferences("loteria_vip_prefs", Context.MODE_PRIVATE)
        val isGlobalEnabled = sharedPrefs.getBoolean("notification_global_enabled", true)
        val defaultSubscriptions = listOf(
            "gana-mas", "leidsa", "real", "la-primera",
            "loteka", "anguila", "king-lottery", "la-suerte", "lotedom",
            "new-york", "florida", "haiti-bolet"
        )

        try {
            if (isGlobalEnabled) {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("loteria-all")
                com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all")
            } else {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("loteria-all")
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
            }
        } catch (e: Throwable) { e.printStackTrace() }

        defaultSubscriptions.forEach { key ->
            val isSubscribed = sharedPrefs.getBoolean("notification_$key", true)
            try {
                if (isGlobalEnabled && isSubscribed) {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("loteria-$key")
                } else {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("loteria-$key")
                }
            } catch (e: Throwable) { e.printStackTrace() }
        }
    }
}
