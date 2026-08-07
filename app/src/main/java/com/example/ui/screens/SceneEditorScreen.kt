package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameObject
import com.example.model.GameProject
import com.example.model.GameScene
import com.example.model.GameUIButton
import com.example.model.GameVariable
import com.example.ui.components.SpriteRenderer
import kotlin.math.roundToInt

@Composable
fun DraggableGameObject(
    obj: GameObject,
    isSelected: Boolean,
    panX: Float,
    panY: Float,
    onSelect: () -> Unit,
    onUpdate: (GameObject) -> Unit
) {
    var posX by remember(obj.id) { mutableFloatStateOf(obj.x) }
    var posY by remember(obj.id) { mutableFloatStateOf(obj.y) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(obj.x, obj.y) {
        if (!isDragging) {
            posX = obj.x
            posY = obj.y
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((panX + posX).roundToInt(), (panY + posY).roundToInt()) }
            .size(obj.width.dp, obj.height.dp)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Cyan else Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(4.dp)
            )
            .pointerInput(obj.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onSelect()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        posX += dragAmount.x
                        posY += dragAmount.y
                        obj.x = posX
                        obj.y = posY
                    },
                    onDragEnd = {
                        isDragging = false
                        obj.x = posX
                        obj.y = posY
                        onUpdate(obj)
                    },
                    onDragCancel = {
                        isDragging = false
                        obj.x = posX
                        obj.y = posY
                        onUpdate(obj)
                    }
                )
            }
            .clickable {
                onSelect()
            }
    ) {
        SpriteRenderer(
            preset = obj.spritePreset,
            imageUri = obj.imageUri,
            colorHex = obj.colorHex,
            modifier = Modifier.fillMaxSize()
        )

        Text(
            text = obj.name,
            fontSize = 10.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun DraggableUIButton(
    btn: GameUIButton,
    isSelected: Boolean,
    panX: Float,
    panY: Float,
    onSelect: () -> Unit,
    onUpdate: (GameUIButton) -> Unit
) {
    var posX by remember(btn.id) { mutableFloatStateOf(btn.x) }
    var posY by remember(btn.id) { mutableFloatStateOf(btn.y) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(btn.x, btn.y) {
        if (!isDragging) {
            posX = btn.x
            posY = btn.y
        }
    }

    Box(
        modifier = Modifier
            .offset { IntOffset((panX + posX).roundToInt(), (panY + posY).roundToInt()) }
            .size(btn.width.dp, btn.height.dp)
            .background(
                color = try { Color(android.graphics.Color.parseColor(btn.colorHex)) } catch (e: Exception) { Color(0xFFFF9800) },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                color = if (isSelected) Color.Yellow else Color.Black,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(btn.id) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        onSelect()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        posX += dragAmount.x
                        posY += dragAmount.y
                        btn.x = posX
                        btn.y = posY
                    },
                    onDragEnd = {
                        isDragging = false
                        btn.x = posX
                        btn.y = posY
                        onUpdate(btn)
                    },
                    onDragCancel = {
                        isDragging = false
                        btn.x = posX
                        btn.y = posY
                        onUpdate(btn)
                    }
                )
            }
            .clickable {
                onSelect()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = btn.label,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp
        )
    }
}

@Composable
fun SceneEditorScreen(
    project: GameProject,
    currentScene: GameScene,
    selectedObjectId: String?,
    selectedButtonId: String?,
    onSelectObject: (String?) -> Unit,
    onSelectButton: (String?) -> Unit,
    onAddObject: () -> Unit,
    onAddUIButton: () -> Unit,
    onUpdateObject: (GameObject) -> Unit,
    onUpdateUIButton: (GameUIButton) -> Unit,
    onDeleteObject: (String) -> Unit,
    onDeleteUIButton: (String) -> Unit,
    onOpenBlueprint: (GameObject) -> Unit,
    onOpenButtonBlueprint: (GameUIButton) -> Unit,
    onPlayTest: () -> Unit,
    onOpenExport: () -> Unit,
    onSwitchScene: (String) -> Unit,
    onCreateScene: (String) -> Unit,
    onCloneScene: (GameScene) -> Unit,
    onDeleteScene: (GameScene) -> Unit,
    onBackToHome: () -> Unit
) {
    var showGrid by remember { mutableStateOf(true) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var sceneDropdownExpanded by remember { mutableStateOf(false) }
    var showNewSceneDialog by remember { mutableStateOf(false) }
    var newSceneName by remember { mutableStateOf("") }
    var showAddVarDialog by remember { mutableStateOf(false) }
    var newVarName by remember { mutableStateOf("") }
    var newVarValue by remember { mutableStateOf("0") }

    // Image Picker for object sprite import matching wireframe #1 IMPORT
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val activeObj = currentScene.objects.find { o -> o.id == selectedObjectId }
            if (activeObj != null) {
                activeObj.imageUri = it.toString()
                activeObj.spritePreset = "CUSTOM_IMAGE"
                onUpdateObject(activeObj)
            }
        }
    }

    val activeObject = currentScene.objects.find { it.id == selectedObjectId }
    val activeButton = currentScene.uiButtons.find { it.id == selectedButtonId }

    // Main 2D Scene Canvas Layout matching Wireframe #1
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Left & Center viewport column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Top Toolbar matching Wireframe #1 [Play] [Scene Dropdown] [Export]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    // PLAY / TEST button matching Wireframe #1 top left play button
                    Button(
                        onClick = onPlayTest,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("play_test_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PLAY", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Scenes Dropdown
                    Box {
                        Button(
                            onClick = { sceneDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(currentScene.name.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("▼", color = Color.LightGray, fontSize = 10.sp)
                        }

                        DropdownMenu(
                            expanded = sceneDropdownExpanded,
                            onDismissRequest = { sceneDropdownExpanded = false }
                        ) {
                            project.scenes.forEach { sc ->
                                DropdownMenuItem(
                                    text = { Text(sc.name) },
                                    onClick = {
                                        onSwitchScene(sc.id)
                                        sceneDropdownExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create Scene", color = Color(0xFF10B981)) },
                                onClick = {
                                    sceneDropdownExpanded = false
                                    showNewSceneDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📋 Clone Current Scene", color = Color.Cyan) },
                                onClick = {
                                    sceneDropdownExpanded = false
                                    onCloneScene(currentScene)
                                }
                            )
                            if (project.scenes.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("❌ Delete Scene", color = Color(0xFFEF4444)) },
                                    onClick = {
                                        sceneDropdownExpanded = false
                                        onDeleteScene(currentScene)
                                    }
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Grid toggle
                    IconButton(onClick = { showGrid = !showGrid }) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Grid",
                            tint = if (showGrid) Color.Cyan else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // EXPORT GAME button matching wireframe #1 & wireframe #4 export launcher
                    Button(
                        onClick = onOpenExport,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("open_export_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 2D Scene Canvas Viewport matching Wireframe #1 center gray area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF020617))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panX += dragAmount.x
                            panY += dragAmount.y
                        }
                    }
                    .clickable {
                        onSelectObject(null)
                        onSelectButton(null)
                    }
            ) {
                // Background Color / Grid
                val canvasBg = try { Color(android.graphics.Color.parseColor(currentScene.backgroundColorHex)) } catch (e: Exception) { Color(0xFF020617) }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(canvasBg)
                    if (showGrid) {
                        val gridSize = 40f
                        val startX = (panX % gridSize)
                        val startY = (panY % gridSize)
                        var x = startX
                        while (x < size.width) {
                            if (x >= 0) drawLine(Color(0xFF1E293B), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                            x += gridSize
                        }
                        var y = startY
                        while (y < size.height) {
                            if (y >= 0) drawLine(Color(0xFF1E293B), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                            y += gridSize
                        }
                    }
                }

                // Render GameObjects on Scene Canvas
                currentScene.objects.forEach { obj ->
                    DraggableGameObject(
                        obj = obj,
                        isSelected = (obj.id == selectedObjectId),
                        panX = panX,
                        panY = panY,
                        onSelect = {
                            onSelectObject(obj.id)
                            onSelectButton(null)
                        },
                        onUpdate = onUpdateObject
                    )
                }

                // Render UI Buttons on Scene Canvas
                currentScene.uiButtons.forEach { btn ->
                    DraggableUIButton(
                        btn = btn,
                        isSelected = (btn.id == selectedButtonId),
                        panX = panX,
                        panY = panY,
                        onSelect = {
                            onSelectButton(btn.id)
                            onSelectObject(null)
                        },
                        onUpdate = onUpdateUIButton
                    )
                }

                // Reset / Recenter Camera view overlay button
                if (panX != 0f || panY != 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .clickable {
                                panX = 0f
                                panY = 0f
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🎯 Recenter View", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Bottom Action Bar matching Wireframe #1 [+ OBJ] [+ BTN]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .background(Color(0xFF0F172A))
                    .border(2.dp, Color.Black)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onAddObject,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
                        .testTag("add_object_button")
                ) {
                    Text("OBJ", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                }

                Button(
                    onClick = onAddUIButton,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
                        .testTag("add_ui_button")
                ) {
                    Text("BTN", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        // Right Inspector Panel matching Wireframe #1 (EVENT, 2 BLOX, BLOCKS EDITOR, IMPORT, VAR)
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(Color(0xFF1E293B))
                .border(2.dp, Color.Black)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "INSPECTOR",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White
            )

            if (activeObject != null) {
                // Object Header & Name
                OutlinedTextField(
                    value = activeObject.name,
                    onValueChange = {
                        activeObject.name = it
                        onUpdateObject(activeObject)
                    },
                    label = { Text("Object Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tag Input
                OutlinedTextField(
                    value = activeObject.tag,
                    onValueChange = {
                        activeObject.tag = it
                        onUpdateObject(activeObject)
                    },
                    label = { Text("Tag (e.g. Player, Platform, Enemy)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                // Event & Blocks count badge matching wireframe #1 [EVENT] [2 BLOX]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF334155), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("BLUEPRINT LOGIC", fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                    Text(
                        "${activeObject.blueprintNodes.size} BLOX",
                        fontSize = 12.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Black
                    )
                }

                // BLOCKS EDITOR Button matching Wireframe #1 button [-> BLOCKS EDITOR]
                Button(
                    onClick = { onOpenBlueprint(activeObject) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
                        .testTag("open_blocks_editor_button")
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BLOCKS EDITOR", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }

                // Image / Sprite Inspector with IMPORT button matching Wireframe #1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                        .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SpriteRenderer(
                            preset = activeObject.spritePreset,
                            imageUri = activeObject.imageUri,
                            colorHex = activeObject.colorHex,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("import_image_button")
                        ) {
                            Text("IMPORT IMAGE", fontSize = 10.sp, color = Color.Cyan)
                        }
                    }
                }

                // Sprite Preset picker chips
                Text("Sprite Preset:", fontSize = 12.sp, color = Color.LightGray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf("BOX", "PLAYER", "ENEMY", "PLATFORM", "COIN", "SPACESHIP", "HEART")
                    items(presets) { p ->
                        Button(
                            onClick = {
                                activeObject.spritePreset = p
                                activeObject.imageUri = null
                                onUpdateObject(activeObject)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeObject.spritePreset == p) Color.Cyan else Color(0xFF334155)
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(p, fontSize = 10.sp, color = if (activeObject.spritePreset == p) Color.Black else Color.White)
                        }
                    }
                }

                // Physics & Gravity toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Physics / Gravity:", fontSize = 12.sp, color = Color.White)
                    Switch(
                        checked = activeObject.hasPhysics,
                        onCheckedChange = {
                            activeObject.hasPhysics = it
                            onUpdateObject(activeObject)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Static Solid (Platform):", fontSize = 12.sp, color = Color.White)
                    Switch(
                        checked = activeObject.isStatic,
                        onCheckedChange = {
                            activeObject.isStatic = it
                            onUpdateObject(activeObject)
                        }
                    )
                }

                // Position Inputs: X, Y, W, H
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activeObject.x.toInt().toString(),
                        onValueChange = {
                            activeObject.x = it.toFloatOrNull() ?: activeObject.x
                            onUpdateObject(activeObject)
                        },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeObject.y.toInt().toString(),
                        onValueChange = {
                            activeObject.y = it.toFloatOrNull() ?: activeObject.y
                            onUpdateObject(activeObject)
                        },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activeObject.width.toInt().toString(),
                        onValueChange = {
                            activeObject.width = it.toFloatOrNull() ?: activeObject.width
                            onUpdateObject(activeObject)
                        },
                        label = { Text("Width") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = activeObject.height.toInt().toString(),
                        onValueChange = {
                            activeObject.height = it.toFloatOrNull() ?: activeObject.height
                            onUpdateObject(activeObject)
                        },
                        label = { Text("Height") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // VAR Panel matching Wireframe #1 VAR [0.01]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF334155), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VAR (Variables)", fontWeight = FontWeight.Bold, color = Color.White)
                            IconButton(onClick = { showAddVarDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Cyan)
                            }
                        }

                        activeObject.variables.forEach { v ->
                            Text("${v.name} = ${v.value}", fontSize = 12.sp, color = Color.Yellow)
                        }
                    }
                }

                // Delete Object Button
                Button(
                    onClick = { onDeleteObject(activeObject.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_object_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Object")
                }

            } else if (activeButton != null) {
                // UI Button Inspector
                OutlinedTextField(
                    value = activeButton.name,
                    onValueChange = {
                        activeButton.name = it
                        onUpdateUIButton(activeButton)
                    },
                    label = { Text("Button Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = activeButton.label,
                    onValueChange = {
                        activeButton.label = it
                        onUpdateUIButton(activeButton)
                    },
                    label = { Text("Display Label") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = activeButton.actionKey,
                    onValueChange = {
                        activeButton.actionKey = it
                        onUpdateUIButton(activeButton)
                    },
                    label = { Text("Action Key (e.g. JUMP, LEFT, SHOOT)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onDeleteUIButton(activeButton.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Button")
                }
            } else {
                // Nothing selected state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap any Object or Button on the canvas to inspect & edit properties",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Add Variable Dialog
    if (showAddVarDialog && activeObject != null) {
        AlertDialog(
            onDismissRequest = { showAddVarDialog = false },
            title = { Text("Add Variable to ${activeObject.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newVarName,
                        onValueChange = { newVarName = it },
                        label = { Text("Variable Name (e.g. speed, health)") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newVarValue,
                        onValueChange = { newVarValue = it },
                        label = { Text("Initial Value") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newVarName.isNotBlank()) {
                        activeObject.variables.add(GameVariable(name = newVarName, value = newVarValue))
                        onUpdateObject(activeObject)
                        showAddVarDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVarDialog = false }) { Text("Cancel") }
            }
        )
    }

    // New Scene Dialog
    if (showNewSceneDialog) {
        AlertDialog(
            onDismissRequest = { showNewSceneDialog = false },
            title = { Text("Create New Scene") },
            text = {
                OutlinedTextField(
                    value = newSceneName,
                    onValueChange = { newSceneName = it },
                    label = { Text("Scene Name (e.g. Level 2, Boss Stage)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newSceneName.isNotBlank()) {
                        onCreateScene(newSceneName)
                        showNewSceneDialog = false
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSceneDialog = false }) { Text("Cancel") }
            }
        )
    }
}
