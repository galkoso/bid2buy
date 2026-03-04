package com.example.bid2buy.repositories

import com.example.bid2buy.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val usersCollection = firestore.collection("users")

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

    suspend fun updateUserProfile(uid: String, displayName: String) {
        usersCollection.document(uid).update(
            mapOf(
                "displayName" to displayName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}