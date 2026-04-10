package com.example.asa.ui.splash

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.asa.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val onFinishedRef = rememberUpdatedState(onFinished)
    var videoReady by remember { mutableStateOf(false) }

    val circleAlpha by animateFloatAsState(
        targetValue = if (videoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "circleAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .alpha(circleAlpha)
                .border(width = 2.dp, color = Color(0xFFB8860B), shape = CircleShape)
                .clip(CircleShape)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            private var mediaPlayer: MediaPlayer? = null

                            override fun onSurfaceTextureAvailable(
                                surfaceTexture: SurfaceTexture,
                                width: Int,
                                height: Int
                            ) {
                                val uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.splash}")
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(ctx, uri)
                                    setSurface(Surface(surfaceTexture))
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = false
                                        mp.start()
                                        // НЕ показываем здесь — ждём первый реальный кадр
                                    }
                                    setOnCompletionListener {
                                        onFinishedRef.value()
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        onFinishedRef.value()
                                        true
                                    }
                                    prepareAsync()
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                mediaPlayer?.release()
                                mediaPlayer = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                                // Первый кадр реально нарисован — показываем круг
                                if (!videoReady) videoReady = true
                            }
                        }
                    }
                }
            )
        }
    }
}
