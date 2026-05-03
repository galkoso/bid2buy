package com.example.bid2buy.repositories

import android.net.Uri
import com.example.bid2buy.model.Listing
import com.example.bid2buy.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private val usersCollection = firestore.collection("users")
    private val listingsCollection = firestore.collection("listings")

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    suspend fun ensureUserDocumentExists() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = usersCollection.document(uid)
        val userDoc = userRef.get().await()

        if (!userDoc.exists()) {
            val newUser = UserProfile(
                uid = uid,
                displayName = auth.currentUser?.displayName ?: "",
                email = auth.currentUser?.email ?: ""
            )
            userRef.set(newUser).await()
        }
    }

    suspend fun refreshUser(uid: String): UserProfile? {
        val userDoc = usersCollection.document(uid).get().await()
        return userDoc.toObject(UserProfile::class.java)
    }

    suspend fun updateUserProfile(
        uid: String,
        displayName: String,
        phoneNumber: String,
        location: String,
        bio: String,
        photoURL: String? = null
    ) {
        val userListings = listingsCollection.whereEqualTo("createdByUid", uid).get().await()
        val bidListings = listingsCollection.whereEqualTo("highestBidderUid", uid).get().await()

        firestore.runTransaction { transaction ->
            val userRef = usersCollection.document(uid)
            val updates = mutableMapOf(
                "displayName" to displayName,
                "phoneNumber" to phoneNumber,
                "location" to location,
                "bio" to bio,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            photoURL?.let { updates["photoURL"] = it }
            transaction.update(userRef, updates)

            userListings.documents.forEach { doc ->
                transaction.update(doc.reference, "createdByName", displayName)
            }

            bidListings.documents.forEach { doc ->
                transaction.update(doc.reference, "highestBidderName", displayName)
            }
            null
        }.await()
    }

    suspend fun uploadProfileImage(uid: String, imageUri: Uri): String {
        val storageRef = storage.reference
            .child("listing_photos")
            .child(uid)
            .child("profile")
            .child("profile.jpg")

        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    fun observeActiveListingsCount(uid: String): Flow<Int> = callbackFlow {
        val listener = listingsCollection
            .whereEqualTo("createdByUid", uid)
            .whereEqualTo("status", "ACTIVE")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let { querySnapshot ->
                    val now = Timestamp.now()
                    val activeCount = querySnapshot.toObjects(Listing::class.java).count { listing ->
                        listing.closingAt != null && listing.closingAt.toDate().time > now.toDate().time
                    }
                    trySend(activeCount)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeTotalItemsSold(uid: String): Flow<Int> = callbackFlow {
        val listener = listingsCollection
            .whereEqualTo("createdByUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val now = Timestamp.now()
                val soldCount = snapshot?.toObjects(Listing::class.java)?.count { listing ->
                    val isExpired = listing.closingAt?.let { it.toDate().time <= now.toDate().time } ?: false
                    val isClosed = listing.status == "CLOSED" || isExpired
                    isClosed && listing.bidCount > 0
                } ?: 0
                trySend(soldCount)
            }
        awaitClose { listener.remove() }
    }
}
