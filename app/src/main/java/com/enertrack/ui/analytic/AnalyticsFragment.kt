package com.enertrack.ui.analytic

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.enertrack.data.model.ChartDataPoint
import com.enertrack.databinding.FragmentAnalyticsBinding
import com.enertrack.util.UIState
import java.util.Locale

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyticsViewModel by viewModels {
        AnalyticsViewModelFactory(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Swipe Refresh
        binding.swipeRefreshAnalytics.setOnRefreshListener {
            viewModel.fetchAllStatistics()
        }

        observeViewModel()
        viewModel.fetchAllStatistics()
    }

    private fun observeViewModel() {
        // === 1. WEEKLY CHART ===
        viewModel.weeklyStats.observe(viewLifecycleOwner) { state ->
            // Matikan loading refresh kalau data udah masuk
            if (state !is UIState.Loading) {
                binding.swipeRefreshAnalytics.isRefreshing = false
            }

            binding.progressBarWeekly.isVisible = state is UIState.Loading

            if (state is UIState.Success) {
                val data = state.data
                val hasRealData = data.isNotEmpty() && data.any { it.value > 0.0 }

                if (hasRealData) {
                    binding.chartWeekly.isVisible = true
                    binding.tvEmptyWeekly.isVisible = false
                    setupWeeklyChart(data)
                } else {
                    binding.chartWeekly.isVisible = false
                    binding.tvEmptyWeekly.isVisible = true
                }
            } else if (state is UIState.Error) {
                binding.chartWeekly.isVisible = false
                binding.tvEmptyWeekly.text = "Error: ${state.message}"
                binding.tvEmptyWeekly.isVisible = true
            }
        }

        // === 2. MONTHLY CHART ===
        viewModel.monthlyStats.observe(viewLifecycleOwner) { state ->
            binding.progressBarMonthly.isVisible = state is UIState.Loading

            if (state is UIState.Success) {
                val data = state.data
                val hasRealData = data.isNotEmpty() && data.any { it.value > 0.0 }

                if (hasRealData) {
                    binding.chartMonthly.isVisible = true
                    binding.tvEmptyMonthly.isVisible = false
                    setupMonthlyChart(data)
                } else {
                    binding.chartMonthly.isVisible = false
                    binding.tvEmptyMonthly.isVisible = true
                }
            } else if (state is UIState.Error) {
                binding.chartMonthly.isVisible = false
                binding.tvEmptyMonthly.isVisible = true
            }
        }
    }

    private fun setupWeeklyChart(data: List<ChartDataPoint>) {
        try {
            val chartData = linkedMapOf<String, Float>()
            data.forEach { dataPoint ->
                chartData[dataPoint.label] = dataPoint.value.toFloat()
            }

            binding.chartWeekly.animation.duration = 1000L
            binding.chartWeekly.labelsFormatter = { value -> String.format(Locale.US, "%.1f", value) }
            binding.chartWeekly.animate(chartData)
        } catch (e: Exception) {
            Log.e("Analytics", "Error setting weekly chart", e)
        }
    }

    private fun setupMonthlyChart(data: List<ChartDataPoint>) {
        try {
            val limitedData = data.take(4)
            val chartData = linkedMapOf<String, Float>()
            limitedData.forEach { dataPoint ->
                chartData[dataPoint.label] = dataPoint.value.toFloat()
            }

            binding.chartMonthly.animation.duration = 1000L
            binding.chartMonthly.labelsFormatter = { value -> String.format(Locale.US, "%.1f", value) }
            binding.chartMonthly.animate(chartData)
        } catch (e: Exception) {
            Log.e("Analytics", "Error setting monthly chart", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}