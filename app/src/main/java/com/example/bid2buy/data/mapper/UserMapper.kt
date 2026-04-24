package com.example.bid2buy.data.mapper

import com.example.bid2buy.data.local.entity.UserEntity
import com.example.bid2buy.model.UserProfile
import com.google.firebase.Timestamp

fun UserProfile.toEntity(): UserEntity {
    return UserEntity(
        uid = uid,
        displayName = displayName,
        email = email,
        phoneNumber = phoneNumber,
        location = location,
        bio = bio,
        photoURL = photoURL,
        createdAtMillis = createdAt?.toDate()?.time,
        updatedAtMillis = updatedAt?.toDate()?.time,
        totalItemsSold = totalItemsSold,
        activeListingsCount = activeListingsCount,
        activeBidsCount = activeBidsCount,
        winsCount = winsCount,
        totalBids = totalBids
    )
}

fun UserEntity.toDomain(): UserProfile {
    return UserProfile(
        uid = uid,
        displayName = displayName,
        email = email,
        phoneNumber = phoneNumber,
        location = location,
        bio = bio,
        photoURL = photoURL,
        createdAt = createdAtMillis?.let { Timestamp(java.util.Date(it)) },
        updatedAt = updatedAtMillis?.let { Timestamp(java.util.Date(it)) },
        totalItemsSold = totalItemsSold,
        activeListingsCount = activeListingsCount,
        activeBidsCount = activeBidsCount,
        winsCount = winsCount,
        totalBids = totalBids
    )
}
