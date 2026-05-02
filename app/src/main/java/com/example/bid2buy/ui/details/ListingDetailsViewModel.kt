package com.example.bid2buy.ui.details

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
import java.util.concurrent.TimeUnit

class ListingDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ListingRepository(database.listingDao())

    private val _listing = MutableLiveData<Listing?>()
    val listing: LiveData<Listing?> = _listing

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isOwner = MutableLiveData<Boolean>(false)
    val isOwner: LiveData<Boolean> = _isOwner

    private val _isSignedIn = MutableLiveData<Boolean>(false)
    val isSignedIn: LiveData<Boolean> = _isSignedIn

    private val _canEdit = MutableLiveData<Boolean>(false)
    val canEdit: LiveData<Boolean> = _canEdit

    private val _canDelete = MutableLiveData<Boolean>(false)
    val canDelete: LiveData<Boolean> = _canDelete

    private val _canBid = MutableLiveData<Boolean>(false)
    val canBid: LiveData<Boolean> = _canBid

    private val _isClosed = MutableLiveData<Boolean>(false)
    val isClosed: LiveData<Boolean> = _isClosed

    private val _timeRemaining = MutableLiveData<String>()
    val timeRemaining: LiveData<String> = _timeRemaining

    private var timerJob: Job? = null
    private var observationJob: Job? = null

    fun loadListing(listingId: String) {
        _isLoading.value = true
        
        // 1. Refresh from network (Rule: Local and Remote storage)
        viewModelScope.launch {
            repository.refreshListing(listingId)
            _isLoading.value = false
        }

        // 2. Observe from Room (Single Source of Truth)
        observationJob?.cancel()
        observationJob = viewModelScope.launch {
            repository.observeListing(listingId).collectLatest { listing ->
                _listing.value = listing
                if (listing != null) {
                    updatePermissions(listing)
                    startTimer(listing)
                }
            }
        }
    }

    private fun startTimer(listing: Listing) {
        timerJob?.cancel()
        val closingAt = listing.closingAt ?: return
        
        timerJob = viewModelScope.launch {
            while (true) {
                val now = TimeUtils.now()
                val diff = closingAt.toDate().time - now.toDate().time
                
                if (diff <= 0) {
                    _timeRemaining.value = "Closed"
                    updatePermissions(listing)
                    break
                }
                
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                
                _timeRemaining.value = String.format("%02dh %02dm %02ds", hours, minutes, seconds)
                
                delay(1000)
            }
        }
    }

    private fun updatePermissions(listing: Listing) {
        val currentUserUid = repository.getCurrentUserUid()
        val isGuest = currentUserUid == null
        _isSignedIn.value = !isGuest
        val owner = !isGuest && currentUserUid == listing.createdByUid
        _isOwner.value = owner

        val now = TimeUtils.now()
        val isExpired = listing.closingAt?.let { it.toDate().time <= now.toDate().time } ?: false
        val closed = listing.status == "CLOSED" || isExpired
        _isClosed.value = closed

        _canEdit.value = owner && !closed

        _canDelete.value = owner && listing.bidCount == 0

        _canBid.value = !isGuest && !owner && !closed
    }

    fun deleteListing() {
        val listingId = _listing.value?.id ?: return
        viewModelScope.launch {
            try {
                repository.deleteListing(listingId)
            } catch (e: Exception) {
                _error.value = "Failed to delete: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        observationJob?.cancel()
    }
}
