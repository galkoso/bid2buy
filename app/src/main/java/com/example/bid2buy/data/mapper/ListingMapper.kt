package com.example.bid2buy.data.mapper

import com.example.bid2buy.data.local.entity.ListingEntity
import com.example.bid2buy.model.Listing
import com.google.firebase.Timestamp
import java.util.Date

fun Listing.toEntity(): ListingEntity {
    return ListingEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        condition = condition,
        location = location,
        startingPrice = startingPrice,
        closingAtMillis = closingAt?.toDate()?.time,
        createdAtMillis = createdAt?.toDate()?.time,
        createdByUid = createdByUid,
        createdByName = createdByName,
        photoUrls = photoUrls.joinToString(","),
        status = status,
        currentHighestBid = currentHighestBid,
        highestBidderUid = highestBidderUid,
        highestBidderName = highestBidderName,
        bidCount = bidCount
    )
}

fun ListingEntity.toDomain(): Listing {
    return Listing(
        id = id,
        title = title,
        description = description,
        category = category,
        condition = condition,
        location = location,
        startingPrice = startingPrice,
        closingAt = closingAtMillis?.let { Timestamp(Date(it)) },
        createdAt = createdAtMillis?.let { Timestamp(Date(it)) },
        createdByUid = createdByUid,
        createdByName = createdByName,
        photoUrls = if (photoUrls.isEmpty()) emptyList() else photoUrls.split(","),
        status = status,
        currentHighestBid = currentHighestBid,
        highestBidderUid = highestBidderUid,
        highestBidderName = highestBidderName,
        bidCount = bidCount
    )
}
