package com.example.asa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowInsetsControllerCompat
import com.example.asa.model.AppTheme
import com.example.asa.navigation.AppNavigation
import com.example.asa.repository.DataStoreSettingsRepository
import com.example.asa.session.SharedPreferencesSessionManager
import com.example.asa.ui.theme.ASATheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = SharedPreferencesSessionManager(this)
        val settingsRepository = DataStoreSettingsRepository(this)

        setContent {
            val theme by settingsRepository.themeFlow.collectAsState(initial = AppTheme.YELLOW)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> systemDark
                AppTheme.YELLOW -> false
            }
            val yellowTheme = theme == AppTheme.YELLOW

            ASATheme(darkTheme = darkTheme, yellowTheme = yellowTheme) {
                val navBarColor = MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.isAppearanceLightStatusBars = !darkTheme && !yellowTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme && !yellowTheme
                    @Suppress("DEPRECATION")
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        window.statusBarColor = navBarColor
                        window.navigationBarColor = navBarColor
                    }
                }
                AppNavigation(
                    sessionManager = sessionManager,
                    settingsRepository = settingsRepository
                )
            }
        }
    }
}
