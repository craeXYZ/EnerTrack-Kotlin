package com.enertrack.ui.calculate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
            val context = itemView.context

            // Logika Status:
            // DANGER -> Voltage > 250
            // ON -> Watt > 0
            // OFF/Standby -> Watt == 0

            val isHighVoltage = device.voltase > 250.0

            val displayStatus = when {
                isHighVoltage -> "DANGER"
                device.watt > 0 -> "ON"
                else -> "OFF"
            }

            // Siapkan warna putih untuk TINT icon biar kontras
            val colorWhite = ContextCompat.getColor(context, R.color.white)

            when (displayStatus) {
                "DANGER" -> {
                    // MERAH
                    tvStatus.text = "Danger • ${device.voltase} V"
                    val solidRed = ContextCompat.getColor(context, R.color.dangerRed)
                    val bgRed = ContextCompat.getColor(context, R.color.bgLightRed)

                    // [UPDATED] Container Icon jadi Merah Solid, Icon-nya jadi Putih
                    ivIcon.setBackgroundColor(bgRed)
                    ivIcon.setColorFilter(solidRed)

                    // Text warna merah
                    tvStatus.setTextColor(solidRed)

                    // Card Background Soft
                    card.setCardBackgroundColor(bgRed)
                }
                "ON" -> {
                    // HIJAU
                    tvStatus.text = "Active • ${device.watt} W"
                    val solidGreen = ContextCompat.getColor(context, R.color.energyGreen)
                    val bgGreen = ContextCompat.getColor(context, R.color.bgLightGreen)

                    // [UPDATED] Container Icon jadi Hijau Solid, Icon-nya jadi Putih
                    ivIcon.setBackgroundColor(bgGreen)
                    ivIcon.setColorFilter(solidGreen)

                    // Text warna hijau
                    tvStatus.setTextColor(solidGreen)

                    // Card Background Soft
                    card.setCardBackgroundColor(bgGreen)
                }
                "OFF" -> {
                    // ORANGE
                    tvStatus.text = "Standby • 0 W"
                    val solidOrange = ContextCompat.getColor(context, R.color.energyOrange)
                    val bgOrange = ContextCompat.getColor(context, R.color.bgLightOrange)

                    // [UPDATED] Container Icon jadi Orange Solid, Icon-nya jadi Putih
                    ivIcon.setBackgroundColor(bgOrange)
                    ivIcon.setColorFilter(solidOrange)

                    // Text warna orange
                    tvStatus.setTextColor(solidOrange)

                    // Card Background Soft
                    card.setCardBackgroundColor(bgOrange)
                }
                else -> {
                    // ABU-ABU (Offline)
                    tvStatus.text = "Offline"
                    val solidGrey = ContextCompat.getColor(context, R.color.text_secondary)
                    val bgGrey = ContextCompat.getColor(context, R.color.neutral_100)

                    // [UPDATED] Container Icon jadi Abu Solid, Icon-nya jadi Putih
                    ivIcon.setBackgroundColor(bgGrey)
                    ivIcon.setColorFilter(solidGrey)

                    // Text warna abu
                    tvStatus.setTextColor(solidGrey)

                    // Card Background Soft
                    card.setCardBackgroundColor(bgGrey)
                }
            }

            itemView.setOnClickListener { onItemClick(device) }
        }
    }
}