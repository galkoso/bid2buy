package com.example.bid2buy.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.ListingRepository
import com.example.bid2buy.util.CurrencyManager
import com.example.bid2buy.util.TimeUtils
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ListingRepository(database.listingDao())
    private val currencyManager = CurrencyManager.getInstance(application)
    
    private val _listings = MutableLiveData(emptyList<Listing>())
    val listings: LiveData<List<Listing>> = _listings

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var isPaginating = false

    private var lastFetchedListings: List<Listing> = emptyList()
    private var timerJob: Job? = null
    private var observationJob: Job? = null
    
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var canLoadMore = true
    
    private var displayedItemsLimit = 20
    private val pageSize = 20

    private var currentCategory: String? = null
    private var currentCondition: String? = null
    private var currentPriceRange: String? = null
    private var currentSearchQuery: String? = null

    init {
        fetchExchangeRates()
        startListening()
        refresh()
    }

    private fun fetchExchangeRates() {
        viewModelScope.launch {
            currencyManager.fetchRatesIfNeeded()
            processAndPostListings()
        }
    }

    fun startListening() {
        if (observationJob != null) return

        observationJob = viewModelScope.launch {
            repository.observeActiveListings(TimeUtils.currentTimeMillis()).collectLatest { listings ->
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
                processAndPostListings()
            }
        }
    }

    fun refresh() {
        displayedItemsLimit = 20
        viewModelScope.launch {
            _isLoading.value = true
            fetchExchangeRates()
            lastVisibleDocument = repository.refreshActiveListings(limit = pageSize.toLong())
            canLoadMore = lastVisibleDocument != null
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (isPaginating) return

        val filteredCount = getFilteredList().size
        if (displayedItemsLimit < filteredCount) {
            displayedItemsLimit += pageSize
            processAndPostListings()
            if (displayedItemsLimit <= filteredCount) return 
        }

        if (!canLoadMore || currentSearchQuery != null || currentCategory != null || currentCondition != null || currentPriceRange != null) return

        viewModelScope.launch {
            isPaginating = true
            val lastDoc = repository.loadMoreActiveListings(lastVisibleDocument, limit = pageSize.toLong())
            if (lastDoc != null) {
                lastVisibleDocument = lastDoc
            } else {
                canLoadMore = false
            }
            isPaginating = false
        }
    }

    fun setFilters(category: String?, condition: String?, priceRange: String?) {
        currentCategory = if (category == "All Categories" || category.isNullOrBlank()) null else category
        currentCondition = if (condition == "All Conditions" || condition.isNullOrBlank()) null else condition
        currentPriceRange = if (priceRange == "All Prices" || priceRange.isNullOrBlank()) null else priceRange
        displayedItemsLimit = 20
        processAndPostListings()
    }

    fun setSearchQuery(query: String?) {
        currentSearchQuery = if (query.isNullOrBlank()) null else query
        displayedItemsLimit = 20
        processAndPostListings()
    }

    fun clearFilters() {
        currentCategory = null
        currentCondition = null
        currentPriceRange = null
        displayedItemsLimit = 20
        processAndPostListings()
    }

    private fun getFilteredList(): List<Listing> {
        val now = TimeUtils.currentTimeMillis()
        
        var filteredList = lastFetchedListings.filter { 
            (it.closingAt?.toDate()?.time ?: 0) > now
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
                val basePrice = if (listing.bidCount > 0) (listing.currentHighestBid ?: listing.startingPrice) else listing.startingPrice
                
                when (range) {
                    "Under ₪100" -> basePrice < 100
                    "₪100 - ₪500" -> basePrice in 100.0..500.0
                    "Over ₪500" -> basePrice > 500
                    else -> true
                }
            }
        }

        return filteredList.sortedBy { it.closingAt }
    }

    private fun processAndPostListings() {
        val sortedList = getFilteredList()
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
