package com.enertrack.ui.calculate

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.enertrack.R
import com.enertrack.data.model.IotDevice
import com.google.android.material.card.MaterialCardView

class IotDeviceAdapter(
    private val onItemClick: (IotDevice) -> Unit
) : RecyclerView.Adapter<IotDeviceAdapter.IotViewHolder>() {

    private val deviceList = ArrayList<IotDevice>()

    fun setData(newList: List<IotDevice>) {
        deviceList.clear()
        deviceList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_iot_device, parent, false)
        return IotViewHolder(view)
    }

    override fun onBindViewHolder(holder: IotViewHolder, position: Int) {
        holder.bind(deviceList[position])
    }

    override fun getItemCount(): Int = deviceList.size

    inner class IotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_device_name)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_device_status)
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_device_icon)
        private val card: MaterialCardView = itemView.findViewById(R.id.card_iot_item)

        fun bind(device: IotDevice) {
            tvName.text = device.device_name

            // --- LOGIKA WARNA STATUS (Update) ---
            when (device.status.uppercase()) {
                "ON" -> {
                    // HIJAU (Aktif)
                    tvStatus.text = "Online • ${device.watt} W"
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                    ivIcon.setColorFilter(Color.parseColor("#2196F3"))
                    ivIcon.setBackgroundResource(R.drawable.bg_rounded_light_green)
                }
                "OFF" -> {
                    // ORANYE (Standby)
                    tvStatus.text = "Standby • 0 W"
                    tvStatus.setTextColor(Color.parseColor("#FF9800")) // Oranye
                    ivIcon.setColorFilter(Color.parseColor("#FF9800")) // Icon Oranye
                    ivIcon.setBackgroundColor(Color.parseColor("#FFF3E0")) // Background oranye muda
                }
                else -> {
                    // ABU-ABU (Offline)
                    tvStatus.text = "Offline"
                    tvStatus.setTextColor(Color.parseColor("#9E9E9E"))
                    ivIcon.setColorFilter(Color.parseColor("#757575"))
                    ivIcon.setBackgroundColor(Color.parseColor("#F5F5F5"))
                }
            }

            itemView.setOnClickListener { onItemClick(device) }
        }
    }
}