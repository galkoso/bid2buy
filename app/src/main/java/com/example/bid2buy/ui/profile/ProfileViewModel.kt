package com.example.bid2buy.ui.profile

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.model.UserProfile
import com.example.bid2buy.repositories.BidsRepository
import com.example.bid2buy.repositories.FirestoreUserRepository
import com.example.bid2buy.repositories.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val firestoreUserRepository = FirestoreUserRepository()
    private val repository = UserRepository(firestoreUserRepository, database.userDao())
    private val bidsRepository = BidsRepository(database.bidDao(), database.listingDao())
    
    private val sharedPrefs = application.getSharedPreferences("bid2buy_prefs", Context.MODE_PRIVATE)

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

    private val _totalItemsSold = MutableStateFlow(0)
    val totalItemsSold: StateFlow<Int> = _totalItemsSold.asStateFlow()

    private val _totalBidsCount = MutableStateFlow(0)

    private val _selectedCurrency = MutableStateFlow(sharedPrefs.getString("selected_currency", "ILS") ?: "ILS")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    init {
        val uid = bidsRepository.getCurrentUserUid()
        if (uid != null) {
            observeProfile(uid)
            refreshProfile(uid)
            loadActiveListingsCount(uid)
            loadActiveBidsCount(uid)
            loadWinsCount(uid)
            loadTotalItemsSold(uid)
            loadTotalBidsCount(uid)
            setupSuccessRateCalculation()
            
            viewModelScope.launch {
                bidsRepository.refreshUserBids(uid)
            }
        } else {
            _errorMessage.value = "User not authenticated"
        }
    }

    private fun observeProfile(uid: String) {
        viewModelScope.launch {
            repository.observeUserProfile(uid).collectLatest { profile ->
                if (profile != null) {
                    _userProfile.value = profile
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
            } catch (_: Exception) {
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
            } catch (_: Exception) {
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
            } catch (_: Exception) {
                _winsCount.value = 0
            }
        }
    }

    private fun loadTotalItemsSold(uid: String) {
        viewModelScope.launch {
            try {
                firestoreUserRepository.observeTotalItemsSold(uid).collectLatest { count ->
                    _totalItemsSold.value = count
                }
            } catch (_: Exception) {
                _totalItemsSold.value = 0
            }
        }
    }

    private fun loadTotalBidsCount(uid: String) {
        viewModelScope.launch {
            try {
                bidsRepository.observeTotalBidsCount(uid).collectLatest { count ->
                    _totalBidsCount.value = count
                }
            } catch (_: Exception) {
                _totalBidsCount.value = 0
            }
        }
    }

    private fun setupSuccessRateCalculation() {
        viewModelScope.launch {
            combine(_winsCount, _totalBidsCount) { wins, total ->
                if (total > 0) ((wins.toDouble() / total) * 100).toInt() else 0
            }.collectLatest { rate ->
                _successRate.value = rate
            }
        }
    }

    fun selectCurrency(currencyCode: String) {
        sharedPrefs.edit { putString("selected_currency", currencyCode) }
        _selectedCurrency.value = currencyCode
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
