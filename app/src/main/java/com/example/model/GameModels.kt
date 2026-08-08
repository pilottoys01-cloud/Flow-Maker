package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ObjectType {
    SPRITE,
    UI_BUTTON
}

enum class NodeCategory(val displayName: String, val colorHex: String) {
    MOVE("MOVE", "#2196F3"),
    EVENT("EVENT", "#FFEB3B"),
    VARI("VARI", "#FF5722"),
    START("START", "#4CAF50"),
    DATA("DATA", "#E91E63"),
    PLATF("PLATF", "#9C27B0"),
    RPG("RPG", "#FF9800"),
    SHOOT("SHOOT", "#00BCD4"),
    INPUT("INPUT", "#7C4DFF"),
    SCENE("SCENE", "#607D8B")
}

data class GameVariable(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var value: String = "0",
    var type: String = "NUMBER" // NUMBER, STRING, BOOLEAN
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("value", value)
        put("type", type)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): GameVariable = GameVariable(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name", "var1"),
            value = obj.optString("value", "0"),
            type = obj.optString("type", "NUMBER")
        )
    }
}

data class BlueprintConnection(
    val id: String = UUID.randomUUID().toString(),
    val fromNodeId: String,
    val fromPort: String = "exec_out",
    val toNodeId: String,
    val toPort: String = "exec_in"
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("fromNodeId", fromNodeId)
        put("fromPort", fromPort)
        put("toNodeId", toNodeId)
        put("toPort", toPort)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): BlueprintConnection = BlueprintConnection(
            id = obj.optString("id", UUID.randomUUID().toString()),
            fromNodeId = obj.optString("fromNodeId"),
            fromPort = obj.optString("fromPort", "exec_out"),
            toNodeId = obj.optString("toNodeId"),
            toPort = obj.optString("toPort", "exec_in")
        )
    }
}

data class BlueprintNode(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    val category: NodeCategory,
    val actionType: String,
    var x: Float,
    var y: Float,
    val params: MutableMap<String, String> = mutableMapOf()
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("category", category.name)
        put("actionType", actionType)
        put("x", x.toDouble())
        put("y", y.toDouble())
        val paramsObj = JSONObject()
        params.forEach { (k, v) -> paramsObj.put(k, v) }
        put("params", paramsObj)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): BlueprintNode {
            val paramsObj = obj.optJSONObject("params")
            val paramsMap = mutableMapOf<String, String>()
            if (paramsObj != null) {
                paramsObj.keys().forEach { key ->
                    paramsMap[key] = paramsObj.optString(key, "")
                }
            }
            return BlueprintNode(
                id = obj.optString("id", UUID.randomUUID().toString()),
                title = obj.optString("title", "Node"),
                category = try { NodeCategory.valueOf(obj.optString("category", "MOVE")) } catch (e: Exception) { NodeCategory.MOVE },
                actionType = obj.optString("actionType", "MOVE_XY"),
                x = obj.optDouble("x", 100.0).toFloat(),
                y = obj.optDouble("y", 100.0).toFloat(),
                params = paramsMap
            )
        }
    }
}

