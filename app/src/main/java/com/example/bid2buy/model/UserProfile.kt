package com.example.bid2buy.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val location: String = "",
    val bio: String = "",
    val photoURL: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val totalItemsSold: Int = 0,
    val activeListingsCount: Int = 0,
    val activeBidsCount: Int = 0,
    val winsCount: Int = 0,
    val totalBids: Int = 0
)