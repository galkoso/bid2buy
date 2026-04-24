package com.example.bid2buy.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.ListingRepository
import com.example.bid2buy.util.TimeUtils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val repository = ListingRepository(firestore, database.listingDao())
    
    private val _listings = MutableLiveData<List<Listing>>()
    val listings: LiveData<List<Listing>> = _listings

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isPaginating = MutableLiveData<Boolean>(false)
    val isPaginating: LiveData<Boolean> = _isPaginating

    private val _timerPulse = MutableLiveData<Long>()
    val timerPulse: LiveData<Long> = _timerPulse

    private var lastFetchedListings: List<Listing> = emptyList()
    private var timerJob: Job? = null
    private var observationJob: Job? = null
    
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var canLoadMore = true
    
    // Pagination settings for local display
    private var displayedItemsLimit = 20
    private val PAGE_SIZE = 20

    private var currentCategory: String? = null
    private var currentCondition: String? = null
    private var currentPriceRange: String? = null
    private var currentSearchQuery: String? = null

    init {
        startListening()
        refresh()
    }

    fun startListening() {
        if (observationJob != null) return

        observationJob = viewModelScope.launch {
            repository.observeActiveListings().collectLatest { listings ->
                lastFetchedListings = listings
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
                _timerPulse.postValue(TimeUtils.currentTimeMillis())
                processAndPostListings()
            }
        }
    }

    fun refresh() {
        displayedItemsLimit = PAGE_SIZE // Reset display limit on refresh
        viewModelScope.launch {
            _isLoading.value = true
            lastVisibleDocument = repository.refreshActiveListings()
            canLoadMore = lastVisibleDocument != null
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (_isPaginating.value == true) return

        // 1. Logic for gradual display of ALREADY cached items (works offline)
        val filteredCount = getFilteredList().size
        if (displayedItemsLimit < filteredCount) {
            displayedItemsLimit += PAGE_SIZE
            processAndPostListings()
            // If we still have more cached items to show, we don't necessarily need to hit the network yet
            if (displayedItemsLimit <= filteredCount) return 
        }

        // 2. Logic for fetching NEW items from network (works online)
        if (!canLoadMore || currentSearchQuery != null || currentCategory != null || currentCondition != null || currentPriceRange != null) return

        viewModelScope.launch {
            _isPaginating.value = true
            val lastDoc = repository.loadMoreActiveListings(lastVisibleDocument)
            if (lastDoc != null) {
                lastVisibleDocument = lastDoc
            } else {
                canLoadMore = false
            }
            _isPaginating.value = false
        }
    }

    fun setFilters(category: String?, condition: String?, priceRange: String?) {
        currentCategory = if (category == "All Categories" || category.isNullOrBlank()) null else category
        currentCondition = if (condition == "All Conditions" || condition.isNullOrBlank()) null else condition
        currentPriceRange = if (priceRange == "All Prices" || priceRange.isNullOrBlank()) null else priceRange
        displayedItemsLimit = PAGE_SIZE // Reset limit when filters change
        processAndPostListings()
    }

    fun setSearchQuery(query: String?) {
        currentSearchQuery = if (query.isNullOrBlank()) null else query
        displayedItemsLimit = PAGE_SIZE // Reset limit when search changes
        processAndPostListings()
    }

    fun clearFilters() {
        currentCategory = null
        currentCondition = null
        currentPriceRange = null
        displayedItemsLimit = PAGE_SIZE
        processAndPostListings()
    }

    private fun getFilteredList(): List<Listing> {
        val now = TimeUtils.now()

        var filteredList = lastFetchedListings.filter { 
            it.closingAt != null && it.closingAt.toDate().time > now.toDate().time
        }

        currentSearchQuery?.let { query ->
            filteredList = filteredList.filter {
                it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
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
                val priceToShow = if (listing.bidCount > 0) listing.currentHighestBid ?: listing.startingPrice else listing.startingPrice
                when (range) {
                    "Under ₪100" -> priceToShow < 100
                    "₪100 - ₪500" -> priceToShow in 100.0..500.0
                    "Over ₪500" -> priceToShow > 500
                    else -> true
                }
            }
        }

        return filteredList.sortedBy { it.closingAt }
    }

    private fun processAndPostListings() {
        val sortedList = getFilteredList()
        
        // Apply the display limit for gradual loading (Online & Offline)
        val limitedList = sortedList.take(displayedItemsLimit)
        
        if (_listings.value != limitedList) {
            _listings.postValue(limitedList)
        }
    }

    fun stopListening() {
        observationJob?.cancel()
        observationJob = null
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
