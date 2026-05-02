package com.example.bid2buy.ui.myListings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.ListingRepository
import com.example.bid2buy.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyListingsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ListingRepository(database.listingDao())

    private val _listings = MutableLiveData<List<Listing>>()
    val listings: LiveData<List<Listing>> = _listings

    private val _activeCount = MutableLiveData<Int>(0)
    val activeCount: LiveData<Int> = _activeCount

    private val _closedCount = MutableLiveData<Int>(0)
    val closedCount: LiveData<Int> = _closedCount

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _timerPulse = MutableLiveData<Long>()
    val timerPulse: LiveData<Long> = _timerPulse

    private var refreshJob: Job? = null
    private var observationJob: Job? = null

    fun startListening() {
        val uid = repository.getCurrentUserUid() ?: return
        
        stopListening()
        _isLoading.value = true

        // 1. Refresh from network (Rule: Local and Remote storage)
        viewModelScope.launch {
            repository.refreshUserListings(uid)
            _isLoading.value = false
        }

        // 2. Observe from Room (Single Source of Truth)
        observationJob = viewModelScope.launch {
            repository.observeUserListings(uid).collectLatest { allListings ->
                updateCountsAndListings(allListings)
            }
        }
    }

    fun stopListening() {
        observationJob?.cancel()
        observationJob = null
    }

    private fun updateCountsAndListings(allListings: List<Listing>) {
        val now = TimeUtils.now()
        val active = allListings.filter { it.closingAt != null && it.closingAt.toDate().time > now.toDate().time }
        val closed = allListings.filter { it.closingAt == null || it.closingAt.toDate().time <= now.toDate().time }
        
        _activeCount.postValue(active.size)
        _closedCount.postValue(closed.size)
        _listings.postValue(allListings)
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(60000) // Refresh timer every minute
                _timerPulse.postValue(TimeUtils.currentTimeMillis())
                _listings.value?.let { currentList ->
                    updateCountsAndListings(currentList)
                }
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        stopAutoRefresh()
    }
}
