package com.example.asa.repository

import com.example.asa.App
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class AppConfigDto(
    @SerialName("key") val key: String,
    @SerialName("value") val value: String
)

object AppVersion {
    const val CURRENT = "1.2"
}

class AppConfigRepository {
    private val table get() = App.supabase.postgrest.from("app_config")

    suspend fun getMinVersion(): Result<String> = try {
        val row = table.select {
            filter { eq("key", "min_version") }
        }.decodeSingle<AppConfigDto>()
        Result.success(row.value)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun isVersionSupported(minVersion: String): Boolean {
        return try {
            val current = AppVersion.CURRENT.split(".").map { it.toInt() }
            val min = minVersion.split(".").map { it.toInt() }
            for (i in 0 until maxOf(current.size, min.size)) {
                val c = current.getOrElse(i) { 0 }
                val m = min.getOrElse(i) { 0 }
                if (c > m) return true
                if (c < m) return false
            }
            true // равны — поддерживается
        } catch (e: Exception) {
            true // не смогли распарсить — пускаем
        }
    }
}
