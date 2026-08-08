package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.engine.GameEngine
import com.example.model.GameObject
import com.example.model.GameProject
import com.example.model.GameScene
import com.example.model.GameUIButton
import com.example.model.SampleProjects
import com.example.repository.ProjectRepository
import com.example.ui.screens.AnimationEditorScreen
import com.example.ui.screens.BlueprintEditorScreen
import com.example.ui.screens.ExportDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayGameScreen
import com.example.ui.screens.SceneEditorScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    HOME,
    SCENE_EDITOR,
    BLUEPRINT_EDITOR,
    ANIMATION_EDITOR,
    PLAY
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FlowMakerApp()
                }
            }
        }
    }
}

@Composable
fun FlowMakerApp() {
    val context = LocalContext.current
    val repository = remember { ProjectRepository(context) }
    val gameEngine = remember { GameEngine() }

    val projects = remember { mutableStateListOf<GameProject>() }

    // Load initial projects from repository
    remember {
        projects.addAll(repository.loadAllProjects())
    }

    var currentScreen by remember { mutableStateOf(ScreenState.HOME) }
    var activeProject by remember { mutableStateOf<GameProject?>(null) }
    var activeSceneId by remember { mutableStateOf<String?>(null) }
    var selectedObjectId by remember { mutableStateOf<String?>(null) }
    var selectedButtonId by remember { mutableStateOf<String?>(null) }
    var blueprintTargetObject by remember { mutableStateOf<GameObject?>(null) }
    var animationTargetObject by remember { mutableStateOf<GameObject?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val currentScene = activeProject?.scenes?.find { it.id == activeSceneId }
        ?: activeProject?.scenes?.firstOrNull()

    fun saveProjectsState() {
        repository.saveAllProjects(projects)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            ScreenState.HOME -> {
                HomeScreen(
                    projects = projects,
                    onSelectProject = { proj ->
                        activeProject = proj
                        activeSceneId = proj.activeSceneId.ifEmpty { proj.scenes.firstOrNull()?.id }
                        selectedObjectId = null
                        selectedButtonId = null
                        currentScreen = ScreenState.SCENE_EDITOR
                    },
                    onCreateProject = { name ->
                        val newProj = GameProject(name = name)
                        val defaultScene = GameScene(name = "Level 1")
                        newProj.scenes.add(defaultScene)
                        newProj.activeSceneId = defaultScene.id
                        projects.add(0, newProj)
                        saveProjectsState()

                        activeProject = newProj
                        activeSceneId = defaultScene.id
                        currentScreen = ScreenState.SCENE_EDITOR
                    },
                    onCloneProject = { proj ->
                        val cloned = repository.cloneProject(proj)
                        projects.add(0, cloned)
                        saveProjectsState()
                        Toast.makeText(context, "Cloned project '${proj.name}'", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteProject = { proj ->
                        projects.remove(proj)
                        saveProjectsState()
                        Toast.makeText(context, "Deleted project '${proj.name}'", Toast.LENGTH_SHORT).show()
                    },
                    onImportJson = { jsonStr ->
                        val imported = GameProject.fromJsonString(jsonStr)
                        if (imported != null) {
                            projects.add(0, imported)
                            saveProjectsState()
                            activeProject = imported
                            activeSceneId = imported.scenes.firstOrNull()?.id
                            currentScreen = ScreenState.SCENE_EDITOR
                            Toast.makeText(context, "Successfully imported project!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_LONG).show()
                        }
                    },
                    onLoadSample = { sampleProj ->
                        projects.add(0, sampleProj)
                        saveProjectsState()
                        activeProject = sampleProj
                        activeSceneId = sampleProj.scenes.firstOrNull()?.id
                        currentScreen = ScreenState.SCENE_EDITOR
                    }
                )
            }

            ScreenState.SCENE_EDITOR -> {
                if (activeProject != null && currentScene != null) {
                    SceneEditorScreen(
                        project = activeProject!!,
                        currentScene = currentScene,
                        selectedObjectId = selectedObjectId,
                        selectedButtonId = selectedButtonId,
                        onSelectObject = { id ->
                            selectedObjectId = id
                            if (id != null) selectedButtonId = null
                        },
                        onSelectButton = { id ->
                            selectedButtonId = id
                            if (id != null) selectedObjectId = null
                        },
                        onAddObject = {
                            val newObj = GameObject(
                                name = "Obj ${currentScene.objects.size + 1}",
                                x = 150f + (currentScene.objects.size * 20f),
                                y = 150f + (currentScene.objects.size * 20f),
                                width = 50f,
                                height = 50f
                            )
                            currentScene.objects.add(newObj)
                            selectedObjectId = newObj.id
                            selectedButtonId = null
                            saveProjectsState()
                        },
                        onAddUIButton = {
                            val newBtn = GameUIButton(
                                name = "Btn ${currentScene.uiButtons.size + 1}",
                                label = "BTN",
                                x = 80f + (currentScene.uiButtons.size * 20f),
                                y = 400f,
                                actionKey = "ACTION"
                            )
                            currentScene.uiButtons.add(newBtn)
                            selectedButtonId = newBtn.id
                            selectedObjectId = null
                            saveProjectsState()
                        },
                        onUpdateObject = {
                            saveProjectsState()
                        },
                        onUpdateUIButton = {
                            saveProjectsState()
                        },
                        onDeleteObject = { id ->
                            currentScene.objects.removeAll { it.id == id }
                            selectedObjectId = null
                            saveProjectsState()
                        },
                        onDeleteUIButton = { id ->
                            currentScene.uiButtons.removeAll { it.id == id }
                            selectedButtonId = null
                            saveProjectsState()
                        },
                        onOpenBlueprint = { obj ->
                            blueprintTargetObject = obj
                            currentScreen = ScreenState.BLUEPRINT_EDITOR
                        },
                        onOpenButtonBlueprint = {
                            // UI Button logic if needed
                        },
                        onOpenAnimationEditor = { obj ->
                            animationTargetObject = obj
                            currentScreen = ScreenState.ANIMATION_EDITOR
                        },
                        onPlayTest = {
                            currentScreen = ScreenState.PLAY
                        },
                        onOpenExport = {
                            showExportDialog = true
                        },
                        onSwitchScene = { newSceneId ->
                            activeSceneId = newSceneId
                            activeProject!!.activeSceneId = newSceneId
                            selectedObjectId = null
                            selectedButtonId = null
                            saveProjectsState()
                        },
                        onCreateScene = { name ->
                            val newSc = GameScene(name = name)
                            activeProject!!.scenes.add(newSc)
                            activeSceneId = newSc.id
                            activeProject!!.activeSceneId = newSc.id
                            selectedObjectId = null
                            selectedButtonId = null
                            saveProjectsState()
                        },
                        onCloneScene = { sc ->
                            val clonedSc = repository.cloneScene(sc)
                            activeProject!!.scenes.add(clonedSc)
                            activeSceneId = clonedSc.id
                            activeProject!!.activeSceneId = clonedSc.id
                            selectedObjectId = null
                            selectedButtonId = null
                            saveProjectsState()
                        },
                        onDeleteScene = { sc ->
                            if (activeProject!!.scenes.size > 1) {
                                activeProject!!.scenes.remove(sc)
                                activeSceneId = activeProject!!.scenes.first().id
                                activeProject!!.activeSceneId = activeSceneId!!
                                saveProjectsState()
                            }
                        },
                        onBackToHome = {
                            saveProjectsState()
                            currentScreen = ScreenState.HOME
                        }
                    )
                }
            }

            ScreenState.BLUEPRINT_EDITOR -> {
                if (blueprintTargetObject != null) {
                    BlueprintEditorScreen(
                        gameObject = blueprintTargetObject!!,
                        allObjectsInScene = currentScene?.objects ?: emptyList(),
                        allScenesInProject = activeProject?.scenes ?: emptyList(),
                        onBack = {
                            saveProjectsState()
                            currentScreen = ScreenState.SCENE_EDITOR
                        },
                        onSaveLogic = {
                            saveProjectsState()
                        }
                    )
                }
            }

            ScreenState.ANIMATION_EDITOR -> {
                if (animationTargetObject != null && activeProject != null) {
                    AnimationEditorScreen(
                        project = activeProject!!,
                        gameObject = animationTargetObject!!,
                        onBack = {
                            saveProjectsState()
                            currentScreen = ScreenState.SCENE_EDITOR
                        },
                        onSave = { updatedObj ->
                            saveProjectsState()
                        }
                    )
                }
            }

            ScreenState.PLAY -> {
                if (activeProject != null && currentScene != null) {
                    PlayGameScreen(
                        engine = gameEngine,
                        project = activeProject!!,
                        scene = currentScene,
                        onBackToEditor = {
                            currentScreen = ScreenState.SCENE_EDITOR
                        }
                    )
                }
            }
        }

        // Export Dialog Modal (Wireframe #4)
        if (showExportDialog && activeProject != null) {
            ExportDialog(
                project = activeProject!!,
                repository = repository,
                onDismiss = { showExportDialog = false }
            )
        }
    }
}
