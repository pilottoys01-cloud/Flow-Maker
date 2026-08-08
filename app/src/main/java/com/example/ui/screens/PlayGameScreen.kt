package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.model.GameProject
import com.example.model.GameScene
import com.example.ui.components.SpriteRenderer
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PlayGameScreen(
    engine: GameEngine,
    project: GameProject,
    scene: GameScene,
    onBackToEditor: () -> Unit
) {
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(scene.id) {
        engine.startScene(project, scene)
        var lastTime = System.nanoTime()
        while (isRunning) {
            val now = System.nanoTime()
            val delta = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
            lastTime = now
            engine.updateTick(delta)
            delay(16) // ~60 FPS tick loop
        }
    }

    // Matching Wireframe #3 Game Viewport
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val wDp = maxWidth.value
        val hDp = maxHeight.value

        if (wDp > 0f) engine.viewportWidth = wDp
        if (hDp > 0f) engine.viewportHeight = hDp

        LaunchedEffect(wDp, hDp) {
            if (wDp > 0f) engine.viewportWidth = wDp
            if (hDp > 0f) engine.viewportHeight = hDp
        }

        // Game Engine Scene Canvas
        val bgColor = try { Color(android.graphics.Color.parseColor(engine.backgroundColorHex)) } catch (e: Exception) { Color(0xFF0F172A) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            // Render Runtime Game Objects
            engine.runtimeObjects.forEach { obj ->
                val screenXDp = obj.x + engine.cameraX
                val screenYDp = obj.y + engine.cameraY
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                with(density) { screenXDp.dp.roundToPx() },
                                with(density) { screenYDp.dp.roundToPx() }
                            )
                        }
                        .size(obj.width.dp, obj.height.dp)
                        .clickable { engine.triggerObjectTouch(obj) }
                ) {
                    val currentFrame = if (!obj.currentAnimationName.isNullOrEmpty()) {
                        val anim = obj.animations.find { it.name.equals(obj.currentAnimationName, ignoreCase = true) } ?: obj.animations.firstOrNull()
                        anim?.frames?.getOrNull(obj.currentFrameIndex)
                    } else null

                    val preset = currentFrame?.customSpritePreset ?: obj.spritePreset
                    val imageUri = currentFrame?.customImageUri ?: obj.imageUri

                    SpriteRenderer(
                        preset = preset,
                        imageUri = imageUri,
                        colorHex = obj.colorHex,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Render Runtime UI Buttons on Screen matching Wireframe #3
            engine.runtimeUIButtons.forEach { btn ->
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                with(density) { btn.x.dp.roundToPx() },
                                with(density) { btn.y.dp.roundToPx() }
                            )
                        }
                        .size(btn.width.dp, btn.height.dp)
                ) {
                    Button(
                        onClick = { engine.triggerUIButton(btn.actionKey) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = try { Color(android.graphics.Color.parseColor(btn.colorHex)) } catch (e: Exception) { Color(0xFFFF9800) }
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                            .testTag("runtime_ui_btn_${btn.actionKey}")
                    ) {
                        Text(
                            text = btn.label,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // HUD Overlay (Variables score, lives, etc)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                engine.globalVars.forEach { (key, value) ->
                    Text(
                        text = "${key.uppercase()}: $value",
                        fontWeight = FontWeight.Black,
                        color = Color.Yellow,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Top Overlay Navigation Buttons matching Wireframe #3 top left [<] [Refresh]
        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    isRunning = false
                    onBackToEditor()
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .testTag("exit_play_mode_button")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            IconButton(
                onClick = {
                    engine.startScene(project, scene)
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .testTag("restart_scene_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White)
            }
        }
    }
}
