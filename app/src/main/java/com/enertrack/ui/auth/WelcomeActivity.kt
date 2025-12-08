package com.enertrack.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.enertrack.R
import com.enertrack.databinding.ActivityWelcomeBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private var isAutoSliding = true // Penanda apakah auto slide boleh jalan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Kita rapikan setup-nya ke fungsi terpisah biar enak dibaca
        setupViewPager()
        setupListeners()
    }

    private fun setupViewPager() {
        val slides = listOf(
            OnboardingSlide(R.drawable.welcome_calc_icon, "Calculate", "Easily calculate your device energy usage and cost."),
            OnboardingSlide(R.drawable.welcome_history, "History", "View your past energy usage and device history."),
            OnboardingSlide(R.drawable.welcome_analytic, "Analytics", "Visualize your energy consumption with analytics.")
        )

        val adapter = OnboardingAdapter(slides)
        binding.viewPager.adapter = adapter

        // Hubungkan ViewPager2 dengan TabLayout (untuk indikator titik)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        // === FITUR AUTO SLIDE (YANG SEBELUMNYA KURANG) ===
        startAutoSlide(slides.size)

        // Logika tambahan: Matikan auto slide kalau user lagi geser manual
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                // Kalau user lagi drag (menyeret), stop auto slide
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isAutoSliding = false
                }
                // Kalau sudah selesai drag (idle), bisa dinyalakan lagi (opsional, di sini kita biarin false biar user baca)
                // Atau biarkan logic startAutoSlide yang handle timing-nya
            }
        })
    }

    private fun setupListeners() {
        binding.btnGetStarted.setOnClickListener {
            // Pindah ke AuthActivity yang berisi RegisterFragment
            startActivity(Intent(this, AuthActivity::class.java).apply {
                // Kirim info untuk langsung ke halaman register
                putExtra("DESTINATION", "REGISTER")
            })
            finish() // Tutup halaman welcome biar gak balik lagi kalau di-back
        }
    }

    // === FUNGSI UTAMA AUTO SLIDE ===
    private fun startAutoSlide(itemCount: Int) {
        lifecycleScope.launch {
            while (true) { // Loop selamanya (selama activity hidup)
                delay(3000) // Tunggu 3 detik

                // Cek apakah boleh geser (isAutoSliding true dan Activity masih aktif)
                if (isAutoSliding && !isFinishing) {
                    val currentItem = binding.viewPager.currentItem
                    // Hitung halaman berikutnya. Kalau udah mentok kanan, balik ke 0 (kiri)
                    val nextItem = if (currentItem < itemCount - 1) currentItem + 1 else 0

                    binding.viewPager.setCurrentItem(nextItem, true) // true = pakai animasi geser
                }

                // Reset isAutoSliding jadi true lagi setelah delay,
                // jaga-jaga kalau tadi dimatiin pas user nge-drag
                isAutoSliding = true
            }
        }
    }
}