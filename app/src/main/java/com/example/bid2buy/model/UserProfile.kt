package com.example.bid2buy.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    
    // Marketplace stats
    val totalItemsSold: Int = 0,
    val activeListingsCount: Int = 0,
    val activeBidsCount: Int = 0,
    val winsCount: Int = 0,
    val totalBids: Int = 0
) {
    val successRate: Int
        get() = if (totalBids > 0) (winsCount * 100) / totalBids else 0
}