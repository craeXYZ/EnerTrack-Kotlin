package com.enertrack.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.enertrack.data.model.HistoryItem
import com.enertrack.databinding.BottomSheetHistoryDetailBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale

class HistoryDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHistoryDetailBinding? = null
    private val binding get() = _binding!!

    // Callback untuk memberitahu Fragment utama saat tombol delete diklik
    var onDeleteClick: ((HistoryItem) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ambil data HistoryItem yang dikirim dari Fragment
        val item = arguments?.getSerializable(ARG_HISTORY_ITEM) as? HistoryItem ?: return

        // --- PERBAIKAN NULL SAFETY DI SINI ---
        // Kasih nilai default 0.0 jika datanya null, sebelum dipakai ngitung
        val power = item.power ?: 0.0
        val usage = item.usage ?: 0.0

        // Hitung ulang nilai kWh & Cost untuk ditampilkan (Sekarang udah aman)
        val dailyKwh = (power * usage) / 1000.0
        val monthlyKwh = dailyKwh * 30
        val cost = dailyKwh * 1444.70 // Asumsi tarif standar

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        currencyFormat.maximumFractionDigits = 0
        val formattedCost = currencyFormat.format(cost)

        // --- PERBAIKAN NULL SAFETY DI SINI ---
        // Isi semua view dengan data dari item, kasih nilai default "N/A"
        binding.tvDetailApplianceName.text = item.appliance ?: "N/A"
        binding.tvDetailDate.text = item.date ?: "N/A"
        binding.tvDetailBrand.text = "Brand: ${item.applianceDetails ?: "N/A"}"
        binding.chipDetailCategory.text = item.categoryName ?: "N/A"
        binding.tvDetailPower.text = (item.power ?: 0.0).toString()
        binding.tvDetailUsage.text = (item.usage ?: 0.0).toString()
        binding.tvDetailDailyKwh.text = String.format("%.2f", dailyKwh)
        binding.tvDetailMonthlyKwh.text = String.format("%.2f", monthlyKwh)
        binding.tvDetailCost.text = formattedCost

        // Atur listener untuk tombol delete
        binding.btnDeleteHistory.setOnClickListener {
            onDeleteClick?.invoke(item) // Ini aman, 'item' udah dicek di atas
            dismiss() // Tutup bottom sheet setelah diklik
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "HistoryDetailBottomSheet"
        private const val ARG_HISTORY_ITEM = "history_item"

        // Fungsi helper untuk membuat instance BottomSheet dengan data
        fun newInstance(item: HistoryItem): HistoryDetailBottomSheet {
            val fragment = HistoryDetailBottomSheet()
            fragment.arguments = Bundle().apply {
                putSerializable(ARG_HISTORY_ITEM, item)
            }
            return fragment
        }
    }
}
