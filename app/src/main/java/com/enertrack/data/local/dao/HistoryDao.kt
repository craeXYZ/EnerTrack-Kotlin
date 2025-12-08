package com.enertrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enertrack.data.local.entity.HistoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    // 1. BUAT DITAMPILIN KE USER
    // Ambil semua riwayat KECUALI yang ditandai "mau dihapus"
    @Query("SELECT * FROM history_items WHERE statusSync != 'PENDING_DELETE' ORDER BY date DESC")
    fun getVisibleHistory(): Flow<List<HistoryItemEntity>>

    // 2. BUAT NANDA-IN HAPUS (Offline)
    // Cuma update status, BUKAN beneran hapus
    @Query("UPDATE history_items SET statusSync = 'PENDING_DELETE' WHERE id = :itemId")
    suspend fun markForDeletion(itemId: String)

    // 3. BUAT HAPUS BENERAN (Setelah sync ke server sukses)
    @Query("DELETE FROM history_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String)

    // 4. BUAT MASUKKIN DATA (Baru atau Update)
    // Kunci di OnConflictStrategy.REPLACE:
    // - Kalo ID udah ada, datanya di-update
    // - Kalo ID belum ada, data baru dimasukin
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HistoryItemEntity>)

    // 5. BUAT WORKMANAGER (Ambil data yg belum sinkron)
    @Query("SELECT * FROM history_items WHERE statusSync != 'SYNCED'")
    suspend fun getUnsyncedItems(): List<HistoryItemEntity>

    // 6. BUAT HAPUS SEMUA DATA LOKAL (misal pas Logout)
    @Query("DELETE FROM history_items")
    suspend fun clearAll()
}