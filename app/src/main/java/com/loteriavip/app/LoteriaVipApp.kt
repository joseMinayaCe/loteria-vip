package com.loteriavip.app

import android.app.Application
import com.loteriavip.app.data.notification.NotificationChannelManager

class LoteriaVipApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannelManager.createChannels(this)
        // Ensure FCM topic subscriptions are active on every app start
        // (onNewToken only fires when the token changes, not on every launch)
        NotificationChannelManager.subscribeToAllDefaultTopics(this)
    }
}
