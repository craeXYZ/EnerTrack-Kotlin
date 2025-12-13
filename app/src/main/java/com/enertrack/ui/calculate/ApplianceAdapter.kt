package com.enertrack.ui.calculate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.enertrack.data.model.Appliance
// --- PERBAIKAN: Ganti import binding ke layout yang baru ---
import com.enertrack.databinding.ItemCalculateDeviceBinding
import java.text.NumberFormat
import java.util.*

class ApplianceAdapter(
    private val onDeleteClick: (Long) -> Unit
) : ListAdapter<Appliance, ApplianceAdapter.ViewHolder>(ApplianceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // --- PERBAIKAN: Inflate layout item yang BARU ---
        val binding = ItemCalculateDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }

    // --- PERBAIKAN: Ganti tipe binding di ViewHolder ---
    class ViewHolder(private val binding: ItemCalculateDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Appliance, onDelete: (Long) -> Unit) {
            // Sekarang kita pake ID yang ada di item_calculate_device.xml (v4/v5)

            binding.tvDeviceName.text = item.name

            // Format detail: "120 Watt • 5 Jam/hari"
            binding.tvDeviceDetails.text = "${item.powerRating} Watt • ${item.dailyUsage} Hours/day"

            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            currencyFormat.maximumFractionDigits = 0
            val formattedCost = currencyFormat.format(item.monthlyCost).replace("Rp", "Rp ")

            binding.tvDeviceCost.text = "Est. $formattedCost"

            // Tombol Delete (ID: btn_delete)
            binding.btnDelete.setOnClickListener { onDelete(item.id) }

            // Icon (ID: iv_device_icon) - Ini yang tadi error
            // Kita pake icon default dulu
            // binding.ivDeviceIcon.setImageResource(android.R.drawable.ic_menu_info_details)
            // Atau hapus baris ini kalau di XML udah di-set default src-nya
        }
    }
}

class ApplianceDiffCallback : DiffUtil.ItemCallback<Appliance>() {
    override fun areItemsTheSame(oldItem: Appliance, newItem: Appliance): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Appliance, newItem: Appliance): Boolean = oldItem == newItem
}