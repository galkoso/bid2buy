package com.example.bid2buy.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.ListingsRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ListingDetailsViewModel : ViewModel() {

    private val repository = ListingsRepository()

    private val _listing = MutableLiveData<Listing?>()
    val listing: LiveData<Listing?> = _listing

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isOwner = MutableLiveData<Boolean>(false)
    val isOwner: LiveData<Boolean> = _isOwner

    private val _canEdit = MutableLiveData<Boolean>(false)
    val canEdit: LiveData<Boolean> = _canEdit

    private val _canDelete = MutableLiveData<Boolean>(false)
    val canDelete: LiveData<Boolean> = _canDelete

    private val _canBid = MutableLiveData<Boolean>(false)
    val canBid: LiveData<Boolean> = _canBid

    private val _isClosed = MutableLiveData<Boolean>(false)
    val isClosed: LiveData<Boolean> = _isClosed

    fun loadListing(listingId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.getListing(listingId).collectLatest { listing ->
                    _listing.value = listing
                    _isLoading.value = false
                    if (listing != null) {
                        updatePermissions(listing)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun updatePermissions(listing: Listing) {
        val currentUserUid = repository.getCurrentUserUid()
        val owner = currentUserUid == listing.createdByUid
        _isOwner.value = owner

        val now = Timestamp.now()
        val closed = listing.closingAt?.let { it.seconds < now.seconds } ?: false
        _isClosed.value = closed

        _canEdit.value = owner && !closed

        _canDelete.value = owner && listing.bidCount == 0

        _canBid.value = !owner && !closed
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
}
