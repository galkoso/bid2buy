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

    private var currentCategory: String? = null
    private var currentCondition: String? = null
    private var currentPriceRange: String? = null
    private var currentSearchQuery: String? = null

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
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
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

    fun setFilters(category: String?, condition: String?, priceRange: String?) {
        currentCategory = if (category == "All Categories") null else category
        currentCondition = if (condition == "All Conditions") null else condition
        currentPriceRange = if (priceRange == "All Prices") null else priceRange
        processAndPostListings()
    }

    fun setSearchQuery(query: String?) {
        currentSearchQuery = if (query.isNullOrBlank()) null else query
        processAndPostListings()
    }

    fun clearFilters() {
        currentCategory = null
        currentCondition = null
        currentPriceRange = null
        processAndPostListings()
    }

    private fun processAndPostListings() {
        val now = Timestamp.now()

        var filteredList = lastFetchedListings.filter { 
            it.closingAt != null && it.closingAt.toDate().time > now.toDate().time 
        }

        currentSearchQuery?.let { query ->
            filteredList = filteredList.filter { 
                it.title?.contains(query, ignoreCase = true) == true ||
                it.description?.contains(query, ignoreCase = true) == true
            }
        }

        currentCategory?.let { cat ->
            filteredList = filteredList.filter { it.category == cat }
        }

        currentCondition?.let { cond ->
            filteredList = filteredList.filter { it.condition == cond }
        }

        currentPriceRange?.let { range ->
            filteredList = filteredList.filter { listing ->
                when (range) {
                    "Under ₪100" -> listing.startingPrice < 100
                    "₪100 - ₪500" -> listing.startingPrice in 100.0..500.0
                    "Over ₪500" -> listing.startingPrice > 500
                    else -> true
                }
            }
        }

        val sortedList = filteredList.sortedBy { it.closingAt }
        _listings.postValue(sortedList)
    }

    fun stopListening() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    fun getCurrentFilters(): Triple<String?, String?, String?> {
        return Triple(currentCategory, currentCondition, currentPriceRange)
    }
}
