package com.example.bid2buy.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bid2buy.AuthRepository
import com.example.bid2buy.R
import com.example.bid2buy.WelcomeActivity
import com.example.bid2buy.databinding.FragmentProfileBinding
import com.example.bid2buy.model.UserProfile
import com.example.bid2buy.util.NetworkUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authRepository = AuthRepository()
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isGuest = FirebaseAuth.getInstance().currentUser == null
        if (isGuest) {
            setupGuestUI()
        } else {
            setupObservers()
            setupListeners()
        }
    }

    private fun setupGuestUI() {
        hideShimmer()
        binding.userName.text = getString(R.string.guest_user)
        binding.userEmail.text = getString(R.string.guest_email_subtitle)
        binding.userInitials.text = getString(R.string.guest_initials)
        binding.userInitials.visibility = View.VISIBLE
        binding.profileImage.visibility = View.GONE

        val grayAlpha = 0.5f
        
        binding.editProfileBtn.isEnabled = false
        binding.editProfileBtn.alpha = grayAlpha
        
        binding.activeListingsCard.isEnabled = false
        binding.activeListingsCard.alpha = grayAlpha
        
        binding.activeBidsCard.isEnabled = false
        binding.activeBidsCard.alpha = grayAlpha
        
        binding.winsCard.isEnabled = false
        binding.winsCard.alpha = grayAlpha
        
        binding.myListingsSection.isEnabled = false
        binding.myListingsSection.alpha = grayAlpha
        
        binding.myBidsSection.isEnabled = false
        binding.myBidsSection.alpha = grayAlpha

        binding.currencySection.isEnabled = false
        binding.currencySection.alpha = grayAlpha

        binding.memberSince.text = getString(R.string.time_left_na)
        binding.totalItemsSold.text = getString(R.string.zero)
        binding.successRate.text = getString(R.string.zero_percent)
        
        binding.logoutSection.setOnClickListener {
            handleLogout()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userProfile.collectLatest { profile ->
                        if (profile != null) {
                            updateUI(profile)
                            hideShimmer()
                        }
                    }
                }

                launch {
                    viewModel.successRate.collectLatest { rate ->
                        binding.successRate.text = getString(R.string.percent_format, rate)
                    }
                }

                launch {
                    viewModel.activeListingsCount.collectLatest { count ->
                        binding.activeListingsCount.text = count.toString()
                    }
                }

                launch {
                    viewModel.activeBidsCount.collectLatest { count ->
                        binding.activeBidsCount.text = count.toString()
                    }
                }

                launch {
                    viewModel.winsCount.collectLatest { count ->
                        binding.winsCount.text = count.toString()
                    }
                }

                launch {
                    viewModel.totalItemsSold.collectLatest { count ->
                        binding.totalItemsSold.text = count.toString()
                    }
                }

                launch {
                    viewModel.selectedCurrency.collectLatest { code ->
                        updateCurrencyUI(code)
                    }
                }

                launch {
                    viewModel.errorMessage.collectLatest { message ->
                        message?.let {
                            if (FirebaseAuth.getInstance().currentUser != null) {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                            viewModel.clearError()
                            hideShimmer()
                        }
                    }
                }
            }
        }
    }

    private fun updateCurrencyUI(code: String) {
        val currencyText = when (code) {
            "ILS" -> "ILS (₪)"
            "USD" -> "USD ($)"
            "EUR" -> "EUR (€)"
            "GBP" -> "GBP (£)"
            "JPY" -> "JPY (¥)"
            "AUD" -> "AUD (A$)"
            "CAD" -> "CAD (C$)"
            "CHF" -> "CHF (CHF)"
            else -> "ILS (₪)"
        }
        binding.currentCurrencyText.text = currencyText
    }

    private fun hideShimmer() {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.profileContent.visibility = View.VISIBLE
    }

    private fun updateUI(profile: UserProfile) {
        binding.userName.text = profile.displayName.ifEmpty { getString(R.string.anonymous_user) }
        binding.userEmail.text = profile.email
        
        if (profile.photoURL.isNotEmpty()) {
            Glide.with(this)
                .load(profile.photoURL)
                .circleCrop()
                .into(binding.profileImage)
            binding.userInitials.visibility = View.GONE
            binding.profileImage.visibility = View.VISIBLE
        } else {
            binding.userInitials.text = getInitials(profile.displayName)
            binding.userInitials.visibility = View.VISIBLE
            binding.profileImage.visibility = View.GONE
        }

        profile.createdAt?.let {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            binding.memberSince.text = sdf.format(it.toDate())
        }
    }

    private fun getInitials(displayName: String): String {
        if (displayName.isEmpty()) return "??"
        val parts = displayName.split(" ")
        return if (parts.size >= 2) {
            "${parts[0][0]}${parts[1][0]}".uppercase()
        } else {
            parts[0].take(2).uppercase()
        }
    }

    private fun setupListeners() {
        binding.logoutSection.setOnClickListener {
            handleLogout()
        }

        binding.editProfileBtn.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(requireContext(), getString(R.string.no_internet_profile_edit_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(ProfileFragmentDirections.actionNavigationProfileToEditProfileFragment())
        }

        binding.activeListingsCard.setOnClickListener {
            navigateToMyListings()
        }

        binding.activeBidsCard.setOnClickListener {
            navigateToBids(0)
        }

        binding.winsCard.setOnClickListener {
            navigateToBids(1)
        }

        binding.myListingsSection.setOnClickListener {
            navigateToMyListings()
        }

        binding.myBidsSection.setOnClickListener {
            navigateToBids(0)
        }

        binding.currencySection.setOnClickListener {
            CurrencySelectorBottomSheetFragment().show(childFragmentManager, "CurrencySelector")
        }
    }

    private fun handleLogout() {
        authRepository.logout()
        val intent = Intent(requireContext(), WelcomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToMyListings() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNav.selectedItemId = R.id.navigation_listings
    }

    private fun navigateToBids(initialTab: Int) {
        val action = ProfileFragmentDirections.actionNavigationProfileToNavigationBids(initialTab)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
