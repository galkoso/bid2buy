package com.example.bid2buy.repositories

import android.net.Uri
import com.example.bid2buy.model.Listing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    suspend fun updateListing(listingId: String, updates: Map<String, Any>) {
        val docRef = firestore.collection("listings").document(listingId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentListing = snapshot.toObject(Listing::class.java) 
                ?: throw Exception("Listing not found")

            val now = Timestamp.now()
            if (currentListing.closingAt != null && currentListing.closingAt.compareTo(now) < 0) {
                throw Exception("The auction has already closed")
            }

            if (currentListing.bidCount > 0) {
                throw Exception("Bidding has already started on this item")
            }

            transaction.update(docRef, updates)
        }.await()
    }

    fun getListing(listingId: String): Flow<Listing?> = callbackFlow {
        val subscription = firestore.collection("listings").document(listingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val listing = snapshot?.toObject(Listing::class.java)
                trySend(listing)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun deleteListing(listingId: String) {
        val uid = auth.currentUser?.uid ?: return
        
        try {
            val storageRef = storage.reference
                .child("listing_photos")
                .child(uid)
                .child(listingId)
            
            val listResult = storageRef.listAll().await()
            listResult.items.forEach { it.delete().await() }
        } catch (e: Exception) {
        }

        firestore.collection("listings").document(listingId).delete().await()
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    fun getFirestoreInstance() = firestore
}
