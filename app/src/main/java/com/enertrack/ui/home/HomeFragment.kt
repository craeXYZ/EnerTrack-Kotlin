package com.enertrack.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // <-- IMPORT BARU
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.enertrack.R
import com.enertrack.data.model.ChartDataPoint // <-- IMPORT BARU
import com.enertrack.databinding.FragmentHomeBinding
// import com.enertrack.databinding.LayoutSummaryCardBinding <-- UDAH NGGAK DIPAKE
import com.enertrack.util.UIState // <-- IMPORT BARU
import java.text.NumberFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(requireContext())
    }

    private lateinit var recentHistoryAdapter: RecentHistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Panggil onRefresh() sekali aja pas fragment dibuat
        // (setelah session check di ViewModel selesai)
        // viewModel.onRefresh() <-- Ini udah dipindah ke ViewModel
    }

    private fun setupRecyclerView() {
        recentHistoryAdapter = RecentHistoryAdapter {
            // TODO: Bikin aksi klik di item history
            // findNavController().navigate(R.id.navigation_history)
        }
        binding.recyclerViewRecentHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentHistoryAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onRefresh()
        }
        binding.btnViewAll.setOnClickListener {
            findNavController().navigate(R.id.navigation_history)
        }
        // Listener buat tombol "View More" di chart
        binding.btnViewMoreAnalytics.setOnClickListener {
            findNavController().navigate(R.id.navigation_analytics)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (!isLoading) {
                binding.swipeRefreshLayout.isRefreshing = false
            }
            // Jangan tampilkan progress bar utama kalo loading chart
            // Biar progress bar chart-nya aja yang muter
            // binding.progressBar.isVisible = isLoading
        }

        // Observer buat Summary Cards (Ini udah bener)
        viewModel.todayKwh.observe(viewLifecycleOwner) { value ->
            binding.tvSummaryTodayValue.text = String.format(Locale.US, "%.2f", value)
        }

        viewModel.weeklyKwh.observe(viewLifecycleOwner) { value ->
            binding.tvSummaryWeekValue.text = String.format(Locale.US, "%.2f", value)
        }

        viewModel.monthlyCost.observe(viewLifecycleOwner) { value ->
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            currencyFormat.maximumFractionDigits = 0
            binding.tvSummaryCostValue.text = currencyFormat.format(value).replace("Rp", "").trim()
        }

        // Observer buat Recent History (Ini udah bener)
        viewModel.recentHistory.observe(viewLifecycleOwner) { history ->
            binding.layoutRecentActivity.isVisible = history.isNotEmpty()
            binding.tvNoActivity.isVisible = history.isEmpty()
            recentHistoryAdapter.submitList(history)
        }

        // ==========================================================
        // ===            TAMBAHIN BLOK INI (PENTING)             ===
        // ==========================================================
        viewModel.weeklyChartData.observe(viewLifecycleOwner) { state ->
            // Atur visibility progress bar dan chart
            binding.progressBarChartWeekly.isVisible = state is UIState.Loading
            binding.chartWeekly.isVisible = state is UIState.Success
            binding.tvNoChartData.isVisible = false // Sembunyiin dulu

            when (state) {
                is UIState.Success -> {
                    // Cek datanya 0 semua atau kosong
                    Log.d("CEK_CHART", "Data dari Server: ${state.data}")
                    state.data.forEach {
                        Log.d("CEK_CHART", "Hari: ${it.label}, Nilai: ${it.value}")
                    }
                    val allZero = state.data.all { it.value == 0.0 }
                    if (state.data.isEmpty() || allZero) {
                        // Kalo kosong, sembunyiin chart, tampilin teks "No Data"
                        binding.chartWeekly.isVisible = false
                        binding.tvNoChartData.isVisible = true
                    } else {
                        // Kalo ada data, sembunyiin teks "No Data", tampilin chart
                        binding.chartWeekly.isVisible = true
                        binding.tvNoChartData.isVisible = false
                        setupWeeklyChart(state.data) // Panggil fungsi buat nampilin data
                    }
                }
                is UIState.Error -> {
                    // Kalo error, sembunyiin chart, tampilin teks "No Data"
                    binding.chartWeekly.isVisible = false
                    binding.tvNoChartData.isVisible = true
                    Log.e("HomeFragment", "Failed to load chart: ${state.message}")
                    // Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
                is UIState.Loading -> {
                    // Biarin progress bar muter
                }
            }
        }
    }

    // ==========================================================
    // ===       TAMBAHIN FUNGSI INI (Nyontek dari Analytics)   ===
    // ==========================================================
    private fun setupWeeklyChart(data: List<ChartDataPoint>) {
        try {
            // 1. Siapkan data dalam format yang diminta WilliamChart (Label -> Nilai)
            val chartData = linkedMapOf<String, Float>()
            data.forEach { dataPoint ->
                // Ubah dataPoint.value (Double) jadi Float
                chartData[dataPoint.label] = dataPoint.value.toFloat()
            }

            // 2. Terapkan data dan animasi
            binding.chartWeekly.animation.duration = 1000L // Animasi 1 detik
            binding.chartWeekly.labelsFormatter = { value ->
                String.format(Locale.US, "%.1f", value) // Format jadi 1 angka di belakang koma
            }
            binding.chartWeekly.animate(chartData)

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up weekly chart", e)
            binding.chartWeekly.isVisible = false
            binding.tvNoChartData.isVisible = true
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

