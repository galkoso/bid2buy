package com.example.bid2buy.ui.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.data.local.AppDatabase
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.FirestoreUserRepository
import com.example.bid2buy.repositories.ListingRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

sealed class CreateListingState {
    object Idle : CreateListingState()
    object Loading : CreateListingState()
    data class Success(val listingId: String) : CreateListingState()
    data class Error(val message: String) : CreateListingState()
}

class CreateListingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val listingsRepository = ListingRepository(database.listingDao())
    private val userRepository = FirestoreUserRepository()

    private val _uiState = MutableStateFlow<CreateListingState>(CreateListingState.Idle)
    val uiState: StateFlow<CreateListingState> = _uiState

    fun publishListing(
        title: String,
        description: String,
        category: String,
        condition: String,
        location: String,
        startingPrice: Double,
        currency: String,
        closingDate: Date,
        imageUris: List<Uri>
    ) {
        val currentUserUid = listingsRepository.getCurrentUserUid()
        if (currentUserUid == null) {
            _uiState.value = CreateListingState.Error("User not authenticated")
            return
        }

        if (imageUris.isEmpty()) {
            _uiState.value = CreateListingState.Error("Please select at least one image")
            return
        }

        val urisToUpload = ArrayList(imageUris)

        viewModelScope.launch {
            _uiState.value = CreateListingState.Loading
            try {
                val listingId = withContext(Dispatchers.IO) {
                    val userProfile = userRepository.refreshUser(currentUserUid)
                    
                    val listing = Listing(
                        id = "",
                        title = title,
                        description = description,
                        category = category,
                        condition = condition,
                        location = location,
                        startingPrice = startingPrice,
                        currency = currency,
                        closingAt = Timestamp(closingDate),
                        createdByUid = currentUserUid,
                        createdByName = userProfile?.displayName ?: "Seller",
                        photoUrls = emptyList(),
                        status = "ACTIVE"
                    )

                    val newId = listingsRepository.generateNewListingId()
                    val photoUrls = listingsRepository.uploadImages(urisToUpload, newId)
                    
                    val finalListing = listing.copy(id = newId, photoUrls = photoUrls)
                    listingsRepository.createListing(finalListing)
                    newId
                }

                _uiState.value = CreateListingState.Success(listingId)
            } catch (e: Exception) {
                _uiState.value = CreateListingState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }
}
