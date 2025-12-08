package com.enertrack.ui.history

import androidx.recyclerview.widget.DiffUtil
import com.enertrack.data.model.HistoryItem

class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {

    /**
     * Dipanggil untuk memeriksa apakah dua objek mewakili item yang sama.
     * Biasanya membandingkan ID unik dari item.
     */
    override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        // Ganti 'id' dengan properti ID unik yang ada di model HistoryItem Anda
        return oldItem.id == newItem.id
    }

    /**
     * Dipanggil untuk memeriksa apakah data dari dua item sama.
     * Dipanggil hanya jika areItemsTheSame() mengembalikan true.
     */
    override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
        // Membandingkan semua properti dari data class.
        // Jika HistoryItem adalah data class, ini sudah cukup.
        return oldItem == newItem
    }
}
