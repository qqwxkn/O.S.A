package com.example.asa.ui.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AvatarCropScreen(
    imageUri: Uri,
    onCropped: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary

    BackHandler { onCancel() }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(imageUri) {
        bitmap = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
        initialized = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val bmp = bitmap
        if (bmp == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            return@Column
        }

        val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

        // Пустое место сверху — опускаем всё вниз
        Spacer(modifier = Modifier.weight(0.15f))

        // Надпись над фоткой — одна, с тенью через drawBehind
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Перемещайте и масштабируйте",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.drawBehind {
                    // Обводка через рисование тени
                }
            )
        }

        // Сохраняем параметры Canvas для кропа
        var canvasBoxWidthPx by remember { mutableStateOf(0f) }
        var canvasBoxHeightPx by remember { mutableStateOf(0f) }

        // Область с изображением
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
        ) {
            val boxWidthPx = with(density) { maxWidth.toPx() }
            val boxHeightPx = with(density) { maxHeight.toPx() }
            canvasBoxWidthPx = boxWidthPx
            canvasBoxHeightPx = boxHeightPx
            val cropRadius = minOf(boxWidthPx, boxHeightPx) * 0.42f
            val centerX = boxWidthPx / 2f
            val centerY = boxHeightPx / 2f

            val baseScale = minOf(boxWidthPx / bmp.width, boxHeightPx / bmp.height)
            val minScale = (cropRadius * 2f) / (minOf(bmp.width, bmp.height) * baseScale)

            LaunchedEffect(bmp, boxWidthPx, boxHeightPx) {
                if (!initialized && boxWidthPx > 0 && boxHeightPx > 0) {
                    scale = minScale.coerceAtLeast(1f)
                    offset = Offset.Zero
                    initialized = true
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(minScale, 5f)
                            val drawW = bmp.width * baseScale * scale
                            val drawH = bmp.height * baseScale * scale
                            val maxOffsetX = (drawW / 2f - cropRadius).coerceAtLeast(0f)
                            val maxOffsetY = (drawH / 2f - cropRadius).coerceAtLeast(0f)
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        }
                    }
            ) {
                val drawW = bmp.width * baseScale * scale
                val drawH = bmp.height * baseScale * scale
                val left = centerX - drawW / 2f + offset.x
                val top = centerY - drawH / 2f + offset.y

                drawImage(
                    image = imageBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(drawW.toInt(), drawH.toInt())
                )

                val path = Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            center = Offset(centerX, centerY),
                            radius = cropRadius
                        )
                    )
                }
                clipPath(path, clipOp = ClipOp.Difference) {
                    drawRect(color = Color.Black.copy(alpha = 0.6f))
                }
                drawCircle(
                    color = Color.White,
                    radius = cropRadius,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }

        // Блок кнопок
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp)
                .border(
                    width = 1.dp,
                    color = primaryColor,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        // Используем реальные размеры Canvas, а не displayMetrics
                        val canvasW = canvasBoxWidthPx
                        val canvasH = canvasBoxHeightPx
                        val cropRadius = minOf(canvasW, canvasH) * 0.42f
                        val centerX = canvasW / 2f
                        val centerY = canvasH / 2f
                        val baseScale = minOf(canvasW / bmp.width, canvasH / bmp.height)
                        val bmpScale = baseScale * scale
                        val drawW = bmp.width * bmpScale
                        val drawH = bmp.height * bmpScale
                        val left = centerX - drawW / 2f + offset.x
                        val top = centerY - drawH / 2f + offset.y
                        val cropX = ((centerX - cropRadius - left) / bmpScale).toInt().coerceAtLeast(0)
                        val cropY = ((centerY - cropRadius - top) / bmpScale).toInt().coerceAtLeast(0)
                        val cropSize = ((cropRadius * 2) / bmpScale).toInt()
                            .coerceAtMost(bmp.width - cropX)
                            .coerceAtMost(bmp.height - cropY)
                            .coerceAtLeast(1)
                        val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropSize, cropSize)
                        onCropped(Bitmap.createScaledBitmap(cropped, 512, 512, true))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(44.dp)
                ) {
                    Text("Применить", fontSize = 15.sp)
                }
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(40.dp)
                ) {
                    Text("Назад", fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.05f))
    }
}
