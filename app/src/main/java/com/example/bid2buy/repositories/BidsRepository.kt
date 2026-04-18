package com.example.bid2buy.repositories

import com.example.bid2buy.model.Bid
import com.example.bid2buy.model.Listing
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class BidsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun placeBid(listingId: String, amount: Double) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val displayName = auth.currentUser?.displayName ?: "User"

        val listingDocRef = firestore.collection("listings").document(listingId)
        val bidsCollectionRef = firestore.collection("bids")

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(listingDocRef)
            val listing = snapshot.toObject(Listing::class.java) ?: throw Exception("Listing not found")

            // Validation
            val now = Timestamp.now()
            if (listing.closingAt != null && listing.closingAt.compareTo(now) < 0) {
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

            // Create Bid Document
            val bidDocRef = bidsCollectionRef.document()
            val bid = Bid(
                id = bidDocRef.id,
                listingId = listingId,
                bidderUid = uid,
                bidderName = displayName,
                amount = amount,
                timestamp = Timestamp.now()
            )

            // Update Listing
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
    }

    suspend fun getBidsForUser(uid: String): List<Bid> {
        return firestore.collection("bids")
            .whereEqualTo("bidderUid", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Bid::class.java)
    }

    suspend fun getListingsByIds(listingIds: List<String>): List<Listing> {
        if (listingIds.isEmpty()) return emptyList()
        // Firestore 'in' query supports up to 10 elements. 
        // For a school project, we can chunk it or assume small amount for now.
        // Let's implement a simple chunked fetch.
        return listingIds.chunked(10).flatMap { chunk ->
            firestore.collection("listings")
                .whereIn("id", chunk)
                .get()
                .await()
                .toObjects(Listing::class.java)
        }
    }

    fun getCurrentUserUid(): String? = auth.currentUser?.uid
}
