package com.enertrack.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager // <-- Pastikan import ini ada
import com.enertrack.data.model.HistoryItem
import com.enertrack.databinding.FragmentHistoryBinding
import com.google.android.material.chip.Chip

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory(requireContext())
    }
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupPagination()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { item ->
            val detailSheet = HistoryDetailBottomSheet.newInstance(item).apply {
                onDeleteClick = { itemToDelete ->
                    showDeleteConfirmationDialog(itemToDelete)
                }
            }
            detailSheet.show(parentFragmentManager, HistoryDetailBottomSheet.TAG)
        }

        // ================== PERBAIKAN UTAMA DI SINI ==================
        binding.recyclerViewHistory.apply {
            // Pasang "rak"-nya (LayoutManager) di sini
            layoutManager = LinearLayoutManager(requireContext())
            // Pasang "forklift"-nya (Adapter)
            adapter = historyAdapter
        }
        // =============================================================
    }

    private fun showDeleteConfirmationDialog(item: HistoryItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Record")
            .setMessage("Are you sure you want to delete the record for '${item.appliance}'?")
            .setPositiveButton("Delete") { dialog, _ ->
                viewModel.deleteItem(item)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupFilters() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.searchQuery.value = text.toString()
        }
        val categories = listOf("All", "Entertainment", "Cooling", "Health", "Lighting", "Kitchen", "Heating")
        binding.chipGroupCategories.isSingleSelection = true
        categories.forEach { categoryName ->
            val chip = Chip(context).apply {
                text = categoryName
                isCheckable = true
                isChecked = categoryName == "All"
            }
            binding.chipGroupCategories.addView(chip)
        }
        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds.first())
                viewModel.selectedCategory.value = chip.text.toString()
            } else {
                viewModel.selectedCategory.value = "All"
            }
        }
    }

    private fun setupPagination() {
        binding.btnNext.setOnClickListener { viewModel.goToNextPage() }
        binding.btnPrevious.setOnClickListener { viewModel.goToPreviousPage() }
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.onRefresh()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.onToastShown()
            }
        }

        viewModel.paginatedHistoryList.observe(viewLifecycleOwner) { list ->
            historyAdapter.submitList(list)
            binding.tvEmptyData.isVisible = list.isEmpty() && viewModel.isLoading.value == false
        }

        viewModel.currentPage.observe(viewLifecycleOwner) { updatePaginationUi() }
        viewModel.totalPages.observe(viewLifecycleOwner) { updatePaginationUi() }
    }

    private fun updatePaginationUi() {
        val currentPage = viewModel.currentPage.value ?: 1
        val totalPages = viewModel.totalPages.value ?: 1

        binding.layoutPagination.isVisible = totalPages > 1
        binding.tvPageInfo.text = "Page $currentPage of $totalPages"
        binding.btnPrevious.isEnabled = currentPage > 1
        binding.btnNext.isEnabled = currentPage < totalPages
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}