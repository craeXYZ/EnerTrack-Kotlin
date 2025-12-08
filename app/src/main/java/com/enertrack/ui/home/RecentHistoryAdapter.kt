package com.enertrack.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.enertrack.data.model.HistoryItem
import com.enertrack.databinding.ItemRecentHistoryBinding
import com.enertrack.ui.history.HistoryDiffCallback

class RecentHistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, RecentHistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    class ViewHolder(private val binding: ItemRecentHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem, onItemClick: (HistoryItem) -> Unit) {

            // --- PERBAIKAN NULL SAFETY DI SINI ---
            binding.tvApplianceName.text = item.appliance ?: "N/A"

            // ================== PERBAIKAN ADA DI SINI ==================
            binding.tvCategoryDate.text = "${item.categoryName ?: "N/A"} • ${item.date ?: "N/A"}"
            // ========================================================

            // --- PERBAIKAN NULL SAFETY DI SINI ---
            // Kita hitung ulang dailyKwh di sini untuk tampilan, karena dari API nilainya 0.0
            val power = item.power ?: 0.0
            val usage = item.usage ?: 0.0
            val dailyKwhDisplay = (power * usage) / 1000
            binding.tvDailyKwh.text = "${"%.2f".format(dailyKwhDisplay)} kWh"

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
