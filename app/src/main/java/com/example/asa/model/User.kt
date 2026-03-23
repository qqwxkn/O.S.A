package com.example.asa.model

data class User(
    val id: String,
    val login: String,
    val nickname: String,
    val phone: String,
    val vkId: String?,
    val avatarUrl: String?,
    val requestsCount: Int,
    val createdAt: String,
    val hasPassword: Boolean = false
)
