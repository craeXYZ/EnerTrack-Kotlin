package com.enertrack.ui.ai

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        fetchInsightData()
    }

    private fun setupListeners() {
        binding.cardChatAi.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_aiInsight_to_chat)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigation Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        binding.swipeRefreshInsight.setOnRefreshListener {
            fetchInsightData()
        }
    }

    private fun fetchInsightData() {
        showLoadingState()

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getInstance(requireContext())

                // 1. CEK ALAT ELEKTRONIK DULU (PENTING!)
                // User baru ("Rappi") pasti list-nya kosong.
                // Kita cegah dia dapet Grade A palsu (karena 0 kwh dianggap efisien).
                val applianceResponse = apiService.getUserAppliances()
                val hasAppliances = if (applianceResponse.isSuccessful) {
                    !applianceResponse.body().isNullOrEmpty()
                } else {
                    true // Kalau gagal cek, anggap aja ada biar lanjut (fallback)
                }

                // Kalau alat kosong, langsung STOP dan tampilkan Empty State
                if (!hasAppliances) {
                    binding.swipeRefreshInsight.isRefreshing = false
                    showEmptyState()
                    return@launch
                }

                // 2. Kalau punya alat, baru ambil Insight
                val response = apiService.getInsight()

                binding.swipeRefreshInsight.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Cek double protection
                    if (data.grade.isNullOrBlank() || data.grade.equals("N/A", ignoreCase = true)) {
                        showEmptyState()
                    } else {
                        updateUI(data.grade, data.message, data.tips, data.calculationBasis)
                    }
                } else {
                    showErrorState("Failed to load analysis. (${response.code()})")
                    if (response.code() == 401) {
                        Toast.makeText(context, "Session expired, please login again", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                binding.swipeRefreshInsight.isRefreshing = false
                showErrorState("Check your internet connection.")
                e.printStackTrace()
            }
        }
    }

    // === STATE HANDLING ===

    private fun showLoadingState() {
        binding.tvGrade.text = "..."
        binding.tvGrade.setTextColor(Color.GRAY)
        binding.tvGradeMessage.text = "Analyzing your usage..."
        binding.layoutTipsContainer.removeAllViews()
    }

    private fun showEmptyState() {
        binding.tvGrade.text = "-"
        binding.tvGrade.setTextColor(Color.parseColor("#BDBDBD")) // Abu-abu
        binding.tvGradeMessage.text = "Not enough data for analysis."
        binding.tvSubtitle.text = "Energy Efficiency" // Reset subtitle

        // Tampilkan pesan di kotak tips
        binding.layoutTipsContainer.removeAllViews()
        val emptyText = TextView(requireContext()).apply {
            text = "Start adding your electronic devices in the Home menu to get AI insights & efficiency grade."
            setTextColor(Color.GRAY)
            setPadding(32, 32, 32, 32)
            gravity = Gravity.CENTER
            textSize = 14f
        }
        binding.layoutTipsContainer.addView(emptyText)
    }

    private fun showErrorState(message: String) {
        binding.tvGrade.text = "!"
        binding.tvGrade.setTextColor(Color.parseColor("#E57373")) // Merah muda
        binding.tvGradeMessage.text = message
    }

    // === UPDATE UI ===

    private fun updateUI(grade: String, message: String, tips: List<Tip>, basis: String?) {
        binding.tvGrade.text = grade
        binding.tvGradeMessage.text = message

        val gradeColor = when (grade.uppercase()) {
            "A" -> Color.parseColor("#4CAF50")
            "B" -> Color.parseColor("#FF9800")
            "C" -> Color.parseColor("#F44336")
            "D", "E" -> Color.parseColor("#B71C1C")
            else -> Color.parseColor("#9E9E9E")
        }
        binding.tvGrade.setTextColor(gradeColor)

        if (basis == "monthly_projection" || basis == "monthly_projection_history") {
            binding.tvSubtitle.text = "Monthly Energy Efficiency (Projected)"
        } else {
            binding.tvSubtitle.text = "Energy Efficiency"
        }

        binding.layoutTipsContainer.removeAllViews()

        if (tips.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "No specific tips available at the moment. Keep up the good work!"
                setTextColor(Color.GRAY)
                setPadding(32, 32, 32, 32)
                gravity = Gravity.CENTER
            }
            binding.layoutTipsContainer.addView(emptyText)
            return
        }

        val inflater = LayoutInflater.from(requireContext())

        for (tip in tips) {
            val tipView = inflater.inflate(R.layout.item_insight_tip, binding.layoutTipsContainer, false)

            val tvTitle = tipView.findViewById<TextView>(R.id.tvTipTitle)
            val tvDesc = tipView.findViewById<TextView>(R.id.tvTipDesc)
            val imgIcon = tipView.findViewById<ImageView>(R.id.imgTipIcon)

            tvTitle.text = tip.title
            tvDesc.text = tip.description

            // Set icon default biar nggak null
            imgIcon.setImageResource(R.drawable.ic_lightbulb_outline)

            binding.layoutTipsContainer.addView(tipView)
        }
    }

    override fun onResume() {
        super.onResume()
        val actionBar = (activity as? AppCompatActivity)?.supportActionBar
        actionBar?.show()
        actionBar?.title = "AI Energy Insight"
        actionBar?.setDisplayHomeAsUpEnabled(false)
        actionBar?.setHomeButtonEnabled(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}