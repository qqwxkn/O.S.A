package com.example.asa.util

fun normalizePhoneInput(input: String): String {
    val digits = input.filter { it.isDigit() }
    val normalized = when {
        digits.startsWith("7") -> "8" + digits.drop(1)
        digits.startsWith("9") -> "8$digits"
        else -> digits
    }
    return normalized.take(11)
}

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
