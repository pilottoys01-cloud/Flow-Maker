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

    private fun getToneGenerator(): ToneGenerator? {
        if (toneGen == null) {
            try {
                toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return toneGen
    }

    fun playSound(type: String) {
        val gen = getToneGenerator() ?: return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (type.trim().uppercase()) {
                    "JUMP", "SALTO" -> {
                        gen.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    }
                    "SHOOT", "LASER", "DISPARO" -> {
                        gen.startTone(ToneGenerator.TONE_PROP_ACK, 80)
                    }
                    "COIN", "WIN", "MONEDA", "PUNTOS", "GANAR" -> {
                        gen.startTone(ToneGenerator.TONE_CDMA_PIP, 120)
                        delay(100)
                        gen.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                    }
                    "HIT", "EXPLOSION", "GOLPE", "COLISION", "DAÑO", "IMPACTO", "TOUCH", "CHOQUE", "BEEP" -> {
                        gen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                    }
                    "MUSIC", "MELODY", "MUSICA" -> {
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
                        gen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
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
    var isSolid: Boolean = false,
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
    var cameraFollowObject by mutableStateOf<RuntimeObject?>(null)
    var viewportWidth by mutableStateOf(360f)
    var viewportHeight by mutableStateOf(640f)

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
        cameraFollowObject = null

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
                isSolid = obj.isSolid,
                tag = obj.tag,
                blueprintNodes = obj.blueprintNodes.toList(),
                blueprintConnections = obj.blueprintConnections.toList()
            ).apply {
                parentTag = obj.parentTag
            }
            obj.variables.forEach { v ->
                rObj.variables[v.name] = v.value
            }
            runtimeObjects.add(rObj)
        }

        runtimeUIButtons.addAll(scene.uiButtons)
        isInitialized = true

        // Precompute initial parent attachment offsets for attached objects/cubes
        runtimeObjects.forEach { rObj ->
            val pTag = rObj.parentTag
            if (!pTag.isNullOrBlank()) {
                val parent = runtimeObjects.find {
                    it.tag.trim().equals(pTag.trim(), ignoreCase = true) ||
                    it.name.trim().equals(pTag.trim(), ignoreCase = true)
                }
                if (parent != null) {
                    rObj.parentOffsetX = rObj.x - parent.x
                    rObj.parentOffsetY = rObj.y - parent.y
                }
            }
        }

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

        val collidedPairsThisFrame = HashSet<Pair<RuntimeObject, RuntimeObject>>()

        // Physics movement and collisions for objects
        runtimeObjects.toList().forEach { rObj ->
            if (rObj.hasPhysics && !rObj.isStatic && rObj.parentTag.isNullOrEmpty()) {
                // Apply Gravity
                rObj.vy += 800f * dt
                rObj.x += rObj.vx * dt
                rObj.y += rObj.vy * dt

                // Platform and solid collisions
                var landed = false
                val collidables = runtimeObjects.filter {
                    it != rObj && (it.isStatic || it.isSolid || it.tag.equals("Platform", ignoreCase = true) || it.tag.equals("Solid", ignoreCase = true) || it.tag.equals("Ground", ignoreCase = true) || it.tag.equals("Caja", ignoreCase = true) || it.tag.equals("Box", ignoreCase = true))
                }
                collidables.forEach { platform ->
                    if (checkAABBCollision(rObj, platform)) {
                        collidedPairsThisFrame.add(Pair(rObj, platform))
                        val prevY = rObj.y - rObj.vy * dt
                        if (rObj.vy >= 0 && (prevY + rObj.height) <= platform.y + 18f) {
                            rObj.y = platform.y - rObj.height
                            rObj.vy = 0f
                            landed = true
                        } else if (rObj.vy < 0 && prevY >= platform.y + platform.height - 18f) {
                            rObj.y = platform.y + platform.height
                            rObj.vy = 0f
                        } else if (rObj.isSolid || platform.isSolid) {
                            val rObjCenterX = rObj.x + rObj.width / 2f
                            val platformCenterX = platform.x + platform.width / 2f
                            if (rObjCenterX < platformCenterX) {
                                rObj.x = platform.x - rObj.width
                                rObj.vx = 0f
                            } else {
                                rObj.x = platform.x + platform.width
                                rObj.vx = 0f
                            }
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

        // Enforce Parent attachment AFTER movement/physics tick
        runtimeObjects.toList().forEach { rObj ->
            val pTag = rObj.parentTag
            if (!pTag.isNullOrBlank()) {
                val parent = runtimeObjects.find {
                    it.tag.trim().equals(pTag.trim(), ignoreCase = true) ||
                    it.name.trim().equals(pTag.trim(), ignoreCase = true)
                }
                if (parent != null) {
                    if (rObj.parentOffsetX == 0f && rObj.parentOffsetY == 0f && (rObj.x != parent.x || rObj.y != parent.y)) {
                        rObj.parentOffsetX = rObj.x - parent.x
                        rObj.parentOffsetY = rObj.y - parent.y
                    }
                    rObj.x = parent.x + rObj.parentOffsetX
                    rObj.y = parent.y + rObj.parentOffsetY
                    rObj.vx = 0f
                    rObj.vy = 0f
                }
            }
        }

        // Update Camera tracking based on target object center on every frame
        val targetToFollow = cameraFollowObject.takeIf { it != null && runtimeObjects.contains(it) }
            ?: (!cameraFollowTag.isNullOrBlank()).let {
                runtimeObjects.find {
                    it.tag.trim().equals(cameraFollowTag!!.trim(), ignoreCase = true) ||
                    it.name.trim().equals(cameraFollowTag!!.trim(), ignoreCase = true)
                }
            }
            ?: runtimeObjects.find {
                it.tag.trim().equals("Player", ignoreCase = true) ||
                it.name.trim().equals("Player", ignoreCase = true) ||
                it.tag.trim().equals("Jugador", ignoreCase = true) ||
                it.name.trim().equals("Jugador", ignoreCase = true)
            }

        if (targetToFollow != null) {
            val targetCenterX = targetToFollow.x + (targetToFollow.width / 2f)
            val targetCenterY = targetToFollow.y + (targetToFollow.height / 2f)
            cameraX = (viewportWidth / 2f) - targetCenterX
            cameraY = (viewportHeight / 2f) - targetCenterY
        }

        // Bounding box collision detection between objects
        checkTagCollisions(collidedPairsThisFrame)
    }

    private fun checkTagCollisions(collidedPairsThisFrame: Set<Pair<RuntimeObject, RuntimeObject>>) {
        val list = runtimeObjects.toList()
        for (i in list.indices) {
            val a = list[i]
            if (!runtimeObjects.contains(a)) continue

            for (j in i + 1 until list.size) {
                val b = list[j]
                if (!runtimeObjects.contains(b)) continue

                val isColliding = checkAABBCollision(a, b) ||
                        collidedPairsThisFrame.contains(Pair(a, b)) ||
                        collidedPairsThisFrame.contains(Pair(b, a))

                if (isColliding) {
                    var hasHandledSound = false

                    // Trigger ON_COLLISION nodes on A for B
                    if (runtimeObjects.contains(a) && runtimeObjects.contains(b)) {
                        a.blueprintNodes.filter { node ->
                            if (node.actionType != "ON_COLLISION") return@filter false
                            val target = node.params["targetTag"]?.trim()
                                ?: node.params["target"]?.trim()
                                ?: node.params["tag"]?.trim()
                                ?: ""
                            target.isEmpty() ||
                            target.equals("Any", ignoreCase = true) ||
                            target.equals("Cualquiera", ignoreCase = true) ||
                            target.equals("Todos", ignoreCase = true) ||
                            target.equals("*", ignoreCase = true) ||
                            target.equals("Self", ignoreCase = true) ||
                            target.equals("Este Objeto", ignoreCase = true) ||
                            target.equals(b.tag.trim(), ignoreCase = true) ||
                            target.equals(b.name.trim(), ignoreCase = true) ||
                            target.equals(b.spritePreset.trim(), ignoreCase = true) ||
                            (target.isNotEmpty() && (b.tag.contains(target, ignoreCase = true) || b.name.contains(target, ignoreCase = true)))
                        }.forEach { node ->
                            executeConnections(a, node, otherObj = b)
                            hasHandledSound = true
                        }
                    }

                    // Trigger ON_COLLISION nodes on B for A
                    if (runtimeObjects.contains(a) && runtimeObjects.contains(b)) {
                        b.blueprintNodes.filter { node ->
                            if (node.actionType != "ON_COLLISION") return@filter false
                            val target = node.params["targetTag"]?.trim()
                                ?: node.params["target"]?.trim()
                                ?: node.params["tag"]?.trim()
                                ?: ""
                            target.isEmpty() ||
                            target.equals("Any", ignoreCase = true) ||
                            target.equals("Cualquiera", ignoreCase = true) ||
                            target.equals("Todos", ignoreCase = true) ||
                            target.equals("*", ignoreCase = true) ||
                            target.equals("Self", ignoreCase = true) ||
                            target.equals("Este Objeto", ignoreCase = true) ||
                            target.equals(a.tag.trim(), ignoreCase = true) ||
                            target.equals(a.name.trim(), ignoreCase = true) ||
                            target.equals(a.spritePreset.trim(), ignoreCase = true) ||
                            (target.isNotEmpty() && (a.tag.contains(target, ignoreCase = true) || a.name.contains(target, ignoreCase = true)))
                        }.forEach { node ->
                            executeConnections(b, node, otherObj = a)
                            hasHandledSound = true
                        }
                    }

                    // Bullet vs Enemy reaction
                    if ((a.tag.trim().equals("Bullet", ignoreCase = true) && b.tag.trim().equals("Enemy", ignoreCase = true)) ||
                        (a.tag.trim().equals("Bullet", ignoreCase = true) && b.name.trim().equals("Enemy", ignoreCase = true))) {
                        runtimeObjects.remove(a)
                        runtimeObjects.remove(b)
                        RetroAudioEngine.playSound("EXPLOSION")
                        hasHandledSound = true
                        val scoreKey = globalVars.keys.find { it.trim().equals("score", ignoreCase = true) } ?: "score"
                        val currentScore = (globalVars[scoreKey]?.toIntOrNull() ?: 0) + 10
                        globalVars[scoreKey] = currentScore.toString()
                    } else if ((b.tag.trim().equals("Bullet", ignoreCase = true) && a.tag.trim().equals("Enemy", ignoreCase = true)) ||
                        (b.tag.trim().equals("Bullet", ignoreCase = true) && a.name.trim().equals("Enemy", ignoreCase = true))) {
                        runtimeObjects.remove(a)
                        runtimeObjects.remove(b)
                        RetroAudioEngine.playSound("EXPLOSION")
                        hasHandledSound = true
                        val scoreKey = globalVars.keys.find { it.trim().equals("score", ignoreCase = true) } ?: "score"
                        val currentScore = (globalVars[scoreKey]?.toIntOrNull() ?: 0) + 10
                        globalVars[scoreKey] = currentScore.toString()
                    }

                    // Player vs Coin reaction
                    if (a.tag.trim().equals("Player", ignoreCase = true) && b.tag.trim().equals("Coin", ignoreCase = true)) {
                        runtimeObjects.remove(b)
                        RetroAudioEngine.playSound("COIN")
                        hasHandledSound = true
                        val scoreKey = globalVars.keys.find { it.trim().equals("score", ignoreCase = true) } ?: "score"
                        val currentScore = (globalVars[scoreKey]?.toIntOrNull() ?: 0) + 1
                        globalVars[scoreKey] = currentScore.toString()
                    } else if (b.tag.trim().equals("Player", ignoreCase = true) && a.tag.trim().equals("Coin", ignoreCase = true)) {
                        runtimeObjects.remove(a)
                        RetroAudioEngine.playSound("COIN")
                        hasHandledSound = true
                        val scoreKey = globalVars.keys.find { it.trim().equals("score", ignoreCase = true) } ?: "score"
                        val currentScore = (globalVars[scoreKey]?.toIntOrNull() ?: 0) + 1
                        globalVars[scoreKey] = currentScore.toString()
                    }

                    // Default collision sound feedback for any other touched pair
                    if (!hasHandledSound) {
                        val isPlayerA = a.tag.contains("Player", true) || a.name.contains("Player", true) || a.spritePreset.contains("PLAYER", true)
                        val isPlayerB = b.tag.contains("Player", true) || b.name.contains("Player", true) || b.spritePreset.contains("PLAYER", true)
                        val other = if (isPlayerA) b else if (isPlayerB) a else null
                        if (other != null) {
                            val tagUpper = other.tag.uppercase()
                            val nameUpper = other.name.uppercase()
                            val spriteUpper = other.spritePreset.uppercase()
                            if (tagUpper.contains("COIN") || nameUpper.contains("COIN") || spriteUpper.contains("COIN") || tagUpper.contains("MONEDA")) {
                                RetroAudioEngine.playSound("COIN")
                                runtimeObjects.remove(other)
                            } else if (tagUpper.contains("ENEMY") || nameUpper.contains("ENEMY") || spriteUpper.contains("ENEMY") || tagUpper.contains("ENEMIGO")) {
                                RetroAudioEngine.playSound("EXPLOSION")
                            } else {
                                RetroAudioEngine.playSound("HIT")
                            }
                        }
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

    private fun executeConnections(sourceObj: RuntimeObject, node: BlueprintNode, visited: MutableSet<String> = mutableSetOf(), otherObj: RuntimeObject? = null) {
        if (!visited.add(node.id)) return // Prevent cyclic infinite loops
        val connectedConns = sourceObj.blueprintConnections.filter { it.fromNodeId == node.id }
        for (conn in connectedConns) {
            val targetNode = sourceObj.blueprintNodes.find { it.id == conn.toNodeId } ?: continue
            executeNodeAction(sourceObj, targetNode, visited, otherObj)
        }
    }

    private fun executeNodeAction(sourceObj: RuntimeObject, node: BlueprintNode, visited: MutableSet<String>, otherObj: RuntimeObject? = null) {
        val targetActorTag = node.params["targetActor"] ?: "Self"
        val targets = when {
            targetActorTag.isEmpty() || targetActorTag.trim().equals("SELF", ignoreCase = true) -> {
                listOf(sourceObj)
            }
            targetActorTag.trim().equals("OTHER", ignoreCase = true) ||
            targetActorTag.trim().equals("TARGET", ignoreCase = true) ||
            targetActorTag.trim().equals("COLLIDED", ignoreCase = true) -> {
                listOfNotNull(otherObj)
            }
            otherObj != null && (
                targetActorTag.trim().equals(otherObj.tag.trim(), ignoreCase = true) ||
                targetActorTag.trim().equals(otherObj.name.trim(), ignoreCase = true)
            ) -> {
                listOf(otherObj)
            }
            else -> {
                runtimeObjects.filter {
                    it.tag.trim().equals(targetActorTag.trim(), ignoreCase = true) ||
                    it.name.trim().equals(targetActorTag.trim(), ignoreCase = true)
                }
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
                    val allowAirJump = node.params["allowAirJump"]?.toBooleanStrictOrNull() ?: false
                    // Previene salto infinito: solo salta si está en el suelo (isGrounded) o si no usa física
                    if (!targetObj.hasPhysics || targetObj.isGrounded || allowAirJump) {
                        targetObj.vy = -power
                        targetObj.isGrounded = false
                    }
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
                    val cameraTarget = node.params["targetTag"]?.trim() ?: "Player"
                    if (cameraTarget.isEmpty() || cameraTarget.equals("SELF", ignoreCase = true) || cameraTarget.equals("Este Objeto", ignoreCase = true)) {
                        cameraFollowObject = targetObj
                        cameraFollowTag = targetObj.tag.ifBlank { targetObj.name }
                    } else {
                        val found = runtimeObjects.find {
                            it.tag.trim().equals(cameraTarget, ignoreCase = true) ||
                            it.name.trim().equals(cameraTarget, ignoreCase = true)
                        }
                        if (found != null) {
                            cameraFollowObject = found
                            cameraFollowTag = found.tag.ifBlank { found.name }
                        } else {
                            cameraFollowTag = cameraTarget
                            cameraFollowObject = null
                        }
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
                    val name = node.params["name"] ?: "Box"
                    val sprite = node.params["sprite"] ?: "BOX"
                    val dx = node.params["dx"]?.toFloatOrNull() ?: 0f
                    val dy = node.params["dy"]?.toFloatOrNull() ?: -40f
                    val count = node.params["count"]?.toIntOrNull()?.coerceIn(1, 50) ?: 1
                    val isStatic = node.params["isStatic"]?.toBooleanStrictOrNull() ?: false
                    val hasPhysics = node.params["hasPhysics"]?.toBooleanStrictOrNull() ?: true
                    val isSolid = node.params["isSolid"]?.toBooleanStrictOrNull() ?: true
                    val tag = node.params["tag"] ?: name

                    repeat(count) { i ->
                        val spreadX = if (count > 1) (i - (count - 1) / 2f) * 25f else 0f
                        val spawned = RuntimeObject(
                            originalId = UUID.randomUUID().toString(),
                            name = "$name ${runtimeObjects.size + 1}",
                            x = targetObj.x + dx + spreadX,
                            y = targetObj.y + dy,
                            vx = if (hasPhysics && count > 1) (i - (count - 1) / 2f) * 40f else 0f,
                            vy = 0f,
                            width = 36f,
                            height = 36f,
                            colorHex = "#3B82F6",
                            spritePreset = sprite,
                            imageUri = null,
                            hasPhysics = hasPhysics,
                            isStatic = isStatic,
                            isSolid = isSolid,
                            tag = tag
                        )
                        runtimeObjects.add(spawned)
                    }
                }
            }
        }

        // Recursively continue execution downstream along connected target nodes
        executeConnections(sourceObj, node, visited, otherObj)
    }
}