data class GameObject(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var x: Float,
    var y: Float,
    var width: Float = 60f,
    var height: Float = 60f,
    var rotation: Float = 0f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var colorHex: String = "#4CAF50",
    var spritePreset: String = "BOX", // BOX, PLAYER, ENEMY, PLATFORM, COIN, SPACESHIP, HEART, CUSTOM_IMAGE
    var imageUri: String? = null,
    var hasPhysics: Boolean = false,
    var isStatic: Boolean = false,
    var isSolid: Boolean = false,
    var tag: String = "Untagged",
    var parentTag: String? = null,
    val variables: MutableList<GameVariable> = mutableListOf(),
    val blueprintNodes: MutableList<BlueprintNode> = mutableListOf(),
    val blueprintConnections: MutableList<BlueprintConnection> = mutableListOf()
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("width", width.toDouble())
        put("height", height.toDouble())
        put("rotation", rotation.toDouble())
        put("scaleX", scaleX.toDouble())
        put("scaleY", scaleY.toDouble())
        put("colorHex", colorHex)
        put("spritePreset", spritePreset)
        put("imageUri", imageUri ?: "")
        put("hasPhysics", hasPhysics)
        put("isStatic", isStatic)
        put("isSolid", isSolid)
        put("tag", tag)
        put("parentTag", parentTag ?: "")

        val varsArr = JSONArray()
        variables.forEach { varsArr.put(it.toJsonObject()) }
        put("variables", varsArr)

        val nodesArr = JSONArray()
        blueprintNodes.forEach { nodesArr.put(it.toJsonObject()) }
        put("blueprintNodes", nodesArr)

        val connArr = JSONArray()
        blueprintConnections.forEach { connArr.put(it.toJsonObject()) }
        put("blueprintConnections", connArr)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): GameObject {
            val gameObj = GameObject(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Object"),
                x = obj.optDouble("x", 100.0).toFloat(),
                y = obj.optDouble("y", 100.0).toFloat(),
                width = obj.optDouble("width", 60.0).toFloat(),
                height = obj.optDouble("height", 60.0).toFloat(),
                rotation = obj.optDouble("rotation", 0.0).toFloat(),
                scaleX = obj.optDouble("scaleX", 1.0).toFloat(),
                scaleY = obj.optDouble("scaleY", 1.0).toFloat(),
                colorHex = obj.optString("colorHex", "#4CAF50"),
                spritePreset = obj.optString("spritePreset", "BOX"),
                imageUri = obj.optString("imageUri").takeIf { it.isNotEmpty() },
                hasPhysics = obj.optBoolean("hasPhysics", false),
                isStatic = obj.optBoolean("isStatic", false),
                isSolid = obj.optBoolean("isSolid", false),
                tag = obj.optString("tag", "Untagged"),
                parentTag = obj.optString("parentTag", "").takeIf { it.isNotEmpty() }
            )

            val varsArr = obj.optJSONArray("variables")
            if (varsArr != null) {
                for (i in 0 until varsArr.length()) {
                    gameObj.variables.add(GameVariable.fromJsonObject(varsArr.getJSONObject(i)))
                }
            }

            val nodesArr = obj.optJSONArray("blueprintNodes")
            if (nodesArr != null) {
                for (i in 0 until nodesArr.length()) {
                    gameObj.blueprintNodes.add(BlueprintNode.fromJsonObject(nodesArr.getJSONObject(i)))
                }
            }

            val connArr = obj.optJSONArray("blueprintConnections")
            if (connArr != null) {
                for (i in 0 until connArr.length()) {
                    gameObj.blueprintConnections.add(BlueprintConnection.fromJsonObject(connArr.getJSONObject(i)))
                }
            }

            return gameObj
        }
    }
}

data class GameUIButton(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var label: String,
    var x: Float,
    var y: Float,
    var width: Float = 80f,
    var height: Float = 50f,
    var colorHex: String = "#FF9800",
    var imageUri: String? = null,
    var actionKey: String = "JUMP",
    val blueprintNodes: MutableList<BlueprintNode> = mutableListOf(),
    val blueprintConnections: MutableList<BlueprintConnection> = mutableListOf()
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("label", label)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("width", width.toDouble())
        put("height", height.toDouble())
        put("colorHex", colorHex)
        put("imageUri", imageUri ?: "")
        put("actionKey", actionKey)

        val nodesArr = JSONArray()
        blueprintNodes.forEach { nodesArr.put(it.toJsonObject()) }
        put("blueprintNodes", nodesArr)

        val connArr = JSONArray()
        blueprintConnections.forEach { connArr.put(it.toJsonObject()) }
        put("blueprintConnections", connArr)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): GameUIButton {
            val btn = GameUIButton(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Button"),
                label = obj.optString("label", "BTN"),
                x = obj.optDouble("x", 50.0).toFloat(),
                y = obj.optDouble("y", 300.0).toFloat(),
                width = obj.optDouble("width", 80.0).toFloat(),
                height = obj.optDouble("height", 50.0).toFloat(),
                colorHex = obj.optString("colorHex", "#FF9800"),
                imageUri = obj.optString("imageUri").takeIf { it.isNotEmpty() },
                actionKey = obj.optString("actionKey", "JUMP")
            )

            val nodesArr = obj.optJSONArray("blueprintNodes")
            if (nodesArr != null) {
                for (i in 0 until nodesArr.length()) {
                    btn.blueprintNodes.add(BlueprintNode.fromJsonObject(nodesArr.getJSONObject(i)))
                }
            }

            val connArr = obj.optJSONArray("blueprintConnections")
            if (connArr != null) {
                for (i in 0 until connArr.length()) {
                    btn.blueprintConnections.add(BlueprintConnection.fromJsonObject(connArr.getJSONObject(i)))
                }
            }

            return btn
        }
    }
}

