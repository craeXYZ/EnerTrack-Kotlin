package com.enertrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible // 1. Tambah import ini
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout // 2. Tambah import ini
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.Menu

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    // 3. Pindahkan 2 variabel ini ke atas (jadi properti kelas)
    private lateinit var bottomNavView: BottomNavigationView
    private lateinit var appBarLayout: AppBarLayout // 4. Tambah variabel ini

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // 5. Isi variabel yang tadi dipindah
        bottomNavView = findViewById(R.id.bottom_nav_view)
        appBarLayout = findViewById(R.id.app_bar_layout) // 6. Isi variabel AppBarLayout

        setSupportActionBar(toolbar)

        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_analytics, R.id.navigation_calculate, R.id.navigation_history
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNavView.setupWithNavController(navController)

        // --- 7. INI SOLUSI UTAMANYA ---
        // Tambahkan listener untuk memantau perubahan destinasi
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_profile -> {
                    // Jika tujuannya adalah Profile, sembunyikan keduanya
                    appBarLayout.isVisible = false
                    bottomNavView.isVisible = false
                }
                else -> {
                    // Jika tujuannya fragment lain, tampilkan keduanya
                    appBarLayout.isVisible = true
                    bottomNavView.isVisible = true
                }
            }
        }
        // --- Akhir dari solusi ---

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    navController.navigate(R.id.navigation_profile)
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
}
