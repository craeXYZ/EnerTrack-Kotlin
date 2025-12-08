package com.enertrack.data.repository

import com.enertrack.data.local.dao.CategoryDao
import com.enertrack.data.local.entity.CategoryEntity
import com.enertrack.data.model.Category // Model dari API-mu
import com.enertrack.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Constructor-nya sekarang nerima ApiService dan CategoryDao
class CategoryRepository(
    private val apiService: ApiService,
    private val categoryDao: CategoryDao
) {

    // 1. FUNGSI BUAT UI (READ)
    // Langsung ambil dari database (DAO).
    // Kalo data di DB berubah, UI otomatis ikut berubah (karena Flow)
    fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { listEntity ->
            // Ubah List<CategoryEntity> (DB) -> List<Category> (Model UI)
            listEntity.map { entity ->
                Category(id = entity.id, name = entity.name)
            }
        }
    }

    // 2. FUNGSI BUAT SINKRONISASI (DARI API KE DB)
    // Nanti dipanggil sama WorkManager atau pas Swipe Refresh
    suspend fun refreshCategories(): Result<Unit> {
        return try {
            // Panggil API
            val response = apiService.getCategories() // Asumsi nama fungsinya ini
            if (response.isSuccessful) {
                val categoriesFromApi = response.body() ?: emptyList()

                // Ubah model API -> model Entity DB
                val entities = categoriesFromApi.map { category ->
                    CategoryEntity(id = category.id, name = category.name)
                }

                // Simpen ke database
                // NGGAK PERLU clearAll(), langsung timpa aja
                categoryDao.insertAll(entities)
                Result.Success(Unit)
            } else {
                Result.Failure(Exception("Failed to fetch categories"))
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}