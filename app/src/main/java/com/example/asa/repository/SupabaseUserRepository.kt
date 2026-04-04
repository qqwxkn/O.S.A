package com.example.asa.repository

import com.example.asa.App
import com.example.asa.model.User
import com.example.asa.util.PasswordHasher
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.exceptions.RestException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class UserDto(
    @SerialName("id") val id: String = "",
    @SerialName("login") val login: String = "",
    @SerialName("password_hash") val passwordHash: String = "",
    @SerialName("nickname") val nickname: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("vk_id") val vkId: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("requests_count") val requestsCount: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

private fun UserDto.toDomain() = User(
    id = id,
    login = login,
    nickname = nickname,
    phone = phone,
    vkId = vkId,
    avatarUrl = avatarUrl,
    requestsCount = requestsCount,
    createdAt = createdAt,
    hasPassword = passwordHash.isNotBlank()
)

class SupabaseUserRepository : UserRepository {

    private val table get() = App.supabase.postgrest.from("users")

    override suspend fun register(
        login: String,
        passwordHash: String,
        nickname: String,
        phone: String
    ): Result<User> = try {
        val dto = UserDto(
            login = login,
            passwordHash = passwordHash,
            nickname = nickname,
            phone = phone
        )
        val result = table.insert(dto) {
                select()
            }.decodeSingle<UserDto>()
        Result.success(result.toDomain())
    } catch (e: RestException) {
        Result.failure(mapRestException(e))
    } catch (e: Exception) {
        Result.failure(Exception("Ошибка сети. Проверьте подключение", e))
    }

    override suspend fun login(phone: String, passwordHash: String): Result<User> {
        return try {
            val rows = table.select {
                filter { eq("phone", phone) }
            }.decodeList<UserDto>()

            val user = rows.firstOrNull()
                ?: return Result.failure(Exception("Неверный номер телефона или пароль"))

            if (passwordHash != user.passwordHash) {
                return Result.failure(Exception("Неверный номер телефона или пароль"))
            }

            Result.success(user.toDomain())
        } catch (e: RestException) {
            Result.failure(mapRestException(e))
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка сети. Проверьте подключение", e))
        }
    }

    override suspend fun getUserById(id: String): Result<User> = try {
        val dto = table.select {
            filter { eq("id", id) }
        }.decodeSingle<UserDto>()
        Result.success(dto.toDomain())
    } catch (e: RestException) {
        Result.failure(mapRestException(e))
    } catch (e: Exception) {
        Result.failure(Exception("Ошибка сети. Проверьте подключение", e))
    }

    override suspend fun updateUser(
        id: String,
        login: String?,
        nickname: String?,
        passwordHash: String?,
        vkId: String?
    ): Result<Unit> {
        return try {
            val patch = buildMap<String, String> {
                login?.let { put("login", it) }
                nickname?.let { put("nickname", it) }
                passwordHash?.let { put("password_hash", it) }
                vkId?.let { put("vk_id", it) }
            }
            if (patch.isEmpty()) return Result.success(Unit)

            table.update(patch) {
                filter { eq("id", id) }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(mapRestException(e))
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка сети. Проверьте подключение", e))
        }
    }

    suspend fun updateAvatarUrl(id: String, avatarUrl: String?): Result<Unit> = try {
        table.update(mapOf("avatar_url" to (avatarUrl ?: ""))) {
            filter { eq("id", id) }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception("Ошибка сохранения аватарки", e))
    }

    override suspend fun incrementRequestsCount(id: String): Result<Unit> {
        if (id.isBlank()) return Result.failure(Exception("User ID is blank"))
        return try {
            val current = table.select {
                filter { eq("id", id) }
            }.decodeSingle<UserDto>()
            val newCount = current.requestsCount + 1
            table.update(mapOf("requests_count" to newCount)) {
                filter { eq("id", id) }
            }
            android.util.Log.d("OSA_DEBUG", "incrementRequestsCount: $id -> $newCount")
            Result.success(Unit)
        } catch (e: RestException) {
            android.util.Log.e("OSA_DEBUG", "incrementRequestsCount RestException: ${e.message}")
            Result.failure(mapRestException(e))
        } catch (e: Exception) {
            android.util.Log.e("OSA_DEBUG", "incrementRequestsCount Exception: ${e.message}")
            Result.failure(Exception("Ошибка обновления счётчика", e))
        }
    }

    private fun mapRestException(e: RestException): Exception {
        val msg = e.message?.lowercase() ?: ""
        return when {
            msg.contains("23505") || msg.contains("unique") || msg.contains("duplicate") ->
                Exception("Пользователь с таким логином или номером телефона уже зарегистрирован")
            else -> Exception("Ошибка сервера: ${e.message}", e)
        }
    }
}
