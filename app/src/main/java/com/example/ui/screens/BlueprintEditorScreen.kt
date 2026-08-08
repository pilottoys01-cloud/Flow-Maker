package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlueprintConnection
import com.example.model.BlueprintNode
import com.example.model.GameObject
import com.example.model.NodeCategory
import kotlin.math.roundToInt

@Composable
fun DropdownFieldSelector(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    labelColor: Color = Color.LightGray
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(text = label, fontSize = 9.sp, color = labelColor, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF334155), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF475569), RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (value.isEmpty()) "None" else value,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "▼",
                    color = Color.LightGray,
                    fontSize = 8.sp
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E293B))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DraggableBlueprintNode(
    node: BlueprintNode,
    isSelected: Boolean,
    isWiringSource: Boolean,
    connectingPortNodeId: String?,
    panX: Float,
    panY: Float,
    zoomScale: Float = 1.0f,
    allObjectsInScene: List<GameObject> = emptyList(),
    allScenesInProject: List<com.example.model.GameScene> = emptyList(),
    onSelectNode: () -> Unit,
    onConnectNode: () -> Unit,
    onStartWiring: () -> Unit,
    onSaveLogic: () -> Unit
) {
    var posX by remember(node.id) { mutableFloatStateOf(node.x) }
    var posY by remember(node.id) { mutableFloatStateOf(node.y) }
    var isDragging by remember { mutableStateOf(false) }

    val actorOptions = remember(allObjectsInScene) {
        val list = mutableListOf("Self")
        allObjectsInScene.forEach { obj ->
            if (obj.tag.isNotEmpty() && !list.contains(obj.tag)) {
                list.add(obj.tag)
            }
            if (obj.name.isNotEmpty() && !list.contains(obj.name)) {
                list.add(obj.name)
            }
        }
        list.toList()
    }

    val parentOptions = remember(allObjectsInScene) {
        val list = mutableListOf("None")
        allObjectsInScene.forEach { obj ->
            if (obj.tag.isNotEmpty() && !list.contains(obj.tag)) {
                list.add(obj.tag)
            }
            if (obj.name.isNotEmpty() && !list.contains(obj.name)) {
                list.add(obj.name)
            }
        }
        list.toList()
    }

    val sceneOptions = remember(allScenesInProject) {
        allScenesInProject.map { it.name }
    }

    val soundOptions = listOf("COIN", "JUMP", "SHOOT", "LASER", "WIN", "HIT", "EXPLOSION", "MUSIC", "MELODY")
    val spriteOptions = listOf("BOX", "PLAYER", "ENEMY", "PLATFORM", "COIN", "SPACESHIP", "HEART", "BULLET")
    val animOptions = remember(allObjectsInScene, node) {
        val list = mutableListOf("Walk", "Run", "Jump", "Idle", "Attack", "Death", "Shoot")
        allObjectsInScene.forEach { obj ->
            obj.animations.forEach { anim ->
                if (anim.name.isNotEmpty() && !list.contains(anim.name)) {
                    list.add(anim.name)
                }
            }
        }
        list
    }

    LaunchedEffect(node.x, node.y) {
        if (!isDragging) {
            posX = node.x
            posY = node.y
        }
    }

    val catColor = try { Color(android.graphics.Color.parseColor(node.category.colorHex)) } catch (e: Exception) { Color(0xFF2563EB) }

    Box(
        modifier = Modifier
            .offset { IntOffset(((panX + posX) * zoomScale).roundToInt(), ((panY + posY) * zoomScale).roundToInt()) }
            .width((180 * zoomScale).dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected || isWiringSource) 3.dp else 2.dp,
                color = if (isWiringSource) Color.Red else if (isSelected) Color.Yellow else Color.Black,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(node.id, zoomScale) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        if (connectingPortNodeId != null && connectingPortNodeId != node.id) {
                            onConnectNode()
                        } else {
                            onSelectNode()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        posX += dragAmount.x / zoomScale
                        posY += dragAmount.y / zoomScale
                        node.x = posX
                        node.y = posY
                    },
                    onDragEnd = {
                        isDragging = false
                        node.x = posX
                        node.y = posY
                        onSaveLogic()
                    },
                    onDragCancel = {
                        isDragging = false
                        node.x = posX
                        node.y = posY
                        onSaveLogic()
                    }
                )
            }
            .clickable {
                if (connectingPortNodeId != null && connectingPortNodeId != node.id) {
                    onConnectNode()
                } else {
                    onSelectNode()
                }
            }
    ) {
        Column {
            // Node Header Box matching wireframe #2 [START] [MOVE]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(catColor)
                    .padding(8.dp)
            ) {
                Text(
                    text = node.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }

            // Node Body & Parameters
            Column(modifier = Modifier.padding(8.dp)) {
                // Dedicated target selector showing which object reproduces/targets this block
                val targetActor = node.params["targetActor"] ?: "Self"
                DropdownFieldSelector(
                    label = "🎯 Actor / Objeto Objetivo (Tag)",
                    value = targetActor,
                    options = actorOptions,
                    onValueChange = { newVal ->
                        node.params["targetActor"] = newVal
                        onSaveLogic()
                    },
                    labelColor = Color(0xFF34D399)
                )

                node.params.filter { it.key != "targetActor" }.forEach { (paramKey, paramVal) ->
                    when (paramKey) {
                        "targetTag" -> {
                            DropdownFieldSelector(
                                label = "🎯 targetTag (Destino/Colisión)",
                                value = paramVal,
                                options = actorOptions,
                                onValueChange = { newVal ->
                                    node.params[paramKey] = newVal
                                    onSaveLogic()
                                }
                            )
                        }
                        "parentTag" -> {
                            DropdownFieldSelector(
                                label = "🔗 parentTag (Objeto Padre)",
                                value = paramVal,
                                options = parentOptions,
                                onValueChange = { newVal ->
                                    node.params[paramKey] = if (newVal == "None") "" else newVal
                                    onSaveLogic()
                                }
                            )
                        }
                        "targetScene" -> {
                            DropdownFieldSelector(
                                label = "🎬 targetScene (Escena de destino)",
                                value = paramVal,
                                options = sceneOptions,
                                onValueChange = { newVal ->
                                    node.params[paramKey] = newVal
                                    onSaveLogic()
                                }
                            )
                        }
                        "soundType" -> {
                            DropdownFieldSelector(
                                label = "🔊 soundType (Audio Retro)",
                                value = paramVal,
                                options = soundOptions,
                                onValueChange = { newVal ->
                                    node.params[paramKey] = newVal
                                    onSaveLogic()
                                }
                            )
                        }
                        "sprite" -> {
                            DropdownFieldSelector(
                                label = "👾 sprite (Diseño / Textura)",
                                value = paramVal,
                                options = spriteOptions,
                                onValueChange = { newVal ->
                                    node.params[paramKey] = newVal
                                    onSaveLogic()
                                }
                            )
                        }
                        "animName" -> {
                            DropdownFieldSelector(
                                label = "🎬 animName (Nombre Animación)",
                                value = paramVal,
                                options = animOptions,
                                onValueChange = { newVal ->
                                    node.params[paramKey] = newVal
                                    onSaveLogic()
                                }
                            )
                        }
                        else -> {
                            var paramTextState by remember(node.id, paramKey) { mutableStateOf(paramVal) }
                            OutlinedTextField(
                                value = paramTextState,
                                onValueChange = { newVal ->
                                    paramTextState = newVal
                                    node.params[paramKey] = newVal
                                    onSaveLogic()
                                },
                                label = { Text(paramKey, fontSize = 9.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Port Connectors Row (Green IN wire port & Red OUT wire port)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (connectingPortNodeId != null && connectingPortNodeId != node.id) Color(0xFF10B981) else Color(0xFF475569))
                            .clickable {
                                if (connectingPortNodeId != null && connectingPortNodeId != node.id) {
                                    onConnectNode()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("IN", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    // Wire output dot matching red circle in wireframe #2
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isWiringSource) Color.Yellow else Color(0xFFEF4444))
                            .clickable {
                                onStartWiring()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OUT", fontSize = 8.sp, color = if (isWiringSource) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BlueprintEditorScreen(
    gameObject: GameObject,
    allObjectsInScene: List<GameObject> = emptyList(),
    allScenesInProject: List<com.example.model.GameScene> = emptyList(),
    onBack: () -> Unit,
    onSaveLogic: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(NodeCategory.START) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var connectingPortNodeId by remember { mutableStateOf<String?>(null) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var customCategories by remember { mutableStateOf(mutableListOf<String>()) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }

    val nodes = gameObject.blueprintNodes
    val connections = gameObject.blueprintConnections

    // Blueprint logic node palette options per category
    val blockTemplates = when (selectedCategory) {
        NodeCategory.START -> listOf(
            Triple("ON GAME START", "ON_START", mutableMapOf<String, String>()),
            Triple("EVERY FRAME", "EVERY_FRAME", mutableMapOf<String, String>())
        )
        NodeCategory.EVENT -> listOf(
            Triple("ON CLICK / TOUCH", "ON_CLICK", mutableMapOf<String, String>()),
            Triple("ON COLLISION WITH", "ON_COLLISION", mutableMapOf("targetTag" to "Enemy")),
            Triple("PLAY RETRO SOUND", "PLAY_SOUND", mutableMapOf("soundType" to "COIN"))
        )
        NodeCategory.ESTADOS -> listOf(
            Triple("EJECUTAR ANIMACIÓN", "PLAY_ANIMATION", mutableMapOf("animName" to "Walk")),
            Triple("AL CORRER", "ON_RUN", mutableMapOf<String, String>()),
            Triple("AL SALTAR", "ON_JUMP", mutableMapOf<String, String>()),
            Triple("AL ATACAR", "ON_ATTACK", mutableMapOf<String, String>()),
            Triple("AL ESTAR QUIETO", "ON_IDLE", mutableMapOf<String, String>())
        )
        NodeCategory.CONTROL -> listOf(
            Triple("EJECUTAR ANIMACIÓN", "PLAY_ANIMATION", mutableMapOf("animName" to "Walk")),
            Triple("ESPERAR X SEGUNDOS", "WAIT", mutableMapOf("seconds" to "1.0"))
        )
        NodeCategory.MOVE -> listOf(
            Triple("MOVE X/Y", "MOVE_XY", mutableMapOf("dx" to "10", "dy" to "0")),
            Triple("SET VELOCITY (VX/VY)", "SET_VELOCITY", mutableMapOf("vx" to "100", "vy" to "-200")),
            Triple("JUMP IMPULSE", "JUMP_IMPULSE", mutableMapOf("power" to "300")),
            Triple("CAMERA FOLLOW TARGET", "FOLLOW_CAMERA", mutableMapOf("targetTag" to "Player")),
            Triple("SET PARENT OBJECT", "SET_PARENT", mutableMapOf("parentTag" to "Player"))
        )
        NodeCategory.PLATF -> listOf(
            Triple("APPLY GRAVITY", "APPLY_GRAVITY", mutableMapOf("gravity" to "2.0")),
            Triple("PLATFORMER MOVE", "PLATFORMER_MOVE", mutableMapOf("speed" to "15"))
        )
        NodeCategory.SHOOT -> listOf(
            Triple("SHOOT BULLET", "SHOOT_BULLET", mutableMapOf("speed" to "500")),
            Triple("SPAWN BOX / OBJECT", "SPAWN_OBJECT", mutableMapOf("name" to "Box", "sprite" to "BOX", "dx" to "0", "dy" to "-40", "count" to "1", "hasPhysics" to "true", "isSolid" to "true")),
            Triple("SPAWN MULTIPLE BOXES", "SPAWN_OBJECT", mutableMapOf("name" to "Box", "sprite" to "BOX", "dx" to "0", "dy" to "-40", "count" to "3", "hasPhysics" to "true", "isSolid" to "true")),
            Triple("SPAWN COIN", "SPAWN_OBJECT", mutableMapOf("name" to "Coin", "sprite" to "COIN", "dx" to "0", "dy" to "-40", "count" to "1")),
            Triple("DESTROY SELF", "DESTROY_SELF", mutableMapOf<String, String>())
        )
        NodeCategory.VARI -> listOf(
            Triple("SET VARIABLE", "SET_VAR", mutableMapOf("varName" to "score", "value" to "10")),
            Triple("ADD TO VARIABLE", "ADD_VAR", mutableMapOf("varName" to "score", "add" to "1"))
        )
        NodeCategory.INPUT -> listOf(
            Triple("ON UI BUTTON", "ON_UI_BUTTON", mutableMapOf("buttonKey" to "JUMP"))
        )
        NodeCategory.SCENE -> listOf(
            Triple("CHANGE SCENE", "CHANGE_SCENE", mutableMapOf("targetScene" to "Level 2")),
            Triple("PLAY BG MUSIC", "PLAY_SOUND", mutableMapOf("soundType" to "MUSIC"))
        )
        else -> listOf(
            Triple("ACCIÓN PERSONALIZADA", "MOVE_XY", mutableMapOf("dx" to "10"))
        )
    }

    // Matching Wireframe #2 Blueprint Editor
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BLUEPRINT: ${gameObject.name.uppercase()}",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            // Zoom controls for Blueprint Editor
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.4f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "${(zoomScale * 100).roundToInt()}%",
                    color = Color.Cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Button(
                    onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(2.5f) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { zoomScale = 1.0f; panX = 0f; panY = 0f },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("100%", color = Color.White, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (selectedNodeId != null) {
                    IconButton(onClick = {
                        val selId = selectedNodeId
                        nodes.removeAll { it.id == selId }
                        connections.removeAll { it.fromNodeId == selId || it.toNodeId == selId }
                        selectedNodeId = null
                        onSaveLogic()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Node", tint = Color(0xFFEF4444))
                    }
                }

                Button(
                    onClick = {
                        onSaveLogic()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("save_blueprint_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DONE", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left Sidebar Categories matching Wireframe #2 (MOVE, EVENT, VARI, START, DATA, PLATF, RPG, SHOOT, INPUT)
            Row(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color.Black)
            ) {
                // Category list buttons (scrollable column)
                LazyColumn(
                    modifier = Modifier
                        .width(95.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF0F172A)),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(NodeCategory.values()) { cat ->
                        val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(if (selectedCategory == cat) catColor else Color(0xFF1E293B))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.displayName,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = if (selectedCategory == cat) Color.Black else Color.White
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color(0xFF059669))
                                .clickable { showAddCategoryDialog = true }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ NUEVA",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Node blocks template column
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blockTemplates) { (title, actionType, params) ->
                        Button(
                            onClick = {
                                val newNode = BlueprintNode(
                                    title = title,
                                    category = selectedCategory,
                                    actionType = actionType,
                                    x = 80f + (nodes.size * 20f),
                                    y = 80f + (nodes.size * 20f),
                                    params = params.toMutableMap()
                                )
                                nodes.add(newNode)
                                if (connectingPortNodeId != null) {
                                    connections.add(
                                        BlueprintConnection(
                                            fromNodeId = connectingPortNodeId!!,
                                            toNodeId = newNode.id
                                        )
                                    )
                                    connectingPortNodeId = null
                                }
                                onSaveLogic()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Black, RoundedCornerShape(6.dp))
                        ) {
                            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Canvas Area for Blueprint Nodes matching Wireframe #2 Node Canvas with Red Wire Wires
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF020617))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            panX += dragAmount.x
                            panY += dragAmount.y
                        }
                    }
            ) {
                // Background grid & wires layer (Drawing red cables between ports matching wireframe #2)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw grid
                    val gridSize = 40f
                    val gridStartX = (panX % gridSize)
                    val gridStartY = (panY % gridSize)
                    var gx = gridStartX
                    while (gx < size.width) {
                        if (gx >= 0) drawLine(Color(0xFF0F172A), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                        gx += gridSize
                    }
                    var gy = gridStartY
                    while (gy < size.height) {
                        if (gy >= 0) drawLine(Color(0xFF0F172A), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                        gy += gridSize
                    }

                    // Draw wires
                    connections.forEach { conn ->
                        val fromNode = nodes.find { it.id == conn.fromNodeId }
                        val toNode = nodes.find { it.id == conn.toNodeId }
                        if (fromNode != null && toNode != null) {
                            val startX = (panX + fromNode.x + 180f) * zoomScale
                            val startY = (panY + fromNode.y + 40f) * zoomScale
                            val endX = (panX + toNode.x) * zoomScale
                            val endY = (panY + toNode.y + 40f) * zoomScale

                            // Draw red wire curve matching wireframe #2
                            val path = Path().apply {
                                moveTo(startX, startY)
                                cubicTo(
                                    startX + 60f * zoomScale, startY,
                                    endX - 60f * zoomScale, endY,
                                    endX, endY
                                )
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFEF4444), // Red cable wire
                                style = Stroke(width = 6f * zoomScale)
                            )
                        }
                    }
                }

                // Render Nodes on Canvas
                nodes.forEach { node ->
                    DraggableBlueprintNode(
                        node = node,
                        isSelected = (node.id == selectedNodeId),
                        isWiringSource = (node.id == connectingPortNodeId),
                        connectingPortNodeId = connectingPortNodeId,
                        panX = panX,
                        panY = panY,
                        zoomScale = zoomScale,
                        allObjectsInScene = allObjectsInScene,
                        allScenesInProject = allScenesInProject,
                        onSelectNode = { selectedNodeId = node.id },
                        onConnectNode = {
                            if (connectingPortNodeId != null && connectingPortNodeId != node.id) {
                                connections.add(
                                    BlueprintConnection(
                                        fromNodeId = connectingPortNodeId!!,
                                        toNodeId = node.id
                                    )
                                )
                                connectingPortNodeId = null
                                onSaveLogic()
                            }
                        },
                        onStartWiring = { connectingPortNodeId = node.id },
                        onSaveLogic = onSaveLogic
                    )
                }

                // Reset / Recenter Camera view overlay button
                if (panX != 0f || panY != 0f || zoomScale != 1.0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .clickable {
                                panX = 0f
                                panY = 0f
                                zoomScale = 1.0f
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🎯 Reset View", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Añadir Categoría de Bloques", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Nombre de la nueva categoría:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            customCategories.add(newCatName.trim().uppercase())
                            newCatName = ""
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
