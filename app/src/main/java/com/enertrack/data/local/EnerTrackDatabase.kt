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
        // Tambahin entity lain di sini kalo ada (misal DeviceEntity)
    ],
    // --- UBAH ANGKA INI JADI 2 ---
    version = 2, // Naikkan versi ini kalo kamu ubah struktur tabel
    // -----------------------------
    exportSchema = false
)
abstract class EnerTrackDatabase : RoomDatabase() {

    // Daftarin semua DAO di sini
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
                    "enertrack_database" // Nama file DB-nya
                )
                    // Ini udah bener, biarin aja. Pas ganti versi, data lama dihapus (aman buat development)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
