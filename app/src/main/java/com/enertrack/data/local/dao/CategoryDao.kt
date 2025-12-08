package com.enertrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enertrack.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // Ambil semua kategori, otomatis update UI kalo data berubah (via Flow)
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    // Masukkan daftar kategori, timpa data lama kalo ID-nya sama
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    // Hapus semua kategori (dipakai jarang-jarang)
    @Query("DELETE FROM categories")
    suspend fun clearAll()
}