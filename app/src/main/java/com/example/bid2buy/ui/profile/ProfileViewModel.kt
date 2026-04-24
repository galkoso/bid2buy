package com.example.bid2buy.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.model.UserProfile
import com.example.bid2buy.repositories.BidsRepository
import com.example.bid2buy.repositories.FirestoreUserRepository
import com.example.bid2buy.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val firestoreUserRepository = FirestoreUserRepository()
    private val repository = UserRepository(firestoreUserRepository, database.userDao())
    private val bidsRepository = BidsRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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
        val uid = auth.currentUser?.uid
        if (uid != null) {
            observeProfile(uid)
            refreshProfile(uid)
            loadActiveListingsCount(uid)
            loadActiveBidsCount(uid)
            loadWinsCount(uid)
        } else {
            _errorMessage.value = "User not authenticated"
        }
    }

    private fun observeProfile(uid: String) {
        viewModelScope.launch {
            repository.observeUserProfile(uid).collectLatest { profile ->
                if (profile != null) {
                    _userProfile.value = profile
                    _successRate.value = profile.successRate
                }
            }
        }
    }

    private fun refreshProfile(uid: String) {
        viewModelScope.launch {
            repository.refreshUserProfile(uid)
        }
    }

    private fun loadActiveListingsCount(uid: String) {
        viewModelScope.launch {
            try {
                firestoreUserRepository.observeActiveListingsCount(uid).collectLatest { count ->
                    _activeListingsCount.value = count
                }
            } catch (e: Exception) {
                _activeListingsCount.value = _userProfile.value?.activeListingsCount ?: 0
            }
        }
    }

    private fun loadActiveBidsCount(uid: String) {
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

    private fun loadWinsCount(uid: String) {
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
