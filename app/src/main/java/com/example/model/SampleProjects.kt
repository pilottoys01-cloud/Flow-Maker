package com.example.model

import java.util.UUID

object SampleProjects {

    fun createPlatformerProject(): GameProject {
        val scene1 = GameScene(name = "Level 1", backgroundColorHex = "#0F172A")

        // Player
        val player = GameObject(
            name = "Player",
            x = 200f,
            y = 150f,
            width = 50f,
            height = 50f,
            colorHex = "#3B82F6",
            spritePreset = "PLAYER",
            hasPhysics = true,
            isStatic = false,
            tag = "Player"
        )

        // Ground Platform
        val ground = GameObject(
            name = "Ground",
            x = 50f,
            y = 450f,
            width = 500f,
            height = 40f,
            colorHex = "#10B981",
            spritePreset = "PLATFORM",
            hasPhysics = true,
            isStatic = true,
            tag = "Platform"
        )

        // Floating Platform
        val platform = GameObject(
            name = "Platform 1",
            x = 280f,
            y = 320f,
            width = 160f,
            height = 25f,
            colorHex = "#10B981",
            spritePreset = "PLATFORM",
            hasPhysics = true,
            isStatic = true,
            tag = "Platform"
        )

        // Coin
        val coin = GameObject(
            name = "Gold Coin",
            x = 330f,
            y = 260f,
            width = 35f,
            height = 35f,
            colorHex = "#F59E0B",
            spritePreset = "COIN",
            hasPhysics = false,
            tag = "Coin"
        )

        // UI Buttons matching wireframe #1 bottom buttons
        val btnJump = GameUIButton(
            name = "Jump Button",
            label = "JUMP",
            x = 420f,
            y = 480f,
            width = 90f,
            height = 50f,
            colorHex = "#EC4899",
            actionKey = "JUMP"
        )

        val btnLeft = GameUIButton(
            name = "Left Button",
            label = "◄",
            x = 30f,
            y = 480f,
            width = 65f,
            height = 50f,
            colorHex = "#6366F1",
            actionKey = "LEFT"
        )

        val btnRight = GameUIButton(
            name = "Right Button",
            label = "►",
            x = 110f,
            y = 480f,
            width = 65f,
            height = 50f,
            colorHex = "#6366F1",
            actionKey = "RIGHT"
        )

        // Player Blueprint Nodes matching wireframe #2 START -> MOVE
        val nodeStart = BlueprintNode(
            title = "ON GAME START",
            category = NodeCategory.START,
            actionType = "ON_START",
            x = 50f,
            y = 80f
        )

        val nodeGravity = BlueprintNode(
            title = "ENABLE GRAVITY",
            category = NodeCategory.PLATF,
            actionType = "APPLY_GRAVITY",
            x = 280f,
            y = 80f,
            params = mutableMapOf("gravity" to "2.5")
        )

        val nodeBtnJump = BlueprintNode(
            title = "ON BUTTON JUMP",
            category = NodeCategory.INPUT,
            actionType = "ON_UI_BUTTON",
            x = 50f,
            y = 220f,
            params = mutableMapOf("buttonKey" to "JUMP")
        )

        val nodeJumpImpulse = BlueprintNode(
            title = "JUMP IMPULSE",
            category = NodeCategory.PLATF,
            actionType = "JUMP_IMPULSE",
            x = 280f,
            y = 220f,
            params = mutableMapOf("power" to "28")
        )

        val conn1 = BlueprintConnection(fromNodeId = nodeStart.id, toNodeId = nodeGravity.id)
        val conn2 = BlueprintConnection(fromNodeId = nodeBtnJump.id, toNodeId = nodeJumpImpulse.id)

        player.blueprintNodes.addAll(listOf(nodeStart, nodeGravity, nodeBtnJump, nodeJumpImpulse))
        player.blueprintConnections.addAll(listOf(conn1, conn2))

        scene1.objects.addAll(listOf(player, ground, platform, coin))
        scene1.uiButtons.addAll(listOf(btnLeft, btnRight, btnJump))

        return GameProject(
            name = "Platformer Demo",
            activeSceneId = scene1.id,
            scenes = mutableListOf(scene1),
            globalVariables = mutableListOf(
                GameVariable(name = "score", value = "0", type = "NUMBER"),
                GameVariable(name = "lives", value = "3", type = "NUMBER")
            )
        )
    }

    fun createSpaceShooterProject(): GameProject {
        val scene = GameScene(name = "Space Arena", backgroundColorHex = "#030712")

        val ship = GameObject(
            name = "Hero Ship",
            x = 200f,
            y = 400f,
            width = 55f,
            height = 55f,
            colorHex = "#06B6D4",
            spritePreset = "SPACESHIP",
            hasPhysics = false,
            tag = "Player"
        )

        val enemy1 = GameObject(
            name = "Enemy 1",
            x = 100f,
            y = 100f,
            width = 45f,
            height = 45f,
            colorHex = "#EF4444",
            spritePreset = "ENEMY",
            hasPhysics = false,
            tag = "Enemy"
        )

        val enemy2 = GameObject(
            name = "Enemy 2",
            x = 300f,
            y = 120f,
            width = 45f,
            height = 45f,
            colorHex = "#EF4444",
            spritePreset = "ENEMY",
            hasPhysics = false,
            tag = "Enemy"
        )

        val btnLeft = GameUIButton(
            name = "Left Button",
            label = "◄",
            x = 30f,
            y = 480f,
            width = 65f,
            height = 50f,
            colorHex = "#0891B2",
            actionKey = "LEFT"
        )

        val btnRight = GameUIButton(
            name = "Right Button",
            label = "►",
            x = 110f,
            y = 480f,
            width = 65f,
            height = 50f,
            colorHex = "#0891B2",
            actionKey = "RIGHT"
        )

        val fireBtn = GameUIButton(
            name = "Fire Button",
            label = "SHOOT 🚀",
            x = 380f,
            y = 480f,
            width = 110f,
            height = 55f,
            colorHex = "#F43F5E",
            actionKey = "SHOOT"
        )

        scene.objects.addAll(listOf(ship, enemy1, enemy2))
        scene.uiButtons.addAll(listOf(btnLeft, btnRight, fireBtn))

        return GameProject(
            name = "Space Shooter 2D",
            activeSceneId = scene.id,
            scenes = mutableListOf(scene),
            globalVariables = mutableListOf(
                GameVariable(name = "score", value = "0", type = "NUMBER")
            )
        )
    }
}
