package com.example.bid2buy.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Bid(
    val id: String = "",
    val listingId: String = "",
    val bidderUid: String = "",
    val bidderName: String = "",
    val amount: Double = 0.0,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)
