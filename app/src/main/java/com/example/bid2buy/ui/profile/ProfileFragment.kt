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
import com.example.bid2buy.AuthRepository
import com.example.bid2buy.WelcomeActivity
import com.example.bid2buy.databinding.FragmentProfileBinding
import com.example.bid2buy.model.UserProfile
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

        setupObservers()
        setupListeners()
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
                        binding.successRate.text = "$rate%"
                    }
                }

                launch {
                    viewModel.errorMessage.collectLatest { message ->
                        message?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                            // Also hide shimmer if there's an error so the user isn't stuck
                            hideShimmer()
                        }
                    }
                }
            }
        }
    }

    private fun hideShimmer() {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.profileContent.visibility = View.VISIBLE
    }

    private fun updateUI(profile: UserProfile) {
        binding.userName.text = profile.displayName.ifEmpty { "Anonymous User" }
        binding.userEmail.text = profile.email
        binding.userInitials.text = getInitials(profile.displayName)
        
        binding.activeListingsCount.text = profile.activeListingsCount.toString()
        binding.activeBidsCount.text = profile.activeBidsCount.toString()
        binding.winsCount.text = profile.winsCount.toString()
        binding.totalItemsSold.text = profile.totalItemsSold.toString()

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
            authRepository.logout()
            val intent = Intent(requireContext(), WelcomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }

        binding.editProfileBtn.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
