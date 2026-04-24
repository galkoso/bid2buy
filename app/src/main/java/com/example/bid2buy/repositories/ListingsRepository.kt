package com.example.bid2buy.repositories

import android.net.Uri
import com.example.bid2buy.model.Listing
import com.example.bid2buy.util.TimeUtils
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
import java.util.Calendar
import java.util.Date

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
                    .child("photo_${TimeUtils.currentTimeMillis()}_$index.jpg")
                
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

            val now = TimeUtils.now()
            if (currentListing.closingAt != null && currentListing.closingAt.compareTo(now) < 0) {
                throw Exception("The auction has already closed")
            }

            if (currentListing.bidCount > 0) {
                // If there are bids, only allow specific fields
                val allowedKeys = setOf("title", "description", "location", "photoUrls")
                val illegalKeys = updates.keys.filter { it !in allowedKeys }
                if (illegalKeys.isNotEmpty()) {
                    throw Exception("Bidding has started. Cannot update: ${illegalKeys.joinToString()}")
                }
            }

            transaction.update(docRef, updates)
        }.await()
    }

    fun getListing(listingId: String): Flow<Listing?> = callbackFlow {
        val subscription = firestore.collection("listings")
            .whereEqualTo("id", listingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val listing = snapshot?.toObjects(Listing::class.java)?.firstOrNull()
                trySend(listing)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun deleteListing(listingId: String) {
        val doc = firestore.collection("listings").document(listingId).get().await()
        val createdByUid = doc.getString("createdByUid") ?: return
        
        // 1. Delete associated images from Firebase Storage
        try {
            val storageRef = storage.reference
                .child("listing_photos")
                .child(createdByUid)
                .child(listingId)
            
            val listResult = storageRef.listAll().await()
            listResult.items.forEach { it.delete().await() }
        } catch (e: Exception) {
            // Photos might not exist or storage path might differ
        }

        // 2. Delete all bids associated with this listing
        try {
            val bidsQuery = firestore.collection("bids")
                .whereEqualTo("listingId", listingId)
                .get()
                .await()
            
            val batch = firestore.batch()
            for (bidDoc in bidsQuery.documents) {
                batch.delete(bidDoc.reference)
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Error deleting bids
        }

        // 3. Finally, delete the listing document itself
        firestore.collection("listings").document(listingId).delete().await()
    }

    /**
     * Removes listings that closed more than 1 year ago.
     */
    suspend fun cleanupOldListings() {
        try {
            val oneYearAgo = Calendar.getInstance().apply {
                timeInMillis = TimeUtils.currentTimeMillis()
                add(Calendar.YEAR, -1)
            }.time

            val oldListingsQuery = firestore.collection("listings")
                .whereLessThan("closingAt", Timestamp(oneYearAgo))
                .get()
                .await()

            for (document in oldListingsQuery.documents) {
                deleteListing(document.id)
            }
        } catch (e: Exception) {
        }
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    fun getFirestoreInstance() = firestore
}
