package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun SpriteRenderer(
    preset: String,
    imageUri: String?,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    val tintColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    if (!imageUri.isNullOrEmpty()) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Custom Object Image",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(6.dp))
        )
    } else {
        Canvas(modifier = modifier) {
            val w = size.width
            val h = size.height

            when (preset.uppercase()) {
                "PLAYER" -> {
                    // Player robot / hero icon
                    drawRoundRect(
                        color = tintColor,
                        size = Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    // Visor
                    drawRoundRect(
                        color = Color.Cyan,
                        topLeft = Offset(w * 0.15f, h * 0.2f),
                        size = Size(w * 0.7f, h * 0.25f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    // Eye glow
                    drawCircle(
                        color = Color.White,
                        center = Offset(w * 0.35f, h * 0.32f),
                        radius = w * 0.06f
                    )
                    drawCircle(
                        color = Color.White,
                        center = Offset(w * 0.65f, h * 0.32f),
                        radius = w * 0.06f
                    )
                }
                "ENEMY" -> {
                    // Spiky enemy shape
                    val path = Path().apply {
                        moveTo(w / 2f, 0f)
                        lineTo(w, h / 3f)
                        lineTo(w * 0.8f, h)
                        lineTo(w * 0.2f, h)
                        lineTo(0f, h / 3f)
                        close()
                    }
                    drawPath(path = path, color = tintColor)
                    // Angry eyes
                    drawCircle(color = Color.Yellow, center = Offset(w * 0.35f, h * 0.4f), radius = w * 0.08f)
                    drawCircle(color = Color.Yellow, center = Offset(w * 0.65f, h * 0.4f), radius = w * 0.08f)
                }
                "PLATFORM" -> {
                    // Platform tile
                    drawRoundRect(
                        color = tintColor,
                        size = Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    // Top highlight line
                    drawRect(
                        color = Color.White.copy(alpha = 0.4f),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h * 0.2f)
                    )
                }
                "COIN" -> {
                    // Gold coin
                    drawCircle(color = Color(0xFFF59E0B), center = Offset(w / 2f, h / 2f), radius = w / 2f)
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.35f,
                        style = Stroke(width = 4f)
                    )
                }
                "SPACESHIP" -> {
                    val path = Path().apply {
                        moveTo(w / 2f, 0f)
                        lineTo(w, h)
                        lineTo(w / 2f, h * 0.8f)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = path, color = tintColor)
                }
                "BULLET" -> {
                    drawOval(color = tintColor, size = Size(w, h))
                }
                "HEART" -> {
                    val path = Path().apply {
                        moveTo(w / 2f, h * 0.8f)
                        cubicTo(0f, h * 0.5f, 0f, h * 0.1f, w / 2f, h * 0.3f)
                        cubicTo(w, h * 0.1f, w, h * 0.5f, w / 2f, h * 0.8f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFEF4444))
                }
                else -> { // BOX
                    drawRoundRect(
                        color = tintColor,
                        size = Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.5f),
                        size = Size(w, h),
                        style = Stroke(width = 3f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }
        }
    }
}
