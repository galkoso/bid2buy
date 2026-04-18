package com.example.bid2buy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.UserProfile
import com.example.bid2buy.repositories.BidsRepository
import com.example.bid2buy.repositories.FirestoreUserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: FirestoreUserRepository = FirestoreUserRepository(),
    private val bidsRepository: BidsRepository = BidsRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successRate = MutableStateFlow(0)
    val successRate: StateFlow<Int> = _successRate.asStateFlow()

    private val _activeListingsCount = MutableStateFlow(0)
    val activeListingsCount: StateFlow<Int> = _activeListingsCount.asStateFlow()

    private val _activeBidsCount = MutableStateFlow(0)
    val activeBidsCount: StateFlow<Int> = _activeBidsCount.asStateFlow()

    private val _winsCount = MutableStateFlow(0)
    val winsCount: StateFlow<Int> = _winsCount.asStateFlow()

    init {
        loadUserProfile()
        loadActiveListingsCount()
        loadActiveBidsCount()
        loadWinsCount()
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _errorMessage.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            try {
                repository.observeUser(uid).collectLatest { profile ->
                    _userProfile.value = profile
                    _successRate.value = profile?.successRate ?: 0
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "An error occurred"
            }
        }
    }

    private fun loadActiveListingsCount() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                repository.observeActiveListingsCount(uid).collectLatest { count ->
                    _activeListingsCount.value = count
                }
            } catch (e: Exception) {
                _activeListingsCount.value = _userProfile.value?.activeListingsCount ?: 0
            }
        }
    }

    private fun loadActiveBidsCount() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                bidsRepository.observeActiveBidsCount(uid).collectLatest { count ->
                    _activeBidsCount.value = count
                }
            } catch (e: Exception) {
                _activeBidsCount.value = _userProfile.value?.activeBidsCount ?: 0
            }
        }
    }

    private fun loadWinsCount() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                bidsRepository.observeWinsCount(uid).collectLatest { count ->
                    _winsCount.value = count
                }
            } catch (e: Exception) {
                _winsCount.value = _userProfile.value?.winsCount ?: 0
            }
        }
    }

    fun updateDisplayName(newDisplayName: String) {
        val uid = auth.currentUser?.uid ?: return
        val currentProfile = _userProfile.value
        viewModelScope.launch {
            try {
                repository.updateUserProfile(
                    uid = uid,
                    displayName = newDisplayName,
                    phoneNumber = currentProfile?.phoneNumber ?: "",
                    location = currentProfile?.location ?: "",
                    bio = currentProfile?.bio ?: ""
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
