package com.example.bid2buy

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.bid2buy.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.example.bid2buy.util.TimeUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sync time with server to prevent local clock manipulation
        lifecycleScope.launch {
            TimeUtils.syncTime()
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)

        val isGuest = FirebaseAuth.getInstance().currentUser == null

        if (isGuest) {
            // Faded out and disable FAB
            binding.fabAdd.alpha = 0.5f
            binding.fabAdd.setOnClickListener(null)
            binding.fabAdd.isClickable = false

            // Disable restricted menu items (they usually look faded when disabled)
            val menu = binding.navView.menu
            menu.findItem(R.id.navigation_listings).isEnabled = false
            menu.findItem(R.id.navigation_bids).isEnabled = false
            
            // Set alpha on the internal views of the navigation for a more pronounced faded effect
            // Note: BottomNavigationView doesn't have a simple setAlpha for items, 
            // but isEnabled=false usually does the trick with standard color selectors.
        } else {
            binding.fabAdd.setOnClickListener {
                navController.navigate(R.id.navigation_add)
            }
        }

        binding.navView.setOnItemSelectedListener { item ->
            // If it's enabled, navigate. If disabled, isEnabled=false handles it.
            if (item.isEnabled) {
                if (navController.currentDestination?.id != item.itemId) {
                    navController.navigate(item.itemId)
                }
                true
            } else {
                false
            }
        }

        binding.navView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.navigation_bids && navController.currentDestination?.id == R.id.navigation_bids) {
                return@setOnItemReselectedListener
            }
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