data class GameScene(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var backgroundColorHex: String = "#1E293B",
    val objects: MutableList<GameObject> = mutableListOf(),
    val uiButtons: MutableList<GameUIButton> = mutableListOf()
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("backgroundColorHex", backgroundColorHex)

        val objArr = JSONArray()
        objects.forEach { objArr.put(it.toJsonObject()) }
        put("objects", objArr)

        val btnArr = JSONArray()
        uiButtons.forEach { btnArr.put(it.toJsonObject()) }
        put("uiButtons", btnArr)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): GameScene {
            val scene = GameScene(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Scene 1"),
                backgroundColorHex = obj.optString("backgroundColorHex", "#1E293B")
            )

            val objArr = obj.optJSONArray("objects")
            if (objArr != null) {
                for (i in 0 until objArr.length()) {
                    scene.objects.add(GameObject.fromJsonObject(objArr.getJSONObject(i)))
                }
            }

            val btnArr = obj.optJSONArray("uiButtons")
            if (btnArr != null) {
                for (i in 0 until btnArr.length()) {
                    scene.uiButtons.add(GameUIButton.fromJsonObject(btnArr.getJSONObject(i)))
                }
            }

            return scene
        }
    }
}

data class GameProject(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var lastModified: Long = System.currentTimeMillis(),
    var activeSceneId: String = "",
    val scenes: MutableList<GameScene> = mutableListOf(),
    val globalVariables: MutableList<GameVariable> = mutableListOf()
) {
    fun getActiveScene(): GameScene? {
        return scenes.find { it.id == activeSceneId } ?: scenes.firstOrNull()
    }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("lastModified", lastModified)
        put("activeSceneId", activeSceneId)

        val scenesArr = JSONArray()
        scenes.forEach { scenesArr.put(it.toJsonObject()) }
        put("scenes", scenesArr)

        val varsArr = JSONArray()
        globalVariables.forEach { varsArr.put(it.toJsonObject()) }
        put("globalVariables", varsArr)
    }

    fun toJsonString(indent: Int = 2): String = toJsonObject().toString(indent)

    companion object {
        fun fromJsonObject(obj: JSONObject): GameProject {
            val proj = GameProject(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name", "Untitled Game"),
                lastModified = obj.optLong("lastModified", System.currentTimeMillis()),
                activeSceneId = obj.optString("activeSceneId", "")
            )

            val scenesArr = obj.optJSONArray("scenes")
            if (scenesArr != null) {
                for (i in 0 until scenesArr.length()) {
                    proj.scenes.add(GameScene.fromJsonObject(scenesArr.getJSONObject(i)))
                }
            }

            val varsArr = obj.optJSONArray("globalVariables")
            if (varsArr != null) {
                for (i in 0 until varsArr.length()) {
                    proj.globalVariables.add(GameVariable.fromJsonObject(varsArr.getJSONObject(i)))
                }
            }

            if (proj.activeSceneId.isEmpty() && proj.scenes.isNotEmpty()) {
                proj.activeSceneId = proj.scenes.first().id
            }

            return proj
        }

        fun fromJsonString(json: String): GameProject? {
            return try {
                fromJsonObject(JSONObject(json))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
