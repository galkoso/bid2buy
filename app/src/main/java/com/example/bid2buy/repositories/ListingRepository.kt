package com.example.bid2buy.repositories

import android.net.Uri
import com.example.bid2buy.data.local.dao.ListingDao
import com.example.bid2buy.data.mapper.toDomain
import com.example.bid2buy.data.mapper.toEntity
import com.example.bid2buy.model.Listing
import com.example.bid2buy.util.TimeUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ListingRepository(
    private val listingDao: ListingDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun observeActiveListings(currentTimeMillis: Long): Flow<List<Listing>> {
        return listingDao.getActiveListings(currentTimeMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeListing(listingId: String): Flow<Listing?> {
        return listingDao.getListingById(listingId).map { it?.toDomain() }
    }

    fun observeUserListings(uid: String): Flow<List<Listing>> {
        return listingDao.getListingsByUser(uid).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshActiveListings(limit: Long = 20): DocumentSnapshot? {
        try {
            val now = Timestamp.now()
            val snapshot = firestore.collection("listings")
                .whereEqualTo("status", "ACTIVE")
                .whereGreaterThan("closingAt", now)
                .orderBy("closingAt", Query.Direction.ASCENDING)
                .limit(limit)
                .get()
                .await()
            
            val remoteListings = snapshot.toObjects(Listing::class.java)
            val entities = remoteListings.map { it.toEntity() }
            
            listingDao.replaceActiveListings(entities)
            
            return snapshot.documents.lastOrNull()
        } catch (_: Exception) {
            return null
        }
    }

    suspend fun loadMoreActiveListings(lastVisible: DocumentSnapshot?, limit: Long = 20): DocumentSnapshot? {
        if (lastVisible == null) return null
        
        try {
            val now = Timestamp.now()
            val snapshot = firestore.collection("listings")
                .whereEqualTo("status", "ACTIVE")
                .whereGreaterThan("closingAt", now)
                .orderBy("closingAt", Query.Direction.ASCENDING)
                .startAfter(lastVisible)
                .limit(limit)
                .get()
                .await()
            
            val remoteListings = snapshot.toObjects(Listing::class.java)
            val entities = remoteListings.map { it.toEntity() }
            
            listingDao.upsertListings(entities)
            
            return snapshot.documents.lastOrNull()
        } catch (_: Exception) {
            return null
        }
    }

    suspend fun refreshListing(listingId: String) {
        try {
            val snapshot = firestore.collection("listings").document(listingId).get().await()
            val remoteListing = snapshot.toObject(Listing::class.java)
            remoteListing?.let {
                listingDao.upsertListing(it.toEntity())
            }
        } catch (_: Exception) { }
    }

    suspend fun refreshUserListings(uid: String) {
        try {
            val snapshot = firestore.collection("listings")
                .whereEqualTo("createdByUid", uid)
                .get()
                .await()
            
            val remoteListings = snapshot.toObjects(Listing::class.java)
            listingDao.upsertListings(remoteListings.map { it.toEntity() })
        } catch (_: Exception) { }
    }

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

    fun generateNewListingId(): String {
        return firestore.collection("listings").document().id
    }

    suspend fun createListing(listing: Listing) {
        val documentRef = if (listing.id.isEmpty()) {
            firestore.collection("listings").document()
        } else {
            firestore.collection("listings").document(listing.id)
        }
        
        val finalListing = listing.copy(id = documentRef.id)
        documentRef.set(finalListing).await()
        listingDao.upsertListing(finalListing.toEntity())
    }

    suspend fun updateListing(listingId: String, updates: Map<String, Any>) {
        val docRef = firestore.collection("listings").document(listingId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentListing = snapshot.toObject(Listing::class.java) 
                ?: throw Exception("Listing not found")

            val now = TimeUtils.now()
            if (currentListing.closingAt != null && currentListing.closingAt < now) {
                throw Exception("The auction has already closed")
            }

            if (currentListing.bidCount > 0) {
                val allowedKeys = setOf("title", "description", "location", "photoUrls")
                val illegalKeys = updates.keys.filter { it !in allowedKeys }
                if (illegalKeys.isNotEmpty()) {
                    throw Exception("Bidding has started. Cannot update: ${illegalKeys.joinToString()}")
                }
            }

            transaction.update(docRef, updates)
        }.await()
        
        refreshListing(listingId)
    }

    suspend fun deleteListing(listingId: String) {
        val doc = firestore.collection("listings").document(listingId).get().await()
        val createdByUid = doc.getString("createdByUid") ?: return
        
        try {
            val storageRef = storage.reference
                .child("listing_photos")
                .child(createdByUid)
                .child(listingId)
            
            val listResult = storageRef.listAll().await()
            listResult.items.forEach { it.delete().await() }
        } catch (_: Exception) { }

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
        } catch (_: Exception) { }

        firestore.collection("listings").document(listingId).delete().await()
        listingDao.deleteListing(listingId)
    }

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
        } catch (_: Exception) { }
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid
}
