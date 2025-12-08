package com.enertrack.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment // Import yang dibutuhkan
import com.enertrack.R
import com.enertrack.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cara yang lebih aman untuk mendapatkan NavController di Activity
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.auth_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Cek apakah ada tujuan spesifik dari WelcomeActivity
        val destination = intent.getStringExtra("DESTINATION")
        if (destination == "REGISTER") {
            // Arahkan ke RegisterFragment
            navController.navigate(R.id.registerFragment)
        }
    }
}
