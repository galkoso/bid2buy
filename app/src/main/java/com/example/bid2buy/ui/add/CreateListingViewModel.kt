package com.example.bid2buy.ui.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bid2buy.model.Listing
import com.example.bid2buy.repositories.FirestoreUserRepository
import com.example.bid2buy.repositories.ListingsRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

sealed class CreateListingState {
    object Idle : CreateListingState()
    object Loading : CreateListingState()
    data class Success(val listingId: String) : CreateListingState()
    data class Error(val message: String) : CreateListingState()
}

class CreateListingViewModel(
    private val listingsRepository: ListingsRepository = ListingsRepository(),
    private val userRepository: FirestoreUserRepository = FirestoreUserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateListingState>(CreateListingState.Idle)
    val uiState: StateFlow<CreateListingState> = _uiState

    private val auth = FirebaseAuth.getInstance()

    fun publishListing(
        title: String,
        description: String,
        category: String,
        condition: String,
        location: String,
        startingPrice: Double,
        closingDate: Date,
        imageUris: List<Uri>
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = CreateListingState.Error("User not authenticated")
            return
        }

        if (imageUris.isEmpty()) {
            _uiState.value = CreateListingState.Error("Please select at least one image")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateListingState.Loading
            try {
                val userProfile = userRepository.refreshUser(currentUser.uid)
                
                if (userProfile == null) {
                    _uiState.value =
                        CreateListingState.Error("User profile not found. Please try again.")
                    return@launch
                }

                val listingCollection = listingsRepository.getFirestoreInstance().collection("listings")
                val listingId = listingCollection.document().id

                val photoUrls = listingsRepository.uploadImages(imageUris, listingId)

                val listing = Listing(
                    id = listingId,
                    title = title,
                    description = description,
                    category = category,
                    condition = condition,
                    location = location,
                    startingPrice = startingPrice,
                    closingAt = Timestamp(closingDate),
                    createdByUid = currentUser.uid,
                    createdByName = userProfile.displayName,
                    photoUrls = photoUrls,
                    status = "ACTIVE"
                )

                listingsRepository.createListing(listing)
                _uiState.value = CreateListingState.Success(listingId)
            } catch (e: Exception) {
                _uiState.value = CreateListingState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }
}
