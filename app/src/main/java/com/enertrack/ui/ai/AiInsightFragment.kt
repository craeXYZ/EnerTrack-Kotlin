package com.enertrack.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.enertrack.R
import com.enertrack.databinding.FragmentAiInsightBinding

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
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Placeholder setup UI
    }

    private fun setupListeners() {
        binding.cardChatAi.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_aiInsight_to_chat)
            } catch (e: Exception) {
                Toast.makeText(context, "Navigasi Error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // --- LOGIKA HIDE/SHOW ACTION BAR ---
    override fun onResume() {
        super.onResume()
        // Halaman ini gak butuh Action Bar, jadi hide
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        // Logic Pinter:
        // Cek dulu, kita mau pindah ke mana?
        // Kalau tujuannya BUKAN ke Chat, baru kita munculin lagi Action Barnya.
        // (Karena Chat juga mau nge-hide Action Bar, biar gak kedip/muncul)
        if (findNavController().currentDestination?.id != R.id.navigation_ai_chat) {
            (activity as? AppCompatActivity)?.supportActionBar?.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}