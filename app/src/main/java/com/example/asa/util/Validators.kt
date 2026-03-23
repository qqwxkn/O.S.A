package com.example.asa.util

fun validateRegistrationForm(login: String, password: String, nickname: String, phone: String): Result<Unit> {
    return when {
        login.isBlank() -> Result.failure(Exception("Логин не может быть пустым"))
        password.isBlank() -> Result.failure(Exception("Пароль не может быть пустым"))
        nickname.isBlank() -> Result.failure(Exception("Никнейм не может быть пустым"))
        phone.isBlank() -> Result.failure(Exception("Номер телефона не может быть пустым"))
        else -> Result.success(Unit)
    }
}

fun validateLoginForm(phone: String, password: String): Result<Unit> {
    return when {
        phone.isBlank() -> Result.failure(Exception("Номер телефона не может быть пустым"))
        password.isBlank() -> Result.failure(Exception("Пароль не может быть пустым"))
        else -> Result.success(Unit)
    }
}
