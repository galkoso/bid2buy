package com.example.bid2buy.data.mapper

import com.example.bid2buy.data.local.entity.BidEntity
import com.example.bid2buy.model.Bid
import com.google.firebase.Timestamp
import java.util.Date

fun Bid.toEntity(): BidEntity {
    return BidEntity(
        id = id,
        listingId = listingId,
        bidderUid = bidderUid,
        bidderName = bidderName,
        amount = amount,
        timestampMillis = timestamp?.toDate()?.time ?: 0L
    )
}

fun BidEntity.toDomain(): Bid {
    return Bid(
        id = id,
        listingId = listingId,
        bidderUid = bidderUid,
        bidderName = bidderName,
        amount = amount,
        timestamp = Timestamp(Date(timestampMillis))
    )
}
