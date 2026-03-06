package com.example.bid2buy.repositories

import android.net.Uri
import com.example.bid2buy.model.Listing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class ListingsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun uploadImages(imageUris: List<Uri>, listingId: String): List<String> = coroutineScope {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        
        imageUris.mapIndexed { index, uri ->
            async {
                val storageRef = storage.reference
                    .child("listing_photos")
                    .child(uid)
                    .child(listingId)
                    .child("photo_$index.jpg")
                
                storageRef.putFile(uri).await()
                storageRef.downloadUrl.await().toString()
            }
        }.awaitAll()
    }

    suspend fun createListing(listing: Listing) {
        val documentRef = if (listing.id.isEmpty()) {
            firestore.collection("listings").document()
        } else {
            firestore.collection("listings").document(listing.id)
        }
        
        val finalListing = listing.copy(id = documentRef.id)
        documentRef.set(finalListing).await()
    }

    fun getFirestoreInstance() = firestore
}
