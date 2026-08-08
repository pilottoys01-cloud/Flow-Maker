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

            // 1. Add game_project.json
            addFileToZip(zos, "game_project.json", gameJson.toByteArray(Charsets.UTF_8))

            // 2. Add index.html (HTML5 Game Engine)
            val htmlContent = generateHtmlGameBundle(project)
            addFileToZip(zos, "index.html", htmlContent.toByteArray(Charsets.UTF_8))

            // 3. Add README.md
            val readme = """
                # ${project.name} - HTML5 Game (Flow Maker Export)

                This ZIP package contains your game exported as a standalone HTML5 Web Game.

                ## How to Play:
                1. Extract/unzip this package.
                2. Double-click `index.html` to open and play the game in any web browser!
                3. You can also host `index.html` on web servers such as GitHub Pages, Netlify, or itch.io.
            """.trimIndent()
            addFileToZip(zos, "README.md", readme.toByteArray(Charsets.UTF_8))
        }
    }

    private fun generateHtmlGameBundle(project: GameProject): String {
        val jsonStr = project.toJsonString()
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                <title>${project.name}</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
                    body { background: #0f172a; color: #fff; font-family: system-ui, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; overflow: hidden; }
                    #canvas-container { position: relative; width: 100vw; max-width: 800px; height: 60vh; max-height: 480px; background: #1e293b; border: 3px solid #3b82f6; border-radius: 8px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); overflow: hidden; }
                    canvas { width: 100%; height: 100%; display: block; image-rendering: pixelated; }
                    #controls { display: flex; width: 100vw; max-width: 800px; justify-content: space-between; padding: 12.dp; margin-top: 10px; }
                    .dpad { display: grid; grid-template-columns: repeat(3, 50px); grid-template-rows: repeat(3, 50px); gap: 5px; }
                    .btn-act { width: 60px; height: 60px; background: #2563eb; color: #fff; font-weight: bold; font-size: 14px; border: 2px solid #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; touch-action: manipulation; }
                    .btn-act:active { background: #1d4ed8; transform: scale(0.95); }
                    .dpad-btn { background: #334155; color: #fff; font-weight: bold; border: 1px solid #64748b; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
                    .dpad-btn:active { background: #475569; }
                </style>
            </head>
            <body>
                <h1 style="font-size: 18px; margin-bottom: 8px; color: #38bdf8;">${project.name}</h1>
                <div id="canvas-container">
                    <canvas id="gameCanvas" width="800" height="480"></canvas>
                </div>
                <div id="controls">
                    <div class="dpad">
                        <div></div><div class="dpad-btn" id="btn-up">▲</div><div></div>
                        <div class="dpad-btn" id="btn-left">◀</div><div></div><div class="dpad-btn" id="btn-right">▶</div>
                        <div></div><div class="dpad-btn" id="btn-down">▼</div><div></div>
                    </div>
                    <div style="display: flex; gap: 10px; align-items: center;">
                        <div class="btn-act" id="btn-shoot" style="background:#f43f5e;">SHOOT</div>
                        <div class="btn-act" id="btn-jump" style="background:#10b981;">JUMP</div>
                    </div>
                </div>

                <script>
                    const GAME_DATA = $jsonStr;
                    const canvas = document.getElementById('gameCanvas');
                    const ctx = canvas.getContext('2d');
                    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();

                    function playTone(type) {
                        try {
                            const osc = audioCtx.createOscillator();
                            const gain = audioCtx.createGain();
                            osc.connect(gain);
                            gain.connect(audioCtx.destination);
                            const now = audioCtx.currentTime;
                            if (type === 'JUMP') {
                                osc.frequency.setValueAtTime(300, now);
                                osc.frequency.exponentialRampToValueAtTime(600, now + 0.15);
                                gain.gain.setValueAtTime(0.2, now);
                                gain.gain.exponentialRampToValueAtTime(0.01, now + 0.15);
                                osc.start(now); osc.stop(now + 0.15);
                            } else if (type === 'SHOOT') {
                                osc.frequency.setValueAtTime(800, now);
                                osc.frequency.exponentialRampToValueAtTime(100, now + 0.1);
                                gain.gain.setValueAtTime(0.2, now);
                                gain.gain.exponentialRampToValueAtTime(0.01, now + 0.1);
                                osc.start(now); osc.stop(now + 0.1);
                            } else if (type === 'COIN') {
                                osc.frequency.setValueAtTime(987.77, now);
                                osc.frequency.setValueAtTime(1318.51, now + 0.08);
                                gain.gain.setValueAtTime(0.2, now);
                                gain.gain.exponentialRampToValueAtTime(0.01, now + 0.25);
                                osc.start(now); osc.stop(now + 0.25);
                            } else {
                                osc.frequency.setValueAtTime(200, now);
                                gain.gain.setValueAtTime(0.2, now);
                                gain.gain.exponentialRampToValueAtTime(0.01, now + 0.1);
                                osc.start(now); osc.stop(now + 0.1);
                            }
                        } catch(e) {}
                    }

                    let activeScene = GAME_DATA.scenes.find(s => s.id === GAME_DATA.activeSceneId) || GAME_DATA.scenes[0];
                    let objects = JSON.parse(JSON.stringify(activeScene ? activeScene.objects : []));
                    let globalVars = {};
                    (GAME_DATA.globalVariables || []).forEach(v => globalVars[v.name] = v.value);

                    const keys = {};
                    window.addEventListener('keydown', e => { keys[e.key] = true; if (audioCtx.state === 'suspended') audioCtx.resume(); });
                    window.addEventListener('keyup', e => { keys[e.key] = false; });

                    function setupTouch(id, key) {
                        const el = document.getElementById(id);
                        if (!el) return;
                        el.addEventListener('touchstart', (e) => { e.preventDefault(); keys[key] = true; if (audioCtx.state === 'suspended') audioCtx.resume(); });
                        el.addEventListener('touchend', (e) => { e.preventDefault(); keys[key] = false; });
                    }
                    setupTouch('btn-left', 'ArrowLeft');
                    setupTouch('btn-right', 'ArrowRight');
                    setupTouch('btn-up', 'ArrowUp');
                    setupTouch('btn-down', 'ArrowDown');
                    setupTouch('btn-jump', 'Space');
                    setupTouch('btn-shoot', 'Control');

                    let lastTime = performance.now();
                    function gameLoop(now) {
                        const dt = Math.min((now - lastTime) / 1000, 0.05);
                        lastTime = now;

                        // Update objects
                        objects.forEach(obj => {
                            if (!obj.vx) obj.vx = 0;
                            if (!obj.vy) obj.vy = 0;

                            if (obj.tag === 'Player') {
                                if (keys['ArrowLeft'] || keys['a']) obj.vx = -180;
                                else if (keys['ArrowRight'] || keys['d']) obj.vx = 180;
                                else obj.vx = 0;

                                if ((keys['Space'] || keys['ArrowUp'] || keys['w']) && obj.isGrounded) {
                                    obj.vy = -350;
                                    obj.isGrounded = false;
                                    playTone('JUMP');
                                }
                            }

                            if (obj.hasPhysics && !obj.isStatic) {
                                obj.vy += 600 * dt;
                            }

                            obj.x += obj.vx * dt;
                            obj.y += obj.vy * dt;

                            // Floor collision
                            if (obj.y + obj.height >= 400) {
                                obj.y = 400 - obj.height;
                                obj.vy = 0;
                                obj.isGrounded = true;
                            }
                        });

                        // Draw
                        ctx.fillStyle = activeScene.backgroundColorHex || '#1e293b';
                        ctx.fillRect(0, 0, canvas.width, canvas.height);

                        // Ground line
                        ctx.fillStyle = '#10b981';
                        ctx.fillRect(0, 400, canvas.width, 80);

                        objects.forEach(obj => {
                            ctx.fillStyle = obj.colorHex || '#3b82f6';
                            ctx.fillRect(obj.x, obj.y, obj.width, obj.height);
                            ctx.strokeStyle = '#000';
                            ctx.strokeRect(obj.x, obj.y, obj.width, obj.height);

                            ctx.fillStyle = '#fff';
                            ctx.font = '10px sans-serif';
                            ctx.fillText(obj.name, obj.x + 2, obj.y + 12);
                        });

                        requestAnimationFrame(gameLoop);
                    }
                    requestAnimationFrame(gameLoop);
                </script>
            </body>
            </html>
        """.trimIndent()
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
