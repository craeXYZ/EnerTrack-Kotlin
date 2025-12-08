package com.enertrack.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.enertrack.R
import com.enertrack.databinding.FragmentProfileBinding
import com.enertrack.ui.auth.WelcomeActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
        setupBackPressHandler() // Handle tombol back fisik HP
        observeViewModel()
    }

    private fun setupUI() {
        // Pastikan layout edit tersembunyi di awal
        toggleEditMode(false)
    }

    private fun setupClickListeners() {
        // --- VIEW MODE LISTENERS ---

        // Tombol Back di Header Biru
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Tombol Logout
        binding.btnLogout.setOnClickListener {
            viewModel.logoutUser()
        }

        // Menu: Edit Profile & Password
        binding.menuEditProfile.setOnClickListener {
            val currentUser = viewModel.userData.value
            binding.etEditUsername.setText(currentUser?.username)
            // Password fields dikosongkan saat dibuka
            binding.etNewPassword.text?.clear()
            binding.etConfirmPassword.text?.clear()

            toggleEditMode(true) // Buka halaman edit
        }

        // Menu: Tampilan
        binding.menuTheme.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_themeSettings)
        }

        // --- EDIT MODE LISTENERS ---

        // Tombol Close di Header Putih
        binding.btnCloseEdit.setOnClickListener {
            toggleEditMode(false) // Tutup halaman edit
        }

        // Tombol Save Changes
        binding.btnSaveChanges.setOnClickListener {
            val newUsername = binding.etEditUsername.text.toString().trim()
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // Validasi Username
            if (newUsername.isBlank()) {
                binding.etEditUsername.error = "Username cannot be empty"
                return@setOnClickListener
            }

            // Validasi Password (hanya jika diisi)
            if (newPassword.isNotEmpty()) {
                if (newPassword.length < 6) {
                    binding.etNewPassword.error = "Minimum 6 characters"
                    return@setOnClickListener
                }
                if (newPassword != confirmPassword) {
                    binding.etConfirmPassword.error = "Password does not match"
                    return@setOnClickListener
                }

                // TODO: Logic update password di ViewModel
                Toast.makeText(context, "Password update logic pending", Toast.LENGTH_SHORT).show()
            }

            // Simpan Username
            viewModel.updateUsername(newUsername)
        }
    }

    private fun toggleEditMode(showEdit: Boolean) {
        if (showEdit) {
            binding.groupViewMode.visibility = View.GONE
            binding.layoutEditMode.visibility = View.VISIBLE
        } else {
            binding.groupViewMode.visibility = View.VISIBLE
            binding.layoutEditMode.visibility = View.GONE

            // Bersihkan error
            binding.etEditUsername.error = null
            binding.etNewPassword.error = null
            binding.etConfirmPassword.error = null
        }
    }

    // Handle back button biar nggak langsung keluar aplikasi pas lagi ngedit
    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.layoutEditMode.visibility == View.VISIBLE) {
                    toggleEditMode(false) // Tutup edit mode
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed() // Perilaku back normal
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvUsername.text = it.username ?: "User"
                binding.tvEmail.text = it.email ?: "email@example.com"
            }
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess == true) {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                toggleEditMode(false) // Tutup edit mode kalau sukses
                viewModel.resetUpdateStatus()
            } else if (isSuccess == false) {
                Toast.makeText(requireContext(), "Failed to update profile.", Toast.LENGTH_SHORT).show()
                viewModel.resetUpdateStatus()
            }
        }

        viewModel.logoutStatus.observe(viewLifecycleOwner) { isLoggedOut ->
            if (isLoggedOut == true) {
                val intent = Intent(activity, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity?.finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}