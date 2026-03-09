package com.example.bid2buy.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val condition: String = "",
    val location: String = "",
    val startingPrice: Double = 0.0,
    val closingAt: Timestamp? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val createdByUid: String = "",
    val createdByName: String = "Seller",
    val photoUrls: List<String> = emptyList(),
    val status: String = "ACTIVE",
    
    // Bid related fields
    val currentHighestBid: Double? = null,
    val highestBidderUid: String? = null,
    val highestBidderName: String? = null,
    val bidCount: Int = 0
)
