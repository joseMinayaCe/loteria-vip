package com.loteriavip.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lotteryId: String,
    val title: String,
    val body: String,
    val numbersJson: String = "[]",  // JSON array of winning numbers
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
