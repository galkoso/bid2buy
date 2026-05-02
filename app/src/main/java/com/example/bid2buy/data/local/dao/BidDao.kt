package com.example.bid2buy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bid2buy.data.local.entity.BidEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BidDao {
    @Query("SELECT * FROM bids WHERE listingId = :listingId ORDER BY amount DESC")
    fun getBidsForListing(listingId: String): Flow<List<BidEntity>>

    @Query("SELECT * FROM bids WHERE bidderUid = :uid ORDER BY timestampMillis DESC")
    fun getBidsByUser(uid: String): Flow<List<BidEntity>>

    @Query("SELECT COUNT(*) FROM bids WHERE bidderUid = :uid")
    fun getBidsCountByUser(uid: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBids(bids: List<BidEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBid(bid: BidEntity)

    @Query("DELETE FROM bids WHERE id = :id")
    suspend fun deleteBid(id: String)
}
