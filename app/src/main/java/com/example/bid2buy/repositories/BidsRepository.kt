package com.example.bid2buy.repositories

import com.example.bid2buy.data.local.dao.BidDao
import com.example.bid2buy.data.local.dao.ListingDao
import com.example.bid2buy.data.mapper.toDomain
import com.example.bid2buy.data.mapper.toEntity
import com.example.bid2buy.model.Bid
import com.example.bid2buy.model.Listing
import com.example.bid2buy.util.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BidsRepository(
    private val bidDao: BidDao,
    private val listingDao: ListingDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun observeBidsForListing(listingId: String): Flow<List<Bid>> {
        return bidDao.getBidsForListing(listingId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeUserBids(uid: String): Flow<List<Bid>> {
        return bidDao.getBidsByUser(uid).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeTotalBidsCount(uid: String): Flow<Int> {
        return bidDao.getBidsCountByUser(uid)
    }

    fun observeActiveBidsCount(uid: String): Flow<Int> {
        return combine(
            bidDao.getBidsByUser(uid),
            listingDao.getActiveListings(TimeUtils.currentTimeMillis())
        ) { userBids, activeListings ->
            val listingIdsWithBids = userBids.map { it.listingId }.toSet()
            activeListings.count { it.id in listingIdsWithBids }
        }
    }

    fun observeWinsCount(uid: String): Flow<Int> {
        return listingDao.getActiveListings(0).map { allCached ->
            val now = TimeUtils.currentTimeMillis()
            allCached.count { listing ->
                val isExpired = listing.closingAtMillis != null && listing.closingAtMillis <= now
                val isClosed = listing.status == "CLOSED" || isExpired
                isClosed && listing.highestBidderUid == uid
            }
        }
    }

    suspend fun refreshBidsForListing(listingId: String) {
        try {
            val snapshot = firestore.collection("bids")
                .whereEqualTo("listingId", listingId)
                .get()
                .await()
            val remoteBids = snapshot.toObjects(Bid::class.java)
            bidDao.upsertBids(remoteBids.map { it.toEntity() })
        } catch (_: Exception) { }
    }

    suspend fun refreshUserBids(uid: String) {
        try {
            val snapshot = firestore.collection("bids")
                .whereEqualTo("bidderUid", uid)
                .get()
                .await()
            val remoteBids = snapshot.toObjects(Bid::class.java)
            bidDao.upsertBids(remoteBids.map { it.toEntity() })
            
            val listingIds = remoteBids.map { it.listingId }.distinct()
            if (listingIds.isNotEmpty()) {
                refreshListingsByIds(listingIds)
            }
        } catch (_: Exception) { }
    }

    private suspend fun refreshListingsByIds(listingIds: List<String>) {
        listingIds.chunked(10).forEach { chunk ->
            val snapshot = firestore.collection("listings")
                .whereIn("id", chunk)
                .get()
                .await()
            val remoteListings = snapshot.toObjects(Listing::class.java)
            listingDao.upsertListings(remoteListings.map { it.toEntity() })
        }
    }

    suspend fun placeBid(listingId: String, amount: Double) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val displayName = auth.currentUser?.displayName ?: "User"

        val listingDocRef = firestore.collection("listings").document(listingId)
        val bidsCollectionRef = firestore.collection("bids")

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(listingDocRef)
            val listing = snapshot.toObject(Listing::class.java) ?: throw Exception("Listing not found")

            val now = TimeUtils.now()
            if (listing.closingAt != null && listing.closingAt < now) {
                throw Exception("This auction has already closed")
            }
            
            if (listing.createdByUid == uid) {
                throw Exception("You cannot bid on your own listing")
            }

            val currentHighestBid = listing.currentHighestBid ?: 0.0
            val minBid = if (listing.bidCount == 0) listing.startingPrice else currentHighestBid + 1.0

            if (amount < minBid) {
                throw Exception("Bid must be at least ₪$minBid")
            }

            val bidDocRef = bidsCollectionRef.document()
            val bid = Bid(
                id = bidDocRef.id,
                listingId = listingId,
                bidderUid = uid,
                bidderName = displayName,
                amount = amount,
                timestamp = TimeUtils.now()
            )

            transaction.set(bidDocRef, bid)
            transaction.update(
                listingDocRef,
                mapOf(
                    "currentHighestBid" to amount,
                    "highestBidderUid" to uid,
                    "highestBidderName" to displayName,
                    "bidCount" to listing.bidCount + 1
                )
            )
        }.await()
        
        refreshBidsForListing(listingId)
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid
}
