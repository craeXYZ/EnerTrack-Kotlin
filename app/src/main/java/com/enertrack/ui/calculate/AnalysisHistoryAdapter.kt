package com.enertrack.ui.calculate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.enertrack.data.model.HistoryItem
import com.enertrack.databinding.ItemAnalysisHistoryBinding

class AnalysisHistoryAdapter(
    private val onToggle: (String) -> Unit
) : ListAdapter<HistoryItem, AnalysisHistoryAdapter.ViewHolder>(DiffCallback()) {

    private var selectedIds = emptySet<String>()

    fun setSelectedIds(ids: Set<String>) {
        selectedIds = ids
        notifyDataSetChanged() // Cara sederhana untuk refresh checkbox
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalysisHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onToggle)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        // Perlu dicek, karena item.id sekarang nullable
        holder.bind(item, selectedIds.contains(item.id))
    }

    class ViewHolder(
        private val binding: ItemAnalysisHistoryBinding,
        private val onToggle: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem, isSelected: Boolean) {
            // --- PERBAIKAN NULL SAFETY UNTUK TEXTVIEW ---
            binding.tvApplianceName.text = item.appliance ?: "N/A"
            binding.tvApplianceDetails.text = "${item.applianceDetails ?: "N/A"} • ${item.categoryName ?: "N/A"}"

            // --- PERBAIKAN NULL SAFETY UNTUK PERHITUNGAN (ERROR BARIS 42, 43) ---
            // Beri nilai default 0.0 jika datanya null
            val power = item.power ?: 0.0
            val usage = item.usage ?: 0.0

            binding.tvApplianceSpecs.text = "${power}W • ${usage}h/day"

            val dailyKwh = (power * usage) / 1000
            val monthlyKwh = dailyKwh * 30
            binding.tvApplianceEnergy.text = "Daily: %.2f kWh • Monthly: %.2f kWh".format(dailyKwh, monthlyKwh)

            // --- PERBAIKAN NULL SAFETY UNTUK LISTENER (ERROR BARIS 49) ---
            // Logika ini penting agar checkbox tidak berantakan saat di-scroll
            binding.checkboxSelect.setOnCheckedChangeListener(null)
            binding.checkboxSelect.isChecked = isSelected
            binding.checkboxSelect.setOnCheckedChangeListener { _, _ ->
                item.id?.let { id -> // Hanya panggil onToggle jika id tidak null
                    onToggle(id)
                }
            }

            // Tambahkan listener klik untuk seluruh item
            itemView.setOnClickListener {
                // performClick() akan memicu OnCheckedChangeListener di atas secara aman
                binding.checkboxSelect.performClick()
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem) = oldItem == newItem
    }
}
