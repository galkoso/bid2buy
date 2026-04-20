package com.example.bid2buy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.bid2buy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)

        // Prevent re-navigating to Bids (and resetting the tab) if already there
        binding.navView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.navigation_bids && navController.currentDestination?.id == R.id.navigation_bids) {
                // Do nothing to keep current tab state
                return@setOnItemReselectedListener
            }
            
            // For other items, you can either do nothing or implement scroll to top
            // Default behavior for reselection in NavigationUI is often to pop to the root of the tab
        }

        binding.fabAdd.setOnClickListener {
            navController.navigate(R.id.navigation_add)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home, R.id.navigation_listings, 
                R.id.navigation_bids, R.id.navigation_profile -> {
                    binding.bottomAppBar.performShow()
                    binding.fabAdd.show()
                }
                else -> {
                    binding.bottomAppBar.performHide()
                    binding.fabAdd.hide()
                }
            }
        }
    }
}
