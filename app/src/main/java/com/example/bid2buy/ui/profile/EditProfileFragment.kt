package com.example.bid2buy.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bid2buy.R
import com.example.bid2buy.databinding.FragmentEditProfileBinding
import com.example.bid2buy.model.UserProfile
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    // This launcher allows choosing from gallery, drive, and other file providers
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                Glide.with(this).load(selectedImageUri).circleCrop().into(binding.profileImage)
                binding.userInitials.visibility = View.GONE
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.changePhotoBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_photo)))
        }

        binding.btnSaveChanges.setOnClickListener {
            viewModel.saveProfile(
                displayName = binding.etDisplayName.text.toString(),
                location = binding.etLocation.text.toString(),
                imageUri = selectedImageUri
            )
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userProfile.collectLatest { profile ->
                        profile?.let { populateFields(it) }
                    }
                }

                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        if (isLoading) {
                            binding.btnSaveChanges.setText(R.string.saving)
                            binding.btnSaveChanges.isEnabled = false
                            binding.loadingOverlay.visibility = View.VISIBLE
                        } else {
                            binding.btnSaveChanges.setText(R.string.save_changes)
                            binding.btnSaveChanges.isEnabled = true
                            binding.loadingOverlay.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.saveSuccess.collectLatest { success ->
                        if (success) {
                            Toast.makeText(requireContext(), R.string.profile_updated_success, Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collectLatest { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun populateFields(profile: UserProfile) {
        binding.etDisplayName.setText(profile.displayName)
        binding.etEmail.setText(profile.email)
        binding.etLocation.setText(profile.location)

        if (profile.photoURL.isNotEmpty()) {
            Glide.with(this).load(profile.photoURL).circleCrop().into(binding.profileImage)
            binding.userInitials.visibility = View.GONE
        } else {
            binding.userInitials.text = getInitials(profile.displayName)
            binding.userInitials.visibility = View.VISIBLE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
