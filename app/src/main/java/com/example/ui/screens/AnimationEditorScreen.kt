package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnimationFrame
import com.example.model.GameAnimation
import com.example.model.GameObject
import com.example.model.GameProject
import com.example.ui.components.SpriteRenderer
import kotlinx.coroutines.delay

@Composable
fun AnimationEditorScreen(
    project: GameProject,
    gameObject: GameObject,
    onBack: () -> Unit,
    onSave: (GameObject) -> Unit
) {
    val animations = remember { mutableStateListOf<GameAnimation>().apply { addAll(gameObject.animations) } }
    var currentAnimIndex by remember { mutableIntStateOf(0) }
    var newAnimName by remember { mutableStateOf("") }
    var showNewAnimDialog by remember { mutableStateOf(false) }
    var showFrameMediaLibraryDialog by remember { mutableStateOf(false) }

    // Ensure at least one default animation exists
    if (animations.isEmpty()) {
        animations.add(
            GameAnimation(
                name = "Walk",
                isLoop = true,
                frames = mutableListOf(
                    AnimationFrame(dx = 0f, dy = 0f, durationMs = 200L, centerCollision = true),
                    AnimationFrame(dx = 15f, dy = -5f, durationMs = 200L, centerCollision = true),
                    AnimationFrame(dx = 30f, dy = 0f, durationMs = 200L, centerCollision = true)
                )
            )
        )
    }

    val currentAnim = animations.getOrNull(currentAnimIndex) ?: animations.first()
    var selectedFrameIndex by remember { mutableIntStateOf(0) }
    var recomposeTrigger by remember { mutableIntStateOf(0) }

    // Ensure selected frame index is within bounds
    if (selectedFrameIndex >= currentAnim.frames.size) {
        selectedFrameIndex = (currentAnim.frames.size - 1).coerceAtLeast(0)
    }

    var isPlaying by remember { mutableStateOf(false) }
    var playingFrameIdx by remember { mutableIntStateOf(0) }

    // Touch-Drag coordinates tracked in real-time Compose States
    var dragX by remember(selectedFrameIndex, currentAnimIndex) { mutableFloatStateOf(currentAnim.frames.getOrNull(selectedFrameIndex)?.dx ?: 0f) }
    var dragY by remember(selectedFrameIndex, currentAnimIndex) { mutableFloatStateOf(currentAnim.frames.getOrNull(selectedFrameIndex)?.dy ?: 0f) }

    // Keep drag state in sync if buttons are pressed or animation frames change
    LaunchedEffect(currentAnimIndex, selectedFrameIndex, currentAnim.frames.getOrNull(selectedFrameIndex)?.dx, currentAnim.frames.getOrNull(selectedFrameIndex)?.dy) {
        val activeFrame = currentAnim.frames.getOrNull(selectedFrameIndex)
        if (activeFrame != null) {
            dragX = activeFrame.dx
            dragY = activeFrame.dy
        }
    }

    // Animation playback coroutine loop
    LaunchedEffect(isPlaying, currentAnimIndex, currentAnim.frames.size, currentAnim.isLoop) {
        if (isPlaying && currentAnim.frames.isNotEmpty()) {
            while (isPlaying) {
                val frame = currentAnim.frames.getOrNull(playingFrameIdx) ?: currentAnim.frames.first()
                delay(frame.durationMs.coerceAtLeast(50L))
                if (playingFrameIdx + 1 < currentAnim.frames.size) {
                    playingFrameIdx++
                } else {
                    if (currentAnim.isLoop) {
                        playingFrameIdx = 0
                    } else {
                        isPlaying = false
                        playingFrameIdx = 0
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                gameObject.animations.clear()
                                gameObject.animations.addAll(animations)
                                onSave(gameObject)
                                onBack()
                            },
                            modifier = Modifier.testTag("animation_editor_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎬 Editor de Animaciones: ${gameObject.name}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            gameObject.animations.clear()
                            gameObject.animations.addAll(animations)
                            if (currentAnim.name.isNotBlank()) {
                                gameObject.currentAnimationName = currentAnim.name
                            }
                            onSave(gameObject)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_animation_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("GUARDAR Y VOLVER", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color(0xFF0B0F19)
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // LEFT PANEL: List of Animations
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .border(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ANIMACIONES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Cyan
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(animations.size) { idx ->
                            val anim = animations.getOrNull(idx) ?: return@items
                            val isSelected = idx == currentAnimIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) Color(0xFF2563EB) else Color(0xFF0F172A),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        currentAnimIndex = idx
                                        selectedFrameIndex = 0
                                        isPlaying = false
                                        playingFrameIdx = 0
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = anim.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )

                                if (animations.size > 1) {
                                    IconButton(
                                        onClick = {
                                            animations.removeAt(idx)
                                            currentAnimIndex = (currentAnimIndex - 1).coerceAtLeast(0)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showNewAnimDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ NUEVA ANIMACIÓN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Divider(color = Color(0xFF334155))

                    // Animation Loop Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bucle Infinito", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Repeat Frames", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(
                            checked = currentAnim.isLoop,
                            onCheckedChange = { currentAnim.isLoop = it }
                        )
                    }
                }
            }

            // CENTER & RIGHT CONTENT AREA
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Main Stage and Inspector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // CENTER: Animation Preview Stage
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF020617))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Observe recomposeTrigger to dynamically update preview
                        val trigger = recomposeTrigger

                        val activeFrame = if (isPlaying) {
                            currentAnim.frames.getOrNull(playingFrameIdx) ?: AnimationFrame()
                        } else {
                            currentAnim.frames.getOrNull(selectedFrameIndex) ?: AnimationFrame()
                        }

                        // Preview Grid and Frame Display Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Canvas crosshair lines & Origin point
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f

                                // Grid lines
                                drawLine(Color(0xFF334155), Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = 1f)
                                drawLine(Color(0xFF334155), Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 1f)

                                // Frame Displacement Motion Vector Arrow
                                if (!isPlaying && (dragX != 0f || dragY != 0f)) {
                                    val endX = centerX + dragX * 2f
                                    val endY = centerY + dragY * 2f
                                    drawLine(
                                        color = Color.Yellow,
                                        start = Offset(centerX, centerY),
                                        end = Offset(endX, endY),
                                        strokeWidth = 3f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                                    )
                                }

                                // Centered Collision Box outline
                                if (activeFrame.centerCollision) {
                                    val objW = gameObject.width * activeFrame.scale * 2f
                                    val objH = gameObject.height * activeFrame.scale * 2f
                                    val colX = centerX + (dragX * 2f) - (objW / 2f)
                                    val colY = centerY + (dragY * 2f) - (objH / 2f)

                                    drawRoundRect(
                                        color = Color(0xFFEF4444),
                                        topLeft = Offset(colX, colY),
                                        size = Size(objW, objH),
                                        cornerRadius = CornerRadius(4f, 4f),
                                        style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                                    )
                                }
                            }

                            // Object Sprite Render in frame position (DRAGGABLE!)
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (dragX * 2f).dp,
                                        y = (dragY * 2f).dp
                                    )
                                    .size(
                                        width = (gameObject.width * activeFrame.scale * 2f).dp,
                                        height = (gameObject.height * activeFrame.scale * 2f).dp
                                    )
                                    .border(2.dp, Color.Cyan, RoundedCornerShape(4.dp))
                                    .pointerInput(selectedFrameIndex, currentAnimIndex) {
                                        detectDragGestures(
                                            onDragStart = { isPlaying = false },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragX += dragAmount.x / 2f
                                                dragY += dragAmount.y / 2f
                                                activeFrame.dx = dragX
                                                activeFrame.dy = dragY
                                                recomposeTrigger++ // Update other components
                                            }
                                        )
                                    }
                            ) {
                                val preset = activeFrame.customSpritePreset ?: gameObject.spritePreset
                                val imageUri = activeFrame.customImageUri ?: gameObject.imageUri
                                SpriteRenderer(
                                    preset = preset,
                                    imageUri = imageUri,
                                    colorHex = gameObject.colorHex,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Playback Controls Overlay
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                                    .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        isPlaying = !isPlaying
                                        if (isPlaying) playingFrameIdx = selectedFrameIndex
                                    }
                                ) {
                                    Icon(
                                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir",
                                        tint = Color.Cyan
                                    )
                                }

                                Text(
                                    text = if (isPlaying) "REPRODUCIENDO FRAME #${playingFrameIdx + 1}" else "FRAME SELECCIONADO #${selectedFrameIndex + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(
                                    onClick = {
                                        isPlaying = false
                                        playingFrameIdx = 0
                                        selectedFrameIndex = 0
                                    }
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Detener", tint = Color.Red)
                                }
                            }

                            // Touch-Drag Guide Indicator overlay (top of the canvas)
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.95f), RoundedCornerShape(20.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "¡ARRAS-TRA EL OBJETO PARA COLOCARLO!",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Scrubbing Slider (slide/scrub through frames)
                            if (currentAnim.frames.size > 1) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 70.dp)
                                        .fillMaxWidth(0.85f)
                                        .background(Color(0xFF1E293B).copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎚️ DESLIZA PARA REPRODUCIR FRAMES",
                                            color = Color.Cyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "Frame ${selectedFrameIndex + 1} / ${currentAnim.frames.size}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Slider(
                                        value = selectedFrameIndex.toFloat(),
                                        onValueChange = {
                                            isPlaying = false
                                            selectedFrameIndex = it.toInt().coerceIn(0, currentAnim.frames.size - 1)
                                        },
                                        valueRange = 0f..(currentAnim.frames.size - 1).toFloat(),
                                        steps = if (currentAnim.frames.size > 2) currentAnim.frames.size - 2 else 0,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.Cyan,
                                            activeTrackColor = Color.Cyan,
                                            inactiveTrackColor = Color(0xFF334155)
                                        ),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // RIGHT PANEL: Frame Inspector
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .border(1.dp, Color(0xFF334155))
                    ) {
                        val activeFrame = currentAnim.frames.getOrNull(selectedFrameIndex)
                        if (activeFrame != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "PROPIEDADES DEL FRAME #${selectedFrameIndex + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Cyan
                                )

                                Divider(color = Color(0xFF334155))

                                // BIG PROMINENT SPRITE/IMAGE SELECTOR (Now at the very top of properties!)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "🖼️ IMAGEN / SPRITE DEL FRAME",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Cyan
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val framePreset = activeFrame.customSpritePreset ?: gameObject.spritePreset
                                                val frameImage = activeFrame.customImageUri ?: gameObject.imageUri
                                                SpriteRenderer(
                                                    preset = framePreset,
                                                    imageUri = frameImage,
                                                    colorHex = gameObject.colorHex,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (activeFrame.customImageUri != null) "Personalizada" else if (activeFrame.customSpritePreset != null) "Preset: ${activeFrame.customSpritePreset}" else "Por defecto (Objeto)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (activeFrame.customImageUri != null || activeFrame.customSpritePreset != null) Color.Cyan else Color.LightGray
                                                )
                                                Text(
                                                    text = if (activeFrame.customImageUri != null) "Imagen subida" else if (activeFrame.customSpritePreset != null) "Sprite de preset" else "Sprite base",
                                                    fontSize = 9.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        // PRESET SELECTOR CHIPS ROW!
                                        Text(
                                            text = "Elegir Sprite rápido:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.LightGray
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val presets = listOf("BOX", "PLAYER", "ENEMY", "PLATFORM", "COIN", "SPACESHIP", "HEART", "BULLET")
                                            items(presets) { p ->
                                                val isSelected = activeFrame.customSpritePreset == p
                                                Button(
                                                    onClick = {
                                                        activeFrame.customSpritePreset = p
                                                        activeFrame.customImageUri = null
                                                        recomposeTrigger++
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSelected) Color.Cyan else Color(0xFF1E293B)
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Text(
                                                        text = p,
                                                        fontSize = 9.sp,
                                                        color = if (isSelected) Color.Black else Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = { showFrameMediaLibraryDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f).height(32.dp).testTag("select_frame_image_button")
                                            ) {
                                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Biblioteca", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (activeFrame.customImageUri != null || activeFrame.customSpritePreset != null) {
                                                Button(
                                                    onClick = {
                                                        activeFrame.customImageUri = null
                                                        activeFrame.customSpritePreset = null
                                                        recomposeTrigger++
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                Divider(color = Color(0xFF334155))

                                // DX (Horizontal Motion Offset)
                                Text("Desplazamiento X (dx): ${activeFrame.dx.toInt()} px", fontSize = 12.sp, color = Color.White)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { activeFrame.dx -= 10f; recomposeTrigger++ },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("-10", fontSize = 10.sp) }

                                    Button(
                                        onClick = { activeFrame.dx -= 1f; recomposeTrigger++ },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("-1", fontSize = 10.sp) }

                                    Button(
                                        onClick = { activeFrame.dx = 0f; recomposeTrigger++ },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("0", fontSize = 10.sp) }

                                    Button(
                                        onClick = { activeFrame.dx += 1f; recomposeTrigger++ },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("+1", fontSize = 10.sp) }

                                    Button(
                                        onClick = { activeFrame.dx += 10f; recomposeTrigger++ },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("+10", fontSize = 10.sp) }
                                }

                                // DY (Vertical Motion Offset)
                                Text("Desplazamiento Y (dy): ${activeFrame.dy.toInt()} px", fontSize = 12.sp, color = Color.White)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { activeFrame.dy -= 10f; recomposeTrigger++ },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("-10", fontSize = 10.sp) }

                                    Button(
                                        onClick = { activeFrame.dy -= 1f; recomposeTrigger++ },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("-1", fontSize = 10.sp) }

                                    Button(
                                        onClick = { activeFrame.dy = 0f; recomposeTrigger++ },
                                        modifier = Modifier.weight(1f).height(30.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text("0", fontSize = 10.sp) }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Duplicate & Delete Frame buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            val dup = activeFrame.copy(id = java.util.UUID.randomUUID().toString())
                                            currentAnim.frames.add(selectedFrameIndex + 1, dup)
                                            selectedFrameIndex++
                                            recomposeTrigger++
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Duplicar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (currentAnim.frames.size > 1) {
                                        Button(
                                            onClick = {
                                                currentAnim.frames.removeAt(selectedFrameIndex)
                                                selectedFrameIndex = (selectedFrameIndex - 1).coerceAtLeast(0)
                                                recomposeTrigger++
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Eliminar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // BOTTOM PANEL: Timeline Frame Strip
                Surface(
                    color = Color(0xFF0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.padding(end = 12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "LÍNEA DE TIEMPO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Cyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Desliza ↔",
                                fontSize = 9.sp,
                                color = Color.LightGray
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dummy = recomposeTrigger
                            items(currentAnim.frames.size) { fIdx ->
                                val frame = currentAnim.frames.getOrNull(fIdx) ?: return@items
                                val isFrameSelected = fIdx == selectedFrameIndex

                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .background(
                                            if (isFrameSelected) Color(0xFF2563EB) else Color(0xFF1E293B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            if (isFrameSelected) 2.dp else 1.dp,
                                            if (isFrameSelected) Color.Cyan else Color(0xFF475569),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedFrameIndex = fIdx
                                            isPlaying = false
                                        }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${fIdx + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${frame.durationMs}ms",
                                                fontSize = 9.sp,
                                                color = Color.LightGray
                                            )
                                        }

                                        // Frame thumbnail preview!
                                        Box(
                                            modifier = Modifier
                                                .size(45.dp)
                                                .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val framePreset = frame.customSpritePreset ?: gameObject.spritePreset
                                            val frameImage = frame.customImageUri ?: gameObject.imageUri
                                            SpriteRenderer(
                                                preset = framePreset,
                                                imageUri = frameImage,
                                                colorHex = gameObject.colorHex,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Text(
                                            text = "x:${frame.dx.toInt()} y:${frame.dy.toInt()}",
                                            fontSize = 9.sp,
                                            color = Color.Yellow
                                        )
                                    }
                                }
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .background(Color(0xFF059669), RoundedCornerShape(8.dp))
                                        .clickable {
                                            currentAnim.frames.add(
                                                AnimationFrame(
                                                    dx = ((currentAnim.frames.size + 1) * 10).toFloat(),
                                                    dy = 0f,
                                                    durationMs = 200L,
                                                    centerCollision = true
                                                )
                                            )
                                            selectedFrameIndex = currentAnim.frames.size - 1
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("+ Frame", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (showNewAnimDialog) {
        AlertDialog(
            onDismissRequest = { showNewAnimDialog = false },
            title = { Text("Crear Nueva Animación", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Nombre de la animación (Ej: Run, Jump, Attack, Idle):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAnimName,
                        onValueChange = { newAnimName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAnimName.isNotBlank()) {
                            animations.add(
                                GameAnimation(
                                    name = newAnimName.trim(),
                                    isLoop = true,
                                    frames = mutableListOf(AnimationFrame())
                                )
                            )
                            currentAnimIndex = animations.size - 1
                            selectedFrameIndex = 0
                            newAnimName = ""
                            showNewAnimDialog = false
                        }
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAnimDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Media Library integration inside the Animation Editor
    if (showFrameMediaLibraryDialog) {
        MediaLibraryDialog(
            project = project,
            activeObject = gameObject, // Passes object to ensure "Usar Sprite" button is rendered
            onDismiss = { showFrameMediaLibraryDialog = false },
            onSelectAssetForObject = { asset ->
                val activeFrame = currentAnim.frames.getOrNull(selectedFrameIndex)
                if (activeFrame != null) {
                    activeFrame.customImageUri = asset.uri
                    activeFrame.customSpritePreset = "CUSTOM_IMAGE"
                    recomposeTrigger++
                }
            },
            onProjectUpdated = {
                // Media uploads or changes
            }
        )
    }
}
