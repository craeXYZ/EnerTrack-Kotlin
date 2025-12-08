package com.enertrack.ui.history

import androidx.lifecycle.*
import com.enertrack.data.model.HistoryItem
import com.enertrack.data.repository.HistoryRepository
import com.enertrack.data.repository.Result // <-- IMPORT BARU (atau udah ada)
import com.enertrack.data.repository.onFailure
import com.enertrack.data.repository.onSuccess
import kotlinx.coroutines.flow.collectLatest // <-- IMPORT BARU
import kotlinx.coroutines.launch

class HistoryViewModel(private val historyRepository: HistoryRepository) : ViewModel() {

    // --- State untuk UI ---
    val isLoading = MutableLiveData<Boolean>()
    val paginatedHistoryList = MutableLiveData<List<HistoryItem>>()
    val toastMessage = MutableLiveData<String?>()

    // --- State untuk Filter & Pagination ---
    private var originalList: List<HistoryItem> = emptyList()
    val searchQuery = MutableLiveData("")
    val selectedCategory = MutableLiveData("All") // Default ke "All"
    val currentPage = MutableLiveData(1)
    val totalPages = MutableLiveData(1)
    private val itemsPerPage = 10 // Atur berapa item per halaman

    init {
        // Observer ini akan otomatis memfilter ulang daftar setiap kali ada perubahan
        val filterObserver = Observer<Any> { applyFiltersAndPagination() }
        searchQuery.observeForever(filterObserver)
        selectedCategory.observeForever(filterObserver)
        currentPage.observeForever(filterObserver)

        // --- PERUBAHAN DI SINI ---
        // Mulai dengerin database secara permanen
        observeHistoryDatabase()

        // Ambil data terbaru dari server pas pertama kali ViewModel dibuat
        fetchHistory()
        // ------------------------
    }

    // --- INI FUNGSI BARU ---
    // Fungsi ini "mendengarkan" database (Room) secara non-stop
    // Kalo ada data baru (dari server) atau data dihapus (dari UI),
    // dia akan otomatis update 'originalList'
    private fun observeHistoryDatabase() {
        viewModelScope.launch {
            historyRepository.getHistoryList().collectLatest { dataFromDb ->
                // Data dari DB (DAO) udah diurutkan, jadi gak perlu sort lagi
                originalList = dataFromDb
                applyFiltersAndPagination() // Terapkan filter dan halaman
                isLoading.value = false // Sembunyikan loading kalo ada data baru
            }
        }
    }

    // --- INI FUNGSI YANG DI-UPDATE ---
    // Sekarang, fungsi ini tugasnya cuma 'memicu' sync ke server
    fun fetchHistory() {
        viewModelScope.launch {
            isLoading.value = true
            // Panggil fungsi sync BARU dari repository
            val syncResult = historyRepository.syncHistoryFromServer()

            // Kalo sync-nya GAGAL, kita matiin loading & kasih tau user
            // Kalo sync SUKSES, 'observeHistoryDatabase' akan otomatis
            // nangkep datanya dan matiin loading.
            if (syncResult is Result.Failure) {
                toastMessage.value = syncResult.exception.message ?: "Failed to load history"
                isLoading.value = false
            }
        }
    }

    // --- INI FUNGSI YANG DI-UPDATE ---
    fun deleteItem(item: HistoryItem) {
        // --- PERBAIKAN ERROR BARIS 80 DI SINI ---
        // Cuma jalankan delete KALO item.id-nya GAK null
        item.id?.let { idToDelete ->
            viewModelScope.launch {
                // Panggil fungsi delete BARU (yang offline-first)
                historyRepository.deleteHistoryItem(idToDelete) // Pakai idToDelete (non-null)
                    .onSuccess {
                        // Jika berhasil (ditandai di DB), tampilkan pesan
                        toastMessage.value = "'${item.appliance ?: "Item"}' record deleted" // Kasih default "Item"

                        // NGGAK PERLU PANGGIL fetchHistory() LAGI!
                        // 'observeHistoryDatabase' akan otomatis tau ada data
                        // yang berubah (karena statusnya jadi PENDING_DELETE)
                        // dan UI akan ke-update sendiri.
                    }
                    .onFailure { error ->
                        // Jika gagal, tampilkan pesan error
                        toastMessage.value = error.message ?: "Failed to delete item"
                    }
            }
        } ?: run {
            // Ini kalo ID-nya ternyata null
            toastMessage.value = "Cannot delete item with no ID"
        }
    }

    private fun applyFiltersAndPagination() {
        var filteredList = originalList

        // 1. Filter berdasarkan kategori
        val category = selectedCategory.value
        if (category != null && category != "All") {
            filteredList = filteredList.filter { (it.categoryName ?: "").equals(category, ignoreCase = true) } // Kasih default ""
        }

        // 2. Filter berdasarkan pencarian
        val query = searchQuery.value
        if (!query.isNullOrBlank()) {
            // --- PERBAIKAN ERROR BARIS 109 DI SINI ---
            // Kasih nilai default "" ke it.appliance sebelum di .contains
            filteredList = filteredList.filter { (it.appliance ?: "").contains(query, ignoreCase = true) }
        }

        // 3. Atur Halaman (Pagination)
        val page = currentPage.value ?: 1
        totalPages.value = maxOf(1, (filteredList.size + itemsPerPage - 1) / itemsPerPage)

        val startIndex = (page - 1) * itemsPerPage
        val endIndex = minOf(startIndex + itemsPerPage, filteredList.size)

        paginatedHistoryList.value = if (startIndex < filteredList.size) {
            filteredList.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    // ================== FUNGSI YANG HILANG SEBELUMNYA ==================
    fun goToNextPage() {
        if ((currentPage.value ?: 1) < (totalPages.value ?: 1)) {
            currentPage.value = (currentPage.value ?: 1) + 1
        }
    }

    fun goToPreviousPage() {
        if ((currentPage.value ?: 1) > 1) {
            currentPage.value = (currentPage.value ?: 1) - 1
        }
    }

    // Untuk menandai bahwa pesan Toast sudah ditampilkan
    fun onToastShown() {
        toastMessage.value = null
    }
    // ====================================================================

    override fun onCleared() {
        super.onCleared()
        // Hapus observer untuk menghindari memory leak
        searchQuery.removeObserver { }
        selectedCategory.removeObserver { }
        currentPage.removeObserver { }
    }

    fun onRefresh() {
        // Cukup panggil ulang fungsi utama yang mengambil data
        fetchHistory()
    }
}
