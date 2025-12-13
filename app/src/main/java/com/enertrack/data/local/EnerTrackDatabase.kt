package com.enertrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.enertrack.data.local.dao.CategoryDao
import com.enertrack.data.local.dao.HistoryDao
import com.enertrack.data.local.entity.CategoryEntity
import com.enertrack.data.local.entity.HistoryItemEntity

@Database(
    entities = [
        CategoryEntity::class,
        HistoryItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EnerTrackDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: EnerTrackDatabase? = null

        fun getDatabase(context: Context): EnerTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EnerTrackDatabase::class.java,
                    "enertrack_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // === TAMBAHAN BARU: FUNGSI BUAT MATIIN DB ===
        fun destroyInstance() {
            try {
                if (INSTANCE?.isOpen == true) {
                    INSTANCE?.close() // Tutup koneksi biar file nggak dikunci
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            INSTANCE = null // Reset variable biar nanti bikin baru lagi
        }
    }
}