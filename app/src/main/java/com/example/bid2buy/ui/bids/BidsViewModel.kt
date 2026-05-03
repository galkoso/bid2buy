package com.example.bid2buy.ui.bids

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.BidsRepository
import com.example.bid2buy.model.Bid
import com.example.bid2buy.repositories.ListingRepository
import com.example.bid2buy.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BidsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val bidsRepository = BidsRepository(database.bidDao(), database.listingDao())
    private val listingsRepository = ListingRepository(database.listingDao())

    private val _activeBids = MutableLiveData<List<BidItemUiModel>>()
    val activeBids: LiveData<List<BidItemUiModel>> = _activeBids

    private val _wonBids = MutableLiveData<List<BidItemUiModel>>()
    val wonBids: LiveData<List<BidItemUiModel>> = _wonBids

    private val _lostBids = MutableLiveData<List<BidItemUiModel>>()
    val lostBids: LiveData<List<BidItemUiModel>> = _lostBids

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _timerPulse = MutableLiveData<Long>()
    val timerPulse: LiveData<Long> = _timerPulse

    private var timerJob: Job? = null
    private var observationJob: Job? = null
    
    private var lastFetchedListings: List<Listing> = emptyList()
    private var lastFetchedUserBids: List<Bid> = emptyList()

    init {
        startTimer()
        observeData()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timerPulse.postValue(TimeUtils.currentTimeMillis())
                if (lastFetchedListings.isNotEmpty()) {
                    processAndPostBids()
                }
            }
        }
    }

    private fun observeData() {
        val uid = bidsRepository.getCurrentUserUid() ?: return
        
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            combine(
                bidsRepository.observeUserBids(uid),
                listingsRepository.observeActiveListings(0)
            ) { bids, listings ->
                Pair(bids, listings)
            }.collectLatest { (bids, listings) ->
                lastFetchedUserBids = bids
                lastFetchedListings = listings.filter { listing -> bids.any { it.listingId == listing.id } }
                processAndPostBids()
            }
        }
    }

    fun loadBids(forceRefresh: Boolean = false) {
        val uid = bidsRepository.getCurrentUserUid() ?: return
        
        if (forceRefresh || lastFetchedUserBids.isEmpty()) {
            _isLoading.value = true
        }

        viewModelScope.launch {
            try {
                bidsRepository.refreshUserBids(uid)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun processAndPostBids() {
        val uid = bidsRepository.getCurrentUserUid() ?: return
        
        val active = mutableListOf<BidItemUiModel>()
        val won = mutableListOf<BidItemUiModel>()
        val lost = mutableListOf<BidItemUiModel>()

        val now = TimeUtils.now()

        lastFetchedListings.forEach { listing ->
            val userMaxBid = lastFetchedUserBids.filter { it.listingId == listing.id }.maxByOrNull { it.amount }?.amount ?: 0.0
            val isExpired = listing.closingAt?.let { it.toDate().time <= now.toDate().time } ?: false
            val isClosed = listing.status == "CLOSED" || isExpired

            if (!isClosed) {
                val status = if (listing.highestBidderUid == uid) BidStatus.ACTIVE_WINNING else BidStatus.ACTIVE_OUTBID
                active.add(BidItemUiModel(listing, userMaxBid, status))
            } else {
                if (listing.highestBidderUid == uid) {
                    won.add(BidItemUiModel(listing, userMaxBid, BidStatus.WON))
                } else {
                    lost.add(BidItemUiModel(listing, userMaxBid, BidStatus.LOST))
                }
            }
        }

        _activeBids.postValue(active)
        _wonBids.postValue(won)
        _lostBids.postValue(lost)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        observationJob?.cancel()
    }
}
