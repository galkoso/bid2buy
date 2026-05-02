package com.example.bid2buy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bids")
data class BidEntity(
    @PrimaryKey val id: String,
    val listingId: String,
    val bidderUid: String,
    val bidderName: String,
    val amount: Double,
    val timestampMillis: Long
)
