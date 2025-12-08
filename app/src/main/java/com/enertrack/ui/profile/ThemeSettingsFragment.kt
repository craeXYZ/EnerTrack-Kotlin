package com.enertrack.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.enertrack.R
import com.enertrack.databinding.FragmentThemeSettingsBinding

class ThemeSettingsFragment : Fragment() {

    private var _binding: FragmentThemeSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThemeSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCurrentThemeSelection()
        setupListeners()
    }

    private fun setupCurrentThemeSelection() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> binding.radioLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> binding.radioDark.isChecked = true
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> binding.radioSystem.isChecked = true
            else -> binding.radioSystem.isChecked = true
        }
    }

    private fun setupListeners() {
        // Listener Tombol Back Custom
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Listener Radio Group
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_light -> applyTheme(AppCompatDelegate.MODE_NIGHT_NO)
                R.id.radio_dark -> applyTheme(AppCompatDelegate.MODE_NIGHT_YES)
                R.id.radio_system -> applyTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun applyTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("theme_mode", mode)
            apply()
        }
    }

    // === MANAJEMEN ACTION BAR ===
    // Sembunyikan Header Bawaan Android, Tampilkan Header Custom Kita
    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}