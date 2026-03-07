package com.example.bid2buy.ui.myListings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.ListingsRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyListingsViewModel : ViewModel() {

    private val repository = ListingsRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _listings = MutableLiveData<List<Listing>>()
    val listings: LiveData<List<Listing>> = _listings

    private val _activeCount = MutableLiveData<Int>(0)
    val activeCount: LiveData<Int> = _activeCount

    private val _closedCount = MutableLiveData<Int>(0)
    val closedCount: LiveData<Int> = _closedCount

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Used to trigger UI refreshes for the timer without data changes
    private val _timerPulse = MutableLiveData<Long>()
    val timerPulse: LiveData<Long> = _timerPulse

    private var refreshJob: Job? = null
    private var listingsListener: ListenerRegistration? = null

    fun startListening() {
        val uid = auth.currentUser?.uid ?: return
        
        stopListening()
        _isLoading.value = true
        
        listingsListener = repository.getFirestoreInstance()
            .collection("listings")
            .whereEqualTo("createdByUid", uid)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null) return@addSnapshotListener
                
                val allListings = snapshot?.toObjects(Listing::class.java) ?: emptyList()
                updateCountsAndListings(allListings)
            }
    }

    fun stopListening() {
        listingsListener?.remove()
        listingsListener = null
    }

    private fun updateCountsAndListings(allListings: List<Listing>) {
        val now = Timestamp.now()
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
                _timerPulse.postValue(System.currentTimeMillis())
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
