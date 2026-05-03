package com.example.bid2buy.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.model.UserProfile
import com.example.bid2buy.repositories.FirestoreUserRepository
import com.example.bid2buy.repositories.UserRepository
import com.example.bid2buy.repositories.BidsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val firestoreUserRepository = FirestoreUserRepository()
    private val repository = UserRepository(firestoreUserRepository, database.userDao())
    private val bidsRepository = BidsRepository(database.bidDao(), database.listingDao())
    private val uid = bidsRepository.getCurrentUserUid() ?: ""

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
                repository.observeUserProfile(uid).take(1).collect { profile ->
                    _userProfile.value = profile
                }
            } catch (e: Exception) {
                _errorMessage.emit(e.message ?: "Failed to load profile")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveProfile(
        displayName: String,
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
                    photoURL = firestoreUserRepository.uploadProfileImage(uid, imageUri)
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
