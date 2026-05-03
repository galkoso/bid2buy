package com.example.bid2buy.repositories

import com.example.bid2buy.data.local.dao.UserDao
import com.example.bid2buy.data.mapper.toDomain
import com.example.bid2buy.data.mapper.toEntity
import com.example.bid2buy.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(
    private val firestoreRepository: FirestoreUserRepository,
    private val userDao: UserDao
) {
    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        return userDao.getUserById(uid).map { it?.toDomain() }
    }

    suspend fun refreshUserProfile(uid: String) {
        try {
            val remoteUser = firestoreRepository.refreshUser(uid)
            remoteUser?.let {
                userDao.upsertUser(it.toEntity())
            }
        } catch (_: Exception) { }
    }

    suspend fun updateUserProfile(
        uid: String,
        displayName: String,
        phoneNumber: String,
        location: String,
        bio: String,
        photoURL: String? = null
    ) {
        firestoreRepository.updateUserProfile(uid, displayName, phoneNumber, location, bio, photoURL)
        refreshUserProfile(uid)
    }

    fun getCurrentUserUid(): String? = firestoreRepository.getCurrentUserUid()
}
