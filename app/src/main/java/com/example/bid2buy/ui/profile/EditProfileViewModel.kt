package com.example.bid2buy.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.UserProfile
import com.example.bid2buy.repositories.FirestoreUserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditProfileViewModel : ViewModel() {
    private val repository = FirestoreUserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val uid = auth.currentUser?.uid ?: ""

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Boolean>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        if (uid.isEmpty()) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val profile = repository.refreshUser(uid)
                _userProfile.value = profile
            } catch (e: Exception) {
                _errorMessage.emit(e.message ?: "Failed to load profile")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveProfile(
        displayName: String,
        email: String,
        location: String,
        imageUri: Uri? = null
    ) {
        if (uid.isEmpty()) return

        if (displayName.isBlank()) {
            viewModelScope.launch { _errorMessage.emit("Display name cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                var photoURL: String? = null
                if (imageUri != null) {
                    photoURL = repository.uploadProfileImage(uid, imageUri)
                }

                repository.updateUserProfile(
                    uid = uid,
                    displayName = displayName,
                    phoneNumber = _userProfile.value?.phoneNumber ?: "",
                    location = location,
                    bio = _userProfile.value?.bio ?: "",
                    photoURL = photoURL
                )
                
                _saveSuccess.emit(true)
            } catch (e: Exception) {
                _errorMessage.emit(e.message ?: "Failed to update profile")
            } finally {
                _isLoading.value = false
            }
        }
    }
}