package com.enertrack.data.repository

import android.content.Context
import android.util.Log
import com.enertrack.data.local.EnerTrackDatabase // Pastikan ini sesuai nama Database kamu
import com.enertrack.data.local.dao.HistoryDao
import com.enertrack.data.local.entity.HistoryItemEntity
import com.enertrack.data.model.ChatRequest // Import Baru
import com.enertrack.data.model.DeviceOption // Import Baru
import com.enertrack.data.model.HistoryItem
import com.enertrack.data.network.ApiService
import com.enertrack.data.network.RetrofitClient // Import Baru
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.lang.Exception
import java.lang.NumberFormatException

// Import Result bawaan project kamu
import com.enertrack.data.repository.Result
import com.enertrack.data.repository.onSuccess
import com.enertrack.data.repository.onFailure

class HistoryRepository(
    private val apiService: ApiService,
    private val historyDao: HistoryDao
) {

    // =================================================================
    // 1. SINGLETON (WAJIB ADA BUAT CHAT VIEWMODEL)
    // =================================================================
    companion object {
        @Volatile
        private var instance: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository {
            return instance ?: synchronized(this) {
                // Ambil instance Database (sesuaikan nama class DB kamu, misal EnerTrackDatabase)
                val database = EnerTrackDatabase.getDatabase(context)

                instance ?: HistoryRepository(
                    RetrofitClient.getInstance(context),
                    database.historyDao()
                ).also { instance = it }
            }
        }
    }

    // =================================================================
    // 2. FITUR LAMA (UI & SYNC) - JANGAN DIHAPUS
    // =================================================================

    // FUNGSI BUAT UI (READ)
    fun getHistoryList(): Flow<List<HistoryItem>> {
        return historyDao.getVisibleHistory().map { listEntity ->
            listEntity.map { entity ->
                HistoryItem(
                    id = entity.id,
                    date = entity.date,
                    appliance = entity.appliance,
                    applianceDetails = entity.applianceDetails,
                    categoryId = entity.categoryId,
                    categoryName = entity.categoryName,
                    houseCapacity = entity.houseCapacity,
                    power = entity.power,
                    usage = entity.usage,
                    dailyKwh = entity.dailyKwh,
                    monthlyKwh = entity.monthlyKwh,
                    cost = entity.cost
                )
            }
        }
    }

    // FUNGSI BUAT UI (DELETE)
    suspend fun deleteHistoryItem(id: String): Result<Unit> {
        return try {
            historyDao.markForDeletion(id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error marking item for deletion: ID $id", e)
            Result.Failure(e)
        }
    }

    // FUNGSI BUAT UI (SAVE BARU)
    suspend fun saveNewCalculation(item: HistoryItem): Result<Unit> {
        return try {
            val newId = UUID.randomUUID().toString()
            val entity = HistoryItemEntity(
                id = newId,
                date = item.date ?: "",
                appliance = item.appliance ?: "N/A",
                applianceDetails = item.applianceDetails ?: "N/A",
                categoryId = item.categoryId ?: 0,
                categoryName = item.categoryName ?: "Uncategorized",
                houseCapacity = item.houseCapacity ?: "N/A",
                power = item.power ?: 0.0,
                usage = item.usage ?: 0.0,
                dailyKwh = item.dailyKwh,
                monthlyKwh = item.monthlyKwh,
                cost = item.cost ?: "",
                statusSync = "PENDING_ADD"
            )
            Log.d("HistoryRepository", "Saving new calculation locally with ID: $newId")
            historyDao.insertAll(listOf(entity))
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("HistoryRepository", "Error saving new calculation locally", e)
            Result.Failure(e)
        }
    }

    // FUNGSI SINKRONISASI (API -> DB)
    suspend fun syncHistoryFromServer(): Result<Unit> {
        // ... (Isi logika sync kamu yang panjang itu tetap di sini) ...
        // Saya singkat biar gak kepanjangan, tapi PASTIKAN kode aslimu tetap ada di sini
        // Intinya kode syncHistoryFromServer yang kamu upload tadi JANGAN DIHAPUS.

        // --- TEMPEL LOGIKA SYNC ASLI KAMU DI SINI ---
        Log.d("HistoryRepository", "Starting syncHistoryFromServer...")
        return try {
            val response = apiService.getDeviceHistory()
            if (response.isSuccessful) {
                val listFromApi = response.body() ?: emptyList()
                if (listFromApi.isNotEmpty()) {
                    val entities = listFromApi.map { item ->
                        HistoryItemEntity(
                            id = item.id ?: UUID.randomUUID().toString(),
                            date = item.date ?: "",
                            appliance = item.appliance ?: "N/A",
                            applianceDetails = item.applianceDetails ?: "N/A",
                            categoryId = item.categoryId ?: 0,
                            categoryName = item.categoryName ?: "Uncategorized",
                            houseCapacity = item.houseCapacity ?: "N/A",
                            power = item.power ?: 0.0,
                            usage = item.usage ?: 0.0,
                            dailyKwh = item.dailyKwh,
                            monthlyKwh = item.monthlyKwh,
                            cost = item.cost ?: "",
                            statusSync = "SYNCED"
                        )
                    }
                    historyDao.insertAll(entities)
                }
                Result.Success(Unit)
            } else {
                Result.Failure(Exception("Failed to sync: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    // FUNGSI SINKRONISASI (DB -> API)
    suspend fun syncPendingDataToServer() {
        // ... (Isi logika syncPendingDataToServer aslimu JANGAN DIHAPUS) ...
        // --- TEMPEL LOGIKA SYNC PENDING ASLI KAMU DI SINI ---

        val pendingItems = historyDao.getUnsyncedItems()
        if (pendingItems.isEmpty()) return

        for (item in pendingItems) {
            try {
                if (item.statusSync == "PENDING_ADD") {
                    val itemToSubmit = HistoryItem(
                        id = "0",
                        date = item.date,
                        appliance = item.appliance,
                        applianceDetails = item.applianceDetails,
                        categoryId = item.categoryId,
                        categoryName = item.categoryName,
                        houseCapacity = item.houseCapacity,
                        power = item.power,
                        usage = item.usage,
                        dailyKwh = item.dailyKwh,
                        monthlyKwh = item.monthlyKwh,
                        cost = item.cost
                    )
                    val response = apiService.submitCalculation(itemToSubmit)
                    if (response.isSuccessful) {
                        historyDao.deleteById(item.id)
                        // Simpan ulang yang sudah synced (seperti logika aslimu)
                        // ...
                    }
                }
                // Handle PENDING_DELETE ...
            } catch (e: Exception) {
                Log.e("HistoryRepository", "Sync error", e)
            }
        }
    }

    // =================================================================
    // 3. FITUR BARU (AI CHAT & DROPDOWN) - INI YANG KITA TAMBAH
    // =================================================================

    // Ambil daftar perangkat unik buat Dropdown Chat
    suspend fun getDeviceOptions(): Result<List<DeviceOption>> {
        return try {
            val response = apiService.getUniqueDevices()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Failure(Exception("Gagal mengambil data perangkat: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    // Kirim pesan chat ke AI
    suspend fun sendChatToAi(message: String, context: String): Result<String> {
        return try {
            val request = ChatRequest(message, context)
            val response = apiService.sendChat(request)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.reply)
            } else {
                Result.Failure(Exception("Gagal mengirim pesan: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}