package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.engine.RetroAudioEngine
import com.example.model.GameObject
import com.example.model.GameProject
import com.example.model.MediaType
import com.example.model.ProjectMediaAsset

@Composable
fun MediaLibraryDialog(
    project: GameProject,
    activeObject: GameObject? = null,
    onDismiss: () -> Unit,
    onSelectAssetForObject: ((ProjectMediaAsset) -> Unit)? = null,
    onProjectUpdated: () -> Unit
) {
    val context = LocalContext.current
    var selectedTabIdx by remember { mutableStateOf(0) } // 0: Todos, 1: Imágenes, 2: GIFs, 3: Audios
    var showRenameDialog by remember { mutableStateOf<ProjectMediaAsset?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Media file pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: ""
            val isGif = mimeType.contains("gif", ignoreCase = true) || it.toString().endsWith(".gif", ignoreCase = true)
            val name = it.lastPathSegment?.substringAfterLast("/") ?: "Imagen_${System.currentTimeMillis() % 1000}"
            val asset = ProjectMediaAsset(
                name = if (isGif && !name.endsWith(".gif")) "$name.gif" else name,
                uri = it.toString(),
                type = if (isGif) MediaType.GIF else MediaType.IMAGE
            )
            project.mediaAssets.add(asset)
            onProjectUpdated()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast("/") ?: "Audio_${System.currentTimeMillis() % 1000}"
            val asset = ProjectMediaAsset(
                name = name,
                uri = it.toString(),
                type = MediaType.AUDIO
            )
            project.mediaAssets.add(asset)
            onProjectUpdated()
        }
    }

    // Filtered media list
    val mediaList = remember(project.mediaAssets.size, selectedTabIdx) {
        when (selectedTabIdx) {
            1 -> project.mediaAssets.filter { it.type == MediaType.IMAGE }
            2 -> project.mediaAssets.filter { it.type == MediaType.GIF }
            3 -> project.mediaAssets.filter { it.type == MediaType.AUDIO }
            else -> project.mediaAssets.toList()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .testTag("media_library_dialog"),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📁 BIBLIOTECA DE MEDIOS / ASSETS",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${project.mediaAssets.size} Archivos)",
                            fontSize = 12.sp,
                            color = Color.Cyan
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.LightGray)
                    }
                }

                // Filter Tabs
                val tabTitles = listOf("TODOS", "🖼️ IMÁGENES", "🎞️ GIFs", "🎵 AUDIOS")
                TabRow(
                    selectedTabIndex = selectedTabIdx,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.Cyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .padding(bottom = 12.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIdx == index,
                            onClick = { selectedTabIdx = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTabIdx == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIdx == index) Color.Cyan else Color.LightGray
                                )
                            }
                        )
                    }
                }

                // Upload Actions Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("upload_image_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Subir Imagen / GIF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("upload_audio_button")
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Subir Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            // Load high-quality sample media assets
                            val sampleMedia = listOf(
                                ProjectMediaAsset(
                                    name = "hero_knight.gif",
                                    uri = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExOHJxb3c0NXE2dWx0NDl6bW5ldThwNmIwdGN5cDFtbmsxbzByYmg0NyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/k33A4e7Gv7wL6/giphy.gif",
                                    type = MediaType.GIF
                                ),
                                ProjectMediaAsset(
                                    name = "retro_coin_spin.gif",
                                    uri = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExN3Zxa2psbm1ld3BxNW90NzlqOHdqYXV5dDFocThxaTVyYXk4Mml5NCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/13A9fO2eY2C5G0/giphy.gif",
                                    type = MediaType.GIF
                                ),
                                ProjectMediaAsset(
                                    name = "pixel_fireball.png",
                                    uri = "https://images.rawpixel.com/image_png_800/cHJpdmF0ZS9sci9pbWFnZXMvd2Vic2l0ZS8yMDIzLTA4L3Jhd3BpeGVsX29mZmljZV8yOF9waXhlbF9hcnRfb2ZfYV9maXJlX2ljb25faXNvbGF0ZWRfb25fX3doaXRlX2JhXzkzOGIzMzc5LWFlNjYtNGM2Ni05YThhLTgyNjM2OWE3MzU4Ml8xLnBuZw.png",
                                    type = MediaType.IMAGE
                                ),
                                ProjectMediaAsset(
                                    name = "coin_pickup.wav",
                                    uri = "sample_audio_coin",
                                    type = MediaType.AUDIO
                                ),
                                ProjectMediaAsset(
                                    name = "laser_shoot.wav",
                                    uri = "sample_audio_laser",
                                    type = MediaType.AUDIO
                                )
                            )
                            sampleMedia.forEach { sample ->
                                if (project.mediaAssets.none { it.name == sample.name }) {
                                    project.mediaAssets.add(sample)
                                }
                            }
                            onProjectUpdated()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("✨ Cargar Ejemplos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Grid of Media Assets
                if (mediaList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay medios subidos en esta categoría",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pulsa '+ Subir Imagen / GIF' o '+ Subir Audio' arriba",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(mediaList, key = { it.id }) { asset ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Media Preview Box
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0F172A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (asset.type) {
                                            MediaType.IMAGE, MediaType.GIF -> {
                                                if (asset.uri.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = asset.uri,
                                                        contentDescription = asset.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = if (asset.type == MediaType.GIF) Icons.Default.Gif else Icons.Default.Image,
                                                        contentDescription = null,
                                                        tint = Color.Cyan,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                            }
                                            MediaType.AUDIO -> {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AudioFile,
                                                        contentDescription = null,
                                                        tint = Color(0xFFEC4899),
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Button(
                                                        onClick = {
                                                            RetroAudioEngine.playSound("COIN")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(12.dp))
                                                        Text("Probar", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        // Type Badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(2.dp)
                                                .background(
                                                    when (asset.type) {
                                                        MediaType.IMAGE -> Color(0xFF2563EB)
                                                        MediaType.GIF -> Color(0xFF10B981)
                                                        MediaType.AUDIO -> Color(0xFFEC4899)
                                                    },
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = asset.type.name,
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Asset Name Text
                                    Text(
                                        text = asset.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Assign or Action Buttons
                                    if (onSelectAssetForObject != null && activeObject != null && (asset.type == MediaType.IMAGE || asset.type == MediaType.GIF)) {
                                        Button(
                                            onClick = {
                                                onSelectAssetForObject(asset)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.fillMaxWidth().height(26.dp)
                                        ) {
                                            Text("Usar Sprite", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        IconButton(
                                            onClick = {
                                                showRenameDialog = asset
                                                renameText = asset.name
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Renombrar", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                project.mediaAssets.remove(asset)
                                                onProjectUpdated()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Close Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Cerrar", color = Color.White)
                    }
                }
            }
        }
    }

    // Rename Asset Dialog
    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Renombrar Medio / Asset", color = Color.White, fontSize = 14.sp) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameDialog?.name = renameText.ifBlank { "Asset" }
                        showRenameDialog = null
                        onProjectUpdated()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
