package com.example.bid2buy.repositories

import android.net.Uri
import com.example.bid2buy.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val usersCollection = firestore.collection("users")
    private val listingsCollection = firestore.collection("listings")

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

    fun observeUser(uid: String): Flow<UserProfile?> {
        val userFlow = MutableStateFlow<UserProfile?>(null)
        usersCollection.document(uid).addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                userFlow.value = it.toObject<UserProfile>()
            }
        }
        return userFlow
    }

    suspend fun refreshUser(uid: String): UserProfile? {
        val userDoc = usersCollection.document(uid).get().await()
        return userDoc.toObject<UserProfile>()
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
        val biddedListings = listingsCollection.whereEqualTo("highestBidderUid", uid).get().await()

        firestore.runTransaction { transaction ->
            val userRef = usersCollection.document(uid)
            val updates = mutableMapOf<String, Any>(
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

            biddedListings.documents.forEach { doc ->
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
}
