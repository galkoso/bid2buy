package com.example.bid2buy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bid2buy.data.local.entity.ListingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings WHERE status = 'ACTIVE' AND closingAtMillis > :currentTimeMillis ORDER BY closingAtMillis ASC")
    fun getActiveListings(currentTimeMillis: Long): Flow<List<ListingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListings(listings: List<ListingEntity>)

    @Query("DELETE FROM listings WHERE id = :id")
    suspend fun deleteListing(id: String)

    @Query("DELETE FROM listings WHERE status = 'ACTIVE'")
    suspend fun clearActiveListings()
}
