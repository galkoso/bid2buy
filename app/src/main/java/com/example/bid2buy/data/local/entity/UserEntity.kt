package com.example.bid2buy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String,
    val location: String,
    val bio: String,
    val photoURL: String,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    val totalItemsSold: Int,
    val activeListingsCount: Int,
    val activeBidsCount: Int,
    val winsCount: Int,
    val totalBids: Int
)
