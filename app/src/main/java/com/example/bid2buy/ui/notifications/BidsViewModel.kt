package com.example.bid2buy.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.BidsRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BidsViewModel : ViewModel() {

    private val repository = BidsRepository()

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

    init {
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _timerPulse.postValue(System.currentTimeMillis())
            }
        }
    }

    fun loadBids() {
        val uid = repository.getCurrentUserUid() ?: return
        
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val userBids = repository.getBidsForUser(uid)
                val listingIds = userBids.map { it.listingId }.distinct()
                val listings = repository.getListingsByIds(listingIds)

                val active = mutableListOf<BidItemUiModel>()
                val won = mutableListOf<BidItemUiModel>()
                val lost = mutableListOf<BidItemUiModel>()

                val now = Timestamp.now()

                listings.forEach { listing ->
                    val userMaxBid = userBids.filter { it.listingId == listing.id }.maxByOrNull { it.amount }?.amount ?: 0.0
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

                _activeBids.value = active
                _wonBids.value = won
                _lostBids.value = lost
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
