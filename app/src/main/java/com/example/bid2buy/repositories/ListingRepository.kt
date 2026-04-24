package com.example.bid2buy.repositories

import com.example.bid2buy.data.local.dao.ListingDao
import com.example.bid2buy.data.mapper.toDomain
import com.example.bid2buy.data.mapper.toEntity
import com.example.bid2buy.model.Listing
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ListingRepository(
    private val firestore: FirebaseFirestore,
    private val listingDao: ListingDao
) {
    fun observeActiveListings(): Flow<List<Listing>> {
        return listingDao.getActiveListings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshActiveListings(limit: Long = 20): DocumentSnapshot? {
        try {
            val snapshot = firestore.collection("listings")
                .whereEqualTo("status", "ACTIVE")
                .limit(limit)
                .get()
                .await()
            
            val remoteListings = snapshot.toObjects(Listing::class.java)
            val entities = remoteListings.map { it.toEntity() }
            
            listingDao.clearActiveListings()
            listingDao.upsertListings(entities)
            
            return snapshot.documents.lastOrNull()
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun loadMoreActiveListings(lastVisible: DocumentSnapshot?, limit: Long = 20): DocumentSnapshot? {
        if (lastVisible == null) return null
        
        try {
            val snapshot = firestore.collection("listings")
                .whereEqualTo("status", "ACTIVE")
                .startAfter(lastVisible)
                .limit(limit)
                .get()
                .await()
            
            val remoteListings = snapshot.toObjects(Listing::class.java)
            val entities = remoteListings.map { it.toEntity() }
            
            listingDao.upsertListings(entities)
            
            return snapshot.documents.lastOrNull()
        } catch (e: Exception) {
            return null
        }
    }
}
