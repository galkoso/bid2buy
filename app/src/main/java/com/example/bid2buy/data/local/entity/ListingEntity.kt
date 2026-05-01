package com.example.bid2buy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val condition: String,
    val location: String,
    val startingPrice: Double,
    val currency: String,
    val closingAtMillis: Long?,
    val createdAtMillis: Long?,
    val createdByUid: String,
    val createdByName: String,
    val photoUrls: String, // Stored as comma-separated string
    val status: String,
    val currentHighestBid: Double?,
    val highestBidderUid: String?,
    val highestBidderName: String?,
    val bidCount: Int
)
