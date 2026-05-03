package com.example.bid2buy

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.databinding.ActivityMainBinding
import com.example.bid2buy.repositories.ListingRepository
import com.example.bid2buy.util.NetworkUtils
import com.example.bid2buy.util.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            TimeUtils.syncTime()
            
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = ListingRepository(database.listingDao())
            repository.cleanupOldListings()
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)

        val isGuest = FirebaseAuth.getInstance().currentUser == null

        if (isGuest) {
            binding.fabAdd.alpha = 0.5f
            binding.fabAdd.setOnClickListener(null)
            binding.fabAdd.isClickable = false

            val menu = binding.navView.menu
            menu.findItem(R.id.navigation_listings).isEnabled = false
            menu.findItem(R.id.navigation_bids).isEnabled = false
        } else {
            binding.fabAdd.setOnClickListener {
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    Toast.makeText(this, "No internet connection. Please connect to the internet to add a new listing.", Toast.LENGTH_SHORT).show()
                } else {
                    navController.navigate(R.id.navigation_add)
                }
            }
        }

        binding.navView.setOnItemSelectedListener { item ->
            if (item.isEnabled) {
                if (navController.currentDestination?.id != item.itemId) {
                    navController.navigate(item.itemId)
                }
                true
            } else {
                false
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
