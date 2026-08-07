package com.example.repository

import android.content.Context
import com.example.model.GameProject
import com.example.model.GameScene
import com.example.model.SampleProjects
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectRepository(private val context: Context) {

    private val projectsFile = File(context.filesDir, "flowmaker_projects.json")

    fun loadAllProjects(): List<GameProject> {
        if (!projectsFile.exists()) {
            val defaults = listOf(
                SampleProjects.createPlatformerProject(),
                SampleProjects.createSpaceShooterProject()
            )
            saveAllProjects(defaults)
            return defaults
        }

        return try {
            val jsonText = projectsFile.readText()
            val jsonArr = JSONArray(jsonText)
            val list = mutableListOf<GameProject>()
            for (i in 0 until jsonArr.length()) {
                val projObj = jsonArr.getJSONObject(i)
                list.add(GameProject.fromJsonObject(projObj))
            }
            if (list.isEmpty()) {
                val defaults = listOf(
                    SampleProjects.createPlatformerProject(),
                    SampleProjects.createSpaceShooterProject()
                )
                saveAllProjects(defaults)
                return defaults
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            val defaults = listOf(
                SampleProjects.createPlatformerProject(),
                SampleProjects.createSpaceShooterProject()
            )
            saveAllProjects(defaults)
            defaults
        }
    }

    fun saveAllProjects(projects: List<GameProject>) {
        try {
            val jsonArr = JSONArray()
            projects.forEach { jsonArr.put(it.toJsonObject()) }
            projectsFile.writeText(jsonArr.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cloneProject(project: GameProject): GameProject {
        val jsonStr = project.toJsonString()
        val clonedObj = JSONObject(jsonStr)
        clonedObj.put("id", UUID.randomUUID().toString())
        clonedObj.put("name", "${project.name} (Copy)")
        clonedObj.put("lastModified", System.currentTimeMillis())
        return GameProject.fromJsonObject(clonedObj)
    }

    fun cloneScene(scene: GameScene): GameScene {
        val jsonStr = scene.toJsonObject().toString()
        val clonedObj = JSONObject(jsonStr)
        clonedObj.put("id", UUID.randomUUID().toString())
        clonedObj.put("name", "${scene.name} (Copy)")
        return GameScene.fromJsonObject(clonedObj)
    }

    fun writeProjectZipToStream(project: GameProject, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zos ->
            val gameJson = project.toJsonString(2)

            // Add game_project.json
            addFileToZip(zos, "app/src/main/assets/game_project.json", gameJson.toByteArray())

            // Add build.gradle.kts
            val buildGradle = """
                plugins {
                    id("com.android.application")
                    id("org.jetbrains.kotlin.android")
                }
                android {
                    namespace = "com.flowmaker.game.${project.name.lowercase().filter { it.isLetterOrDigit() }}"
                    compileSdk = 34
                    defaultConfig {
                        applicationId = "com.flowmaker.game.${project.name.lowercase().filter { it.isLetterOrDigit() }}"
                        minSdk = 24
                        targetSdk = 34
                        versionCode = 1
                        versionName = "1.0"
                    }
                }
            """.trimIndent()
            addFileToZip(zos, "app/build.gradle.kts", buildGradle.toByteArray())

            // Add AndroidManifest.xml
            val manifest = """
                <?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                    <application
                        android:allowBackup="true"
                        android:label="${project.name}"
                        android:theme="@android:style/Theme.Material.Light.NoActionBar">
                        <activity
                            android:name=".MainActivity"
                            android:exported="true">
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                    </application>
                </manifest>
            """.trimIndent()
            addFileToZip(zos, "app/src/main/AndroidManifest.xml", manifest.toByteArray())

            // Add Readme instruction
            val readme = """
                # ${project.name} - Flow Maker 2D Android Export
                
                This zip contains a standalone Android source project generated by Flow Maker.
                
                ## How to compile:
                1. Unzip this package.
                2. Open the directory in Android Studio.
                3. Run/Build APK!
            """.trimIndent()
            addFileToZip(zos, "README.md", readme.toByteArray())
        }
    }

    fun exportProjectToZip(project: GameProject): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "${project.name.lowercase().replace(" ", "_")}_android.zip")

            FileOutputStream(zipFile).use { fos ->
                writeProjectZipToStream(project, fos)
            }
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, path: String, content: ByteArray) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        zos.write(content)
        zos.closeEntry()
    }
}
