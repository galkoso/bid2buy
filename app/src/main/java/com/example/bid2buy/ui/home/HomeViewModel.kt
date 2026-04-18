package com.example.bid2buy.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.ListingsRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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
    private var listenerRegistration: ListenerRegistration? = null

    private var currentCategory: String? = null
    private var currentCondition: String? = null
    private var currentPriceRange: String? = null
    private var currentSearchQuery: String? = null

    fun startListening() {
        if (listenerRegistration != null) return

        _isLoading.value = true
        listenerRegistration = repository.getFirestoreInstance()
            .collection("listings")
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // map to objects and trigger process
                    lastFetchedListings = snapshot.toObjects(Listing::class.java)
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
        // Trigger a fresh process
        processAndPostListings()
    }

    fun setFilters(category: String?, condition: String?, priceRange: String?) {
        currentCategory = if (category == "All Categories" || category.isNullOrBlank()) null else category
        currentCondition = if (condition == "All Conditions" || condition.isNullOrBlank()) null else condition
        currentPriceRange = if (priceRange == "All Prices" || priceRange.isNullOrBlank()) null else priceRange
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

        // 1. Filter out expired ones
        var filteredList = lastFetchedListings.filter { 
            it.closingAt != null && it.closingAt.toDate().time > now.toDate().time 
        }

        // 2. Search
        currentSearchQuery?.let { query ->
            filteredList = filteredList.filter {
                it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }
        }

        // 3. Category
        currentCategory?.let { cat ->
            filteredList = filteredList.filter { it.category == cat }
        }

        // 4. Condition
        currentCondition?.let { cond ->
            filteredList = filteredList.filter { it.condition == cond }
        }

        // 5. Price Range
        currentPriceRange?.let { range ->
            filteredList = filteredList.filter { listing ->
                val priceToShow = if (listing.bidCount > 0) listing.currentHighestBid ?: listing.startingPrice else listing.startingPrice
                when (range) {
                    "Under ₪100" -> priceToShow < 100
                    "₪100 - ₪500" -> priceToShow in 100.0..500.0
                    "Over ₪500" -> priceToShow > 500
                    else -> true
                }
            }
        }

        // 6. Sort
        val sortedList = filteredList.sortedBy { it.closingAt }
        
        // Force update by sending a NEW list instance
        _listings.postValue(ArrayList(sortedList))
    }

    fun stopListening() {
        listenerRegistration?.remove()
        listenerRegistration = null
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
