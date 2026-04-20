package com.example.bid2buy.ui.bids

import com.example.bid2buy.model.Listing

data class BidItemUiModel(
    val listing: Listing,
    val userHighestBid: Double,
    val status: BidStatus
)

enum class BidStatus {
    ACTIVE_WINNING,
    ACTIVE_OUTBID,
    WON,
    LOST
}
