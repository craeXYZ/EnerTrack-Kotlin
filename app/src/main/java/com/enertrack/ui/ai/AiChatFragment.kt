package com.enertrack.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.enertrack.R
import com.enertrack.data.model.ChatMessage
import com.enertrack.data.model.DeviceOption
import com.enertrack.data.repository.HistoryRepository
import com.enertrack.databinding.FragmentAiChatBinding

class AiChatFragment : Fragment() {

    private var _binding: FragmentAiChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AiChatViewModel by viewModels {
        AiChatViewModelFactory(HistoryRepository.getInstance(requireContext()))
    }

    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var currentSelectedContext: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupChipListeners()
        observeViewModel()

        if (chatList.isEmpty()) {
            addAiMessage("Halo! Saya siap bantu. Pilih perangkat di atas kalau mau tanya spesifik ya.")
        }
    }

    private fun observeViewModel() {
        viewModel.deviceOptions.observe(viewLifecycleOwner) { options ->
            setupSearchableDropdown(options)
        }

        viewModel.aiResponse.observe(viewLifecycleOwner) { responseText ->
            addAiMessage(responseText)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // === SETUP SEARCHABLE DROPDOWN (AUTOCOMPLETE) ===
    private fun setupSearchableDropdown(options: List<DeviceOption>) {
        // 1. FILTER: Hilangkan opsi "Umum" dari list dropdown
        //    Jadi user cuma milih perangkat asli aja di list
        val filteredOptions = options.filter { !it.label.contains("Umum", ignoreCase = true) }

        val labels = filteredOptions.map { it.label }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, labels)
        binding.dropdownDeviceSelect.setAdapter(adapter)

        // 2. STATE AWAL: Kosongkan teks
        //    Ini bikin "Hint" (Pilih Perangkat) dari XML yang muncul sebagai label yang rapi.
        binding.dropdownDeviceSelect.setText("", false)
        currentSelectedContext = "" // Context default (kosong = umum)

        binding.dropdownDeviceSelect.setOnItemClickListener { parent, _, position, _ ->
            val selectedLabel = parent.getItemAtPosition(position) as String

            // Cari data aslinya di list yang SUDAH DIFILTER
            val selectedOption = filteredOptions.find { it.label == selectedLabel }

            if (selectedOption != null) {
                currentSelectedContext = selectedOption.context
            }
        }

        binding.dropdownDeviceSelect.setOnClickListener {
            binding.dropdownDeviceSelect.showDropDown()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(chatList)
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBackChat.setOnClickListener { findNavController().popBackStack() }

        binding.btnSendChat.setOnClickListener {
            val messageText = binding.etChatInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun setupChipListeners() {
        binding.chipBoros.setOnClickListener { sendMessage("Apakah penggunaan perangkat ini tergolong boros?") }
        binding.chipTips.setOnClickListener { sendMessage("Berikan tips hemat untuk perangkat ini") }
        binding.chipCost.setOnClickListener { sendMessage("Berapa estimasi biaya per bulan?") }
    }

    private fun sendMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, isUser = true))
        scrollToBottom()
        binding.etChatInput.text.clear()
        viewModel.sendMessage(text, currentSelectedContext)
    }

    private fun addAiMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, isUser = false))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (chatList.isNotEmpty()) {
            binding.rvChatMessages.smoothScrollToPosition(chatList.size - 1)
        }
    }

    // === MANAJEMEN TAMPILAN ===
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        activity?.findViewById<View>(R.id.bottom_nav_view)?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        if (findNavController().currentDestination?.id != R.id.navigation_ai_insight) {
            (activity as? AppCompatActivity)?.supportActionBar?.show()
        }
        activity?.findViewById<View>(R.id.bottom_nav_view)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}