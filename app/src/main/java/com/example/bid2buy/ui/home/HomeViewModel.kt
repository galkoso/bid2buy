package com.example.bid2buy.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.ListingsRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = ListingsRepository()
    private val _listings = MutableLiveData<List<Listing>>()
    val listings: LiveData<List<Listing>> = _listings

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _timerPulse = MutableLiveData<Long>()
    val timerPulse: LiveData<Long> = _timerPulse

    private var lastFetchedListings: List<Listing> = emptyList()
    private var timerJob: Job? = null

    fun startListening() {
        _isLoading.value = true
        repository.getFirestoreInstance()
            .collection("listings")
            .whereEqualTo("status", "ACTIVE")
            .get()
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    lastFetchedListings = task.result?.toObjects(Listing::class.java) ?: emptyList()
                    processAndPostListings()
                }
            }
        
        startTimer()
    }

    private fun startTimer() {
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(60000) // Pulse every minute
                _timerPulse.postValue(System.currentTimeMillis())
                processAndPostListings()
            }
        }
    }

    fun refresh() {
        _isLoading.value = true
        repository.getFirestoreInstance()
            .collection("listings")
            .whereEqualTo("status", "ACTIVE")
            .get(Source.SERVER)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    lastFetchedListings = task.result?.toObjects(Listing::class.java) ?: emptyList()
                }
                _timerPulse.postValue(System.currentTimeMillis())
                processAndPostListings()
            }
    }

    private fun processAndPostListings() {
        val now = Timestamp.now()
        val activeListings = lastFetchedListings.filter { 
            it.closingAt != null && it.closingAt.toDate().time > now.toDate().time 
        }.sortedBy { it.closingAt }
        
        _listings.postValue(activeListings)
    }

    fun stopListening() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
