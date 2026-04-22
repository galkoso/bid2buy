package com.example.bid2buy.repositories

import com.example.bid2buy.model.Bid
import com.example.bid2buy.model.Listing
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    fun observeBidsForListing(listingId: String): Flow<List<Bid>> = callbackFlow {
        val listener = firestore.collection("bids")
            .whereEqualTo("listingId", listingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val bids = snapshot?.toObjects(Bid::class.java) ?: emptyList()
                // Sort in memory to avoid potential missing index errors or generic permission denials
                trySend(bids.sortedByDescending { it.amount })
            }
        awaitClose { listener.remove() }
    }

    fun observeActiveBidsCount(uid: String): Flow<Int> = callbackFlow {
        var listingListener: ListenerRegistration? = null
        
        val bidsListener = firestore.collection("bids")
            .whereEqualTo("bidderUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val userBids = snapshot?.toObjects(Bid::class.java) ?: emptyList()
                val listingIds = userBids.map { it.listingId }.distinct()

                listingListener?.remove()
                
                if (listingIds.isEmpty()) {
                    trySend(0)
                } else {
                    listingListener = firestore.collection("listings")
                        .whereIn("id", listingIds.take(10))
                        .addSnapshotListener { listingSnapshot, listingError ->
                            if (listingError != null) {
                                trySend(0)
                                return@addSnapshotListener
                            }
                            val now = Timestamp.now()
                            val listings = listingSnapshot?.toObjects(Listing::class.java) ?: emptyList()
                            val activeCount = listings.count { listing ->
                                val isExpired = listing.closingAt?.let { it.toDate().time <= now.toDate().time } ?: false
                                listing.status == "ACTIVE" && !isExpired
                            }
                            trySend(activeCount)
                        }
                }
            }
            
        awaitClose { 
            bidsListener.remove()
            listingListener?.remove()
        }
    }

    fun observeWinsCount(uid: String): Flow<Int> = callbackFlow {
        val listener = firestore.collection("listings")
            .whereEqualTo("highestBidderUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val now = Timestamp.now()
                val listings = snapshot?.toObjects(Listing::class.java) ?: emptyList()
                val winsCount = listings.count { listing ->
                    val isExpired = listing.closingAt?.let { it.toDate().time <= now.toDate().time } ?: false
                    val isClosed = listing.status == "CLOSED" || isExpired
                    isClosed
                }
                trySend(winsCount)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getListingsByIds(listingIds: List<String>): List<Listing> {
        if (listingIds.isEmpty()) return emptyList()
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
