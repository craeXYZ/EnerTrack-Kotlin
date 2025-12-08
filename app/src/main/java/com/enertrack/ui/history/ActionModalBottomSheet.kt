package com.enertrack.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// 1. Ganti nama import binding agar cocok dengan nama file XML Anda
import com.enertrack.databinding.BottomSheetActionModalBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ActionModalBottomSheet(private val onDeleteClick: () -> Unit) : BottomSheetDialogFragment() {

    // 2. Ganti tipe data _binding dan binding
    private var _binding: BottomSheetActionModalBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 3. Gunakan kelas Binding yang benar untuk inflate
        _binding = BottomSheetActionModalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Kode ini tetap sama karena ID 'btn_delete' di XML Anda sudah benar
        binding.btnDelete.setOnClickListener {
            onDeleteClick()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ActionModalBottomSheet"
    }
}
