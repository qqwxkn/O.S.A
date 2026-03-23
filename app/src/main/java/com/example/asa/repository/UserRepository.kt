package com.example.asa.repository

import com.example.asa.model.User

interface UserRepository {
    suspend fun register(login: String, passwordHash: String, nickname: String, phone: String): Result<User>
    suspend fun login(phone: String, passwordHash: String): Result<User>
    suspend fun getUserById(id: String): Result<User>
    suspend fun updateUser(id: String, login: String? = null, nickname: String? = null, passwordHash: String? = null, vkId: String? = null): Result<Unit>
    suspend fun incrementRequestsCount(id: String): Result<Unit>
}
