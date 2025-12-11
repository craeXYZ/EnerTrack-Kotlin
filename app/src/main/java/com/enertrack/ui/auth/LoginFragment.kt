package com.enertrack.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log // Tambahan buat ngecek Logcat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.enertrack.MainActivity
import com.enertrack.R
import com.enertrack.data.local.SessionManager
import com.enertrack.databinding.FragmentLoginBinding
import com.enertrack.util.UIState
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    // Mengambil ViewModel dari Factory
    private val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, pass)
        }

        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.btnLogin.isEnabled = false
                }
                is UIState.Success -> {
                    binding.progressBar.isVisible = false

                    val userData = state.data
                    val inputEmail = binding.etEmail.text.toString().trim() // AMBIL DARI INPUT LANGSUNG

                    Log.e("CEK_LOGIN", "========================================")
                    Log.e("CEK_LOGIN", "Username: ${userData.username}")
                    Log.e("CEK_LOGIN", "Email dari Server: ${userData.email}") // Ini NULL (Biarkan saja)
                    Log.e("CEK_LOGIN", "Email dari Input: $inputEmail")        // Ini yang BENAR
                    Log.e("CEK_LOGIN", "========================================")

                    Toast.makeText(requireContext(), "Welcome, ${userData.username ?: "User"}!", Toast.LENGTH_SHORT).show()

                    // === SIMPAN SEMUA DATA PENTING ===
                    val sessionManager = SessionManager(requireContext())

                    lifecycleScope.launch {
                        // 1. Simpan Token (Pasti ada)

                        // 2. Simpan Email (PAKAI DATA DARI INPUT USER)
                        // Karena login sukses, berarti email yang diketik user itu VALID.
                        // Kita pakai fallback: Kalau server kasih null, ambil dari input.
                        val emailToSave = if (!userData.email.isNullOrEmpty()) userData.email else inputEmail

                        sessionManager.saveEmail(emailToSave)
                        sessionManager.saveUserId(userData.uid) // Pastikan ID juga kesimpen
                        sessionManager.saveUsername(userData.username ?: "User")

                        Log.e("CEK_LOGIN", "✅ BERHASIL SIMPAN: $emailToSave")

                        // 3. Pindah Halaman
                        val intent = Intent(activity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
                is UIState.Error -> {
                    binding.progressBar.isVisible = false
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}