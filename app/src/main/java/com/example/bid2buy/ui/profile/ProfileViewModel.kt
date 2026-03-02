package com.example.bid2buy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.UserProfile
import com.example.bid2buy.repositories.FirestoreUserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: FirestoreUserRepository = FirestoreUserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successRate = MutableStateFlow(0)
    val successRate: StateFlow<Int> = _successRate.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _errorMessage.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            try {
                // Add a 2-second delay to simulate loading and see the shimmer
                delay(2000)

                repository.observeUser(uid).collectLatest { profile ->
                    _userProfile.value = profile
                    _successRate.value = profile?.successRate ?: 0
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "An error occurred"
            }
        }
    }

    fun updateDisplayName(newDisplayName: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                repository.updateUserProfile(uid, newDisplayName)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
