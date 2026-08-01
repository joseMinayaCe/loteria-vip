package com.loteriavip.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.loteriavip.app.data.local.dao.FavoriteDao
import com.loteriavip.app.data.local.dao.LotteryCacheDao
import com.loteriavip.app.data.local.dao.NotificationDao
import com.loteriavip.app.data.local.entity.FavoriteEntity
import com.loteriavip.app.data.local.entity.LotteryCacheEntity
import com.loteriavip.app.data.local.entity.NotificationEntity

@Database(entities = [FavoriteEntity::class, NotificationEntity::class, LotteryCacheEntity::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun notificationDao(): NotificationDao
    abstract fun lotteryCacheDao(): LotteryCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loteria_vip_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
