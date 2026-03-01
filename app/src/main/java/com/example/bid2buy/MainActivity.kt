package com.example.bid2buy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.bid2buy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)

        binding.fabAdd.setOnClickListener {
            navController.navigate(R.id.navigation_add)
        }

        binding.logoutButton.setOnClickListener {
            authRepository.logout()
            val intent = Intent(this, WelcomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home, R.id.navigation_listings, 
                R.id.navigation_notifications, R.id.navigation_profile -> {
                    binding.bottomAppBar.performShow()
                    binding.fabAdd.show()
                    binding.logoutButton.visibility = android.view.View.VISIBLE
                }
                else -> {
                    binding.bottomAppBar.performHide()
                    binding.fabAdd.hide()
                    binding.logoutButton.visibility = android.view.View.GONE
                }
            }
        }
    }
}