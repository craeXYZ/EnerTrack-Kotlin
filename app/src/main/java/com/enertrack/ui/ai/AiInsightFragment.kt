package com.enertrack.ui.ai

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.enertrack.R
import com.enertrack.data.model.Tip
import com.enertrack.data.network.RetrofitClient
import com.enertrack.databinding.FragmentAiInsightBinding
import kotlinx.coroutines.launch

class AiInsightFragment : Fragment() {

    private var _binding: FragmentAiInsightBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiInsightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()

        // Panggil data langsung saat halaman dibuka
        fetchInsightData()
    }

    private fun setupListeners() {
        // Tombol Chat
        binding.cardChatAi.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_aiInsight_to_chat)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigation Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        // Tombol Refresh di pojok kanan atas
        binding.btnRefresh.setOnClickListener {
            fetchInsightData()
        }
    }

    private fun fetchInsightData() {
        // Set loading state
        binding.tvGrade.text = "..."
        binding.tvGrade.setTextColor(Color.GRAY)
        binding.tvGradeMessage.text = "Analyzing your usage..."
        binding.layoutTipsContainer.removeAllViews()

        // Pakai coroutine buat nembak API
        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getInstance(requireContext())
                val response = apiService.getInsight()

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    updateUI(data.grade, data.message, data.tips, data.calculationBasis)
                } else {
                    binding.tvGradeMessage.text = "Failed to load data. Code: ${response.code()}"
                    // Kalau 401 Unauthorized, mungkin perlu login ulang
                    if (response.code() == 401) {
                        Toast.makeText(context, "Session expired, please login again", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                binding.tvGradeMessage.text = "Connection Error. Check your internet."
                e.printStackTrace()
            }
        }
    }

    private fun updateUI(grade: String, message: String, tips: List<Tip>, basis: String) {
        // 1. Update Grade Text
        binding.tvGrade.text = grade
        binding.tvGradeMessage.text = message

        // 2. Logic Warna Grade
        val gradeColor = when (grade) {
            "A" -> Color.parseColor("#4CAF50") // Hijau seger
            "B" -> Color.parseColor("#FF9800") // Oranye peringatan
            else -> Color.parseColor("#F44336") // Merah bahaya
        }
        binding.tvGrade.setTextColor(gradeColor)

        // 3. Update Subtitle kalau backend bilang ini proyeksi bulanan
        if (basis == "monthly_projection") {
            binding.tvSubtitle.text = "Monthly Energy Efficiency (Projected)"
        }

        // 4. Masukin Tips ke Layout Container secara dinamis
        binding.layoutTipsContainer.removeAllViews() // Hapus loading bar

        if (tips.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "No tips available yet."
                setTextColor(Color.GRAY)
                setPadding(10, 10, 10, 10)
            }
            binding.layoutTipsContainer.addView(emptyText)
            return
        }

        val inflater = LayoutInflater.from(requireContext())

        for (tip in tips) {
            // Inflate layout item_insight_tip.xml yang baru kita buat
            val tipView = inflater.inflate(R.layout.item_insight_tip, binding.layoutTipsContainer, false)

            // Isi data ke view
            val tvTitle = tipView.findViewById<TextView>(R.id.tvTipTitle)
            val tvDesc = tipView.findViewById<TextView>(R.id.tvTipDesc)
            val imgIcon = tipView.findViewById<ImageView>(R.id.imgTipIcon)

            tvTitle.text = tip.title
            tvDesc.text = tip.description

            // Logic Icon Sederhana
            // Pastikan kamu punya drawable ini, kalau error ganti aja ke ic_lightbulb_outline semua
            val iconRes = when(tip.iconType) {
                "ac" -> R.drawable.ic_lightbulb_outline // Ganti ic_ac_unit kalau ada
                "plug" -> R.drawable.ic_lightbulb_outline // Ganti ic_power kalau ada
                else -> R.drawable.ic_lightbulb_outline
            }
            imgIcon.setImageResource(iconRes)

            // Masukin ke container
            binding.layoutTipsContainer.addView(tipView)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        if (findNavController().currentDestination?.id != R.id.navigation_ai_chat) {
            (activity as? AppCompatActivity)?.supportActionBar?.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}