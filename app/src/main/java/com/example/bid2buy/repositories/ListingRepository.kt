package com.example.bid2buy.repositories

import com.example.bid2buy.data.local.dao.ListingDao
import com.example.bid2buy.data.mapper.toDomain
import com.example.bid2buy.data.mapper.toEntity
import com.example.bid2buy.model.Listing
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ListingRepository(
    private val firestore: FirebaseFirestore,
    private val listingDao: ListingDao
) {
    // Room is used as SQLite local cache
    // Firebase is the remote data source
    // Repository syncs Firebase data into Room
    fun observeActiveListings(): Flow<List<Listing>> {
        return listingDao.getActiveListings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshActiveListings() {
        try {
            val snapshot = firestore.collection("listings")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .await()
            
            val remoteListings = snapshot.toObjects(Listing::class.java)
            val entities = remoteListings.map { it.toEntity() }
            
            // Perform update in a single transaction or sequence to minimize Flow emissions
            listingDao.clearActiveListings()
            listingDao.upsertListings(entities)
        } catch (e: Exception) {
            // Offline support: ignore errors and keep using cached data
        }
    }
}
