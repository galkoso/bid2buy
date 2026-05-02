package com.example.bid2buy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.bid2buy.data.local.entity.ListingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings WHERE status = 'ACTIVE' AND closingAtMillis > :currentTimeMillis ORDER BY closingAtMillis ASC")
    fun getActiveListings(currentTimeMillis: Long): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE id = :id")
    fun getListingById(id: String): Flow<ListingEntity?>

    @Query("SELECT * FROM listings WHERE createdByUid = :uid ORDER BY createdAtMillis DESC")
    fun getListingsByUser(uid: String): Flow<List<ListingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListings(listings: List<ListingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListing(listing: ListingEntity)

    @Query("DELETE FROM listings WHERE id = :id")
    suspend fun deleteListing(id: String)

    @Query("DELETE FROM listings WHERE status = 'ACTIVE'")
    suspend fun clearActiveListings()
    
    @Transaction
    suspend fun replaceActiveListings(listings: List<ListingEntity>) {
        clearActiveListings()
        upsertListings(listings)
    }

    @Query("DELETE FROM listings")
    suspend fun clearAll()
}
