package com.example.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.model.BlueprintNode
import com.example.model.GameObject
import com.example.model.GameProject
import com.example.model.GameScene
import com.example.model.GameUIButton
import java.util.UUID
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

object RetroAudioEngine {
    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSound(type: String) {
        val gen = toneGen ?: return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (type.trim().uppercase()) {
                    "JUMP" -> {
                        gen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    }
                    "SHOOT", "LASER" -> {
                        gen.startTone(ToneGenerator.TONE_PROP_ACK, 80)
                    }
                    "COIN", "WIN" -> {
                        gen.startTone(ToneGenerator.TONE_CDMA_PIP, 120)
                        delay(100)
                        gen.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                    }
                    "HIT", "EXPLOSION" -> {
                        gen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                    }
                    "MUSIC", "MELODY" -> {
                        val notes = listOf(
                            ToneGenerator.TONE_PROP_BEEP,
                            ToneGenerator.TONE_PROP_BEEP2,
                            ToneGenerator.TONE_PROP_BEEP,
                            ToneGenerator.TONE_CDMA_PIP
                        )
                        for (note in notes) {
                            gen.startTone(note, 150)
                            delay(200)
                        }
                    }
                    else -> {
                        gen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class RuntimeObject(
    val originalId: String,
    val runtimeId: String = UUID.randomUUID().toString(),
    var name: String,
    x: Float,
    y: Float,
    vx: Float = 0f,
    vy: Float = 0f,
    var width: Float,
    var height: Float,
    var rotation: Float = 0f,
    var colorHex: String,
    var spritePreset: String,
    var imageUri: String?,
    hasPhysics: Boolean,
    var isStatic: Boolean,
    var tag: String,
    val variables: MutableMap<String, String> = mutableMapOf(),
    val blueprintNodes: List<BlueprintNode> = emptyList(),
    val blueprintConnections: List<com.example.model.BlueprintConnection> = emptyList()
) {
    var x by mutableStateOf(x)
    var y by mutableStateOf(y)
    var vx by mutableStateOf(vx)
    var vy by mutableStateOf(vy)
    var hasPhysics by mutableStateOf(hasPhysics)
    var isGrounded by mutableStateOf(false)
    var parentTag by mutableStateOf<String?>(null)
    var parentOffsetX by mutableStateOf(0f)
    var parentOffsetY by mutableStateOf(0f)
}

class GameEngine {

    var currentSceneName by mutableStateOf("Scene")
    var backgroundColorHex by mutableStateOf("#1E293B")
    val runtimeObjects = mutableStateListOf<RuntimeObject>()
    val runtimeUIButtons = mutableStateListOf<GameUIButton>()
    val globalVars = mutableStateMapOf<String, String>()

    var cameraX by mutableStateOf(0f)
    var cameraY by mutableStateOf(0f)
    var cameraFollowTag by mutableStateOf<String?>("Player")

    var activeProject: GameProject? = null
    private var isInitialized = false

    fun startScene(project: GameProject, scene: GameScene) {
        activeProject = project
        currentSceneName = scene.name
        backgroundColorHex = scene.backgroundColorHex

        runtimeObjects.clear()
        runtimeUIButtons.clear()
        globalVars.clear()

        cameraX = 0f
        cameraY = 0f
        cameraFollowTag = "Player"

        project.globalVariables.forEach { v ->
            globalVars[v.name] = v.value
        }

        scene.objects.forEach { obj ->
            val rObj = RuntimeObject(
                originalId = obj.id,
                name = obj.name,
                x = obj.x,
                y = obj.y,
                width = obj.width,
                height = obj.height,
                rotation = obj.rotation,
                colorHex = obj.colorHex,
                spritePreset = obj.spritePreset,
                imageUri = obj.imageUri,
                hasPhysics = obj.hasPhysics,
                isStatic = obj.isStatic,
                tag = obj.tag,
                blueprintNodes = obj.blueprintNodes.toList(),
                blueprintConnections = obj.blueprintConnections.toList()
            )
            obj.variables.forEach { v ->
                rObj.variables[v.name] = v.value
            }
            runtimeObjects.add(rObj)
        }

        runtimeUIButtons.addAll(scene.uiButtons)
        isInitialized = true

        // Execute ON_START nodes for all objects
        executeStartNodes()
    }

    private fun executeStartNodes() {
        runtimeObjects.toList().forEach { rObj ->
            rObj.blueprintNodes.filter { it.actionType == "ON_START" }.forEach { startNode ->
                executeConnections(rObj, startNode)
            }
        }
    }

    fun triggerUIButton(actionKey: String) {
        // Handle standard controls directly as well as custom blueprint bindings
        val keyUpper = actionKey.trim().uppercase()
        runtimeObjects.toList().forEach { rObj ->
            if (rObj.tag == "Player" || rObj.hasPhysics) {
                when (keyUpper) {
                    "JUMP" -> {
                        if (rObj.isGrounded || !rObj.hasPhysics) {
                            rObj.vy = -380f
                            rObj.isGrounded = false
                        }
                    }
                    "LEFT" -> rObj.x = (rObj.x - 18f).coerceAtLeast(0f)
                    "RIGHT" -> rObj.x += 18f
                    "UP" -> rObj.y = (rObj.y - 18f).coerceAtLeast(0f)
                    "DOWN" -> rObj.y += 18f
                    "SHOOT" -> spawnBullet(rObj)
                }
            }

            // Trigger ON_UI_BUTTON node events on objects
            rObj.blueprintNodes.filter {
                it.actionType == "ON_UI_BUTTON" && (
                    it.params["buttonKey"]?.trim()?.equals(actionKey, ignoreCase = true) == true ||
                    it.params["buttonKey"].isNullOrEmpty()
                )
            }.forEach { btnNode ->
                executeConnections(rObj, btnNode)
            }
        }
    }

    fun triggerObjectTouch(targetObj: RuntimeObject) {
        targetObj.blueprintNodes.filter { it.actionType == "ON_CLICK" || it.actionType == "ON_TOUCH" }.forEach { clickNode ->
            executeConnections(targetObj, clickNode)
        }
    }

    private fun spawnBullet(shooter: RuntimeObject, speed: Float = 500f) {
        val bullet = RuntimeObject(
            originalId = UUID.randomUUID().toString(),
            name = "Bullet",
            x = shooter.x + shooter.width / 2f - 8f,
            y = shooter.y - 15f,
            vx = 0f,
            vy = -speed,
            width = 16f,
            height = 16f,
            colorHex = "#FFEB3B",
            spritePreset = "BULLET",
            imageUri = null,
            hasPhysics = false,
            isStatic = false,
            tag = "Bullet"
        )
        runtimeObjects.add(bullet)
    }

    fun updateTick(deltaSeconds: Float) {
        if (!isInitialized) return

        val dt = deltaSeconds.coerceIn(0.001f, 0.05f)

        // Update Parent offsets first so children attach correctly to their parents
        runtimeObjects.toList().forEach { rObj ->
            val pTag = rObj.parentTag
            if (pTag != null && pTag.isNotEmpty()) {
                val parent = runtimeObjects.find {
                    it.tag.trim().equals(pTag.trim(), ignoreCase = true) ||
                    it.name.trim().equals(pTag.trim(), ignoreCase = true)
                }
                if (parent != null) {
                    rObj.x = parent.x + rObj.parentOffsetX
                    rObj.y = parent.y + rObj.parentOffsetY
                }
            }
        }

        // Physics movement and collisions
        runtimeObjects.toList().forEach { rObj ->
            if (rObj.hasPhysics && !rObj.isStatic) {
                // Apply Gravity
                rObj.vy += 800f * dt
                rObj.x += rObj.vx * dt
                rObj.y += rObj.vy * dt

                // Platform collisions
                var landed = false
                runtimeObjects.filter { it.isStatic || it.tag == "Platform" }.forEach { platform ->
                    if (checkAABBCollision(rObj, platform)) {
                        // Land on top of platform
                        if (rObj.vy > 0 && (rObj.y + rObj.height - rObj.vy * dt) <= platform.y + 15f) {
                            rObj.y = platform.y - rObj.height
                            rObj.vy = 0f
                            landed = true
                        }
                    }
                }
                rObj.isGrounded = landed
            } else if (rObj.tag == "Bullet") {
                rObj.y += rObj.vy * dt
                if (rObj.y < -1000f || rObj.y > 2000f) {
                    runtimeObjects.remove(rObj)
                }
            }

            // Execute EVERY_FRAME nodes
            rObj.blueprintNodes.filter { rObjNode -> rObjNode.actionType == "EVERY_FRAME" }.forEach { frameNode ->
                executeConnections(rObj, frameNode)
            }
        }

        // Update Camera tracking based on target object
        val followTag = cameraFollowTag
        if (followTag != null && followTag.isNotEmpty()) {
            val cameraTarget = runtimeObjects.find {
                it.tag.trim().equals(followTag.trim(), ignoreCase = true) ||
                it.name.trim().equals(followTag.trim(), ignoreCase = true)
            }
            if (cameraTarget != null) {
                // Center camera on object, assuming target viewport middle is around 180f, 300f
                cameraX = 180f - cameraTarget.x
                cameraY = 300f - cameraTarget.y
            }
        }

        // Bounding box collision detection between objects
        checkTagCollisions()
    }

    private fun checkTagCollisions() {
        val list = runtimeObjects.toList()
        for (i in list.indices) {
            for (j in i + 1 until list.size) {
                val a = list[i]
                val b = list[j]
                if (checkAABBCollision(a, b)) {
                    // Trigger ON_COLLISION nodes on A for B's tag (case-insensitive & trimmed)
                    a.blueprintNodes.filter {
                        it.actionType == "ON_COLLISION" &&
                        it.params["targetTag"]?.trim()?.equals(b.tag.trim(), ignoreCase = true) == true
                    }.forEach { node -> executeConnections(a, node) }

                    // Trigger ON_COLLISION nodes on B for A's tag (case-insensitive & trimmed)
                    b.blueprintNodes.filter {
                        it.actionType == "ON_COLLISION" &&
                        it.params["targetTag"]?.trim()?.equals(a.tag.trim(), ignoreCase = true) == true
                    }.forEach { node -> executeConnections(b, node) }

                    // Bullet vs Enemy reaction
                    if (a.tag.trim().equals("Bullet", ignoreCase = true) && b.tag.trim().equals("Enemy", ignoreCase = true)) {
                        runtimeObjects.remove(a)
                        runtimeObjects.remove(b)
                        val scoreKey = globalVars.keys.find { it.trim().equals("score", ignoreCase = true) } ?: "score"
                        val currentScore = (globalVars[scoreKey]?.toIntOrNull() ?: 0) + 10
                        globalVars[scoreKey] = currentScore.toString()
                    } else if (b.tag.trim().equals("Bullet", ignoreCase = true) && a.tag.trim().equals("Enemy", ignoreCase = true)) {
                        runtimeObjects.remove(a)
                        runtimeObjects.remove(b)
                        val scoreKey = globalVars.keys.find { it.trim().equals("score", ignoreCase = true) } ?: "score"
                        val currentScore = (globalVars[scoreKey]?.toIntOrNull() ?: 0) + 10
                        globalVars[scoreKey] = currentScore.toString()
                    }

                    // Player vs Coin reaction
                    if (a.tag.trim().equals("Player", ignoreCase = true) && b.tag.trim().equals("Coin", ignoreCase = true)) {
                        runtimeObjects.remove(b)
                        val scoreKey = globalVars.keys.find { it.trim().equals("score", ignoreCase = true) } ?: "score"
                        val currentScore = (globalVars[scoreKey]?.toIntOrNull() ?: 0) + 1
                        globalVars[scoreKey] = currentScore.toString()
                    } else if (b.tag.trim().equals("Player", ignoreCase = true) && a.tag.trim().equals("Coin", ignoreCase = true)) {
                        runtimeObjects.remove(a)
                        val scoreKey = globalVars.keys.find { it.trim().equals("score", ignoreCase = true) } ?: "score"
                        val currentScore = (globalVars[scoreKey]?.toIntOrNull() ?: 0) + 1
                        globalVars[scoreKey] = currentScore.toString()
                    }
                }
            }
        }
    }

    private fun checkAABBCollision(a: RuntimeObject, b: RuntimeObject): Boolean {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y
    }

    private fun executeConnections(sourceObj: RuntimeObject, node: BlueprintNode, visited: MutableSet<String> = mutableSetOf()) {
        if (!visited.add(node.id)) return // Prevent cyclic infinite loops
        val connectedConns = sourceObj.blueprintConnections.filter { it.fromNodeId == node.id }
        for (conn in connectedConns) {
            val targetNode = sourceObj.blueprintNodes.find { it.id == conn.toNodeId } ?: continue
            executeNodeAction(sourceObj, targetNode, visited)
        }
    }

    private fun executeNodeAction(sourceObj: RuntimeObject, node: BlueprintNode, visited: MutableSet<String>) {
        val targetActorTag = node.params["targetActor"] ?: "Self"
        val targets = if (targetActorTag.isEmpty() || targetActorTag.trim().uppercase() == "SELF") {
            listOf(sourceObj)
        } else {
            runtimeObjects.filter {
                it.tag.trim().equals(targetActorTag.trim(), ignoreCase = true) ||
                it.name.trim().equals(targetActorTag.trim(), ignoreCase = true)
            }
        }

        targets.forEach { targetObj ->
            when (node.actionType) {
                "MOVE_XY" -> {
                    val dx = node.params["dx"]?.toFloatOrNull() ?: 0f
                    val dy = node.params["dy"]?.toFloatOrNull() ?: 0f
                    targetObj.x += dx
                    targetObj.y += dy
                }
                "SET_VELOCITY" -> {
                    val vx = node.params["vx"]?.toFloatOrNull() ?: targetObj.vx
                    val vy = node.params["vy"]?.toFloatOrNull() ?: targetObj.vy
                    targetObj.vx = vx
                    targetObj.vy = vy
                }
                "JUMP_IMPULSE" -> {
                    var power = node.params["power"]?.toFloatOrNull() ?: 300f
                    // Autoscale low legacy jump impulse forces to modern coordinate system scale
                    if (power in 1f..100f) {
                        power *= 13f
                    }
                    targetObj.vy = -power
                    targetObj.isGrounded = false
                }
                "APPLY_GRAVITY" -> {
                    targetObj.hasPhysics = true
                }
                "PLATFORMER_MOVE" -> {
                    val speed = node.params["speed"]?.toFloatOrNull() ?: 15f
                    targetObj.x += speed
                }
                "SHOOT_BULLET" -> {
                    val speed = node.params["speed"]?.toFloatOrNull() ?: 500f
                    spawnBullet(targetObj, speed)
                }
                "DESTROY_SELF" -> {
                    runtimeObjects.remove(targetObj)
                }
                "SET_PARENT" -> {
                    val parentTag = node.params["parentTag"] ?: ""
                    if (parentTag.isNotEmpty()) {
                        val parent = runtimeObjects.find {
                            it.tag.trim().equals(parentTag.trim(), ignoreCase = true) ||
                            it.name.trim().equals(parentTag.trim(), ignoreCase = true)
                        }
                        if (parent != null) {
                            targetObj.parentTag = parentTag
                            targetObj.parentOffsetX = targetObj.x - parent.x
                            targetObj.parentOffsetY = targetObj.y - parent.y
                        }
                    } else {
                        targetObj.parentTag = null
                    }
                }
                "FOLLOW_CAMERA" -> {
                    val cameraTarget = node.params["targetTag"] ?: "Player"
                    if (cameraTarget.trim().uppercase() == "SELF") {
                        cameraFollowTag = targetObj.tag
                    } else {
                        cameraFollowTag = cameraTarget
                    }
                }
                "PLAY_SOUND" -> {
                    val soundType = node.params["soundType"] ?: "COIN"
                    RetroAudioEngine.playSound(soundType)
                }
                "SET_VAR" -> {
                    val varName = node.params["varName"] ?: "score"
                    val valStr = node.params["value"] ?: "0"
                    if (varName.isNotEmpty()) {
                        val existingKey = globalVars.keys.find { it.trim().equals(varName.trim(), ignoreCase = true) } ?: varName
                        globalVars[existingKey] = valStr
                    }
                }
                "ADD_VAR" -> {
                    val varName = node.params["varName"] ?: "score"
                    val addVal = node.params["add"]?.toIntOrNull() ?: 1
                    if (varName.isNotEmpty()) {
                        val existingKey = globalVars.keys.find { it.trim().equals(varName.trim(), ignoreCase = true) } ?: varName
                        val curVal = globalVars[existingKey]?.toIntOrNull() ?: 0
                        globalVars[existingKey] = (curVal + addVal).toString()
                    }
                }
                "CHANGE_SCENE" -> {
                    val targetSceneName = node.params["targetScene"] ?: ""
                    val targetScene = activeProject?.scenes?.find { it.name.trim().equals(targetSceneName.trim(), ignoreCase = true) }
                    if (targetScene != null && activeProject != null) {
                        startScene(activeProject!!, targetScene)
                    }
                }
                "SPAWN_OBJECT" -> {
                    val name = node.params["name"] ?: "Coin"
                    val sprite = node.params["sprite"] ?: "COIN"
                    val dx = node.params["dx"]?.toFloatOrNull() ?: 0f
                    val dy = node.params["dy"]?.toFloatOrNull() ?: 0f
                    val isStatic = node.params["isStatic"]?.toBooleanStrictOrNull() ?: false
                    val hasPhysics = node.params["hasPhysics"]?.toBooleanStrictOrNull() ?: false
                    val tag = node.params["tag"] ?: name

                    val spawned = RuntimeObject(
                        originalId = UUID.randomUUID().toString(),
                        name = name,
                        x = targetObj.x + dx,
                        y = targetObj.y + dy,
                        vx = 0f,
                        vy = 0f,
                        width = 32f,
                        height = 32f,
                        colorHex = "#FFEB3B",
                        spritePreset = sprite,
                        imageUri = null,
                        hasPhysics = hasPhysics,
                        isStatic = isStatic,
                        tag = tag
                    )
                    runtimeObjects.add(spawned)
                }
            }
        }

        // Recursively continue execution downstream along connected target nodes
        executeConnections(sourceObj, node, visited)
    }
}
