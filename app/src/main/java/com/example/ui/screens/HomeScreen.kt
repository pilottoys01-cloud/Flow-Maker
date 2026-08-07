package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameProject
import com.example.model.GameScene
import com.example.model.SampleProjects

@Composable
fun HomeScreen(
    projects: List<GameProject>,
    onSelectProject: (GameProject) -> Unit,
    onCreateProject: (String) -> Unit,
    onCloneProject: (GameProject) -> Unit,
    onDeleteProject: (GameProject) -> Unit,
    onImportJson: (String) -> Unit,
    onLoadSample: (GameProject) -> Unit
) {
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var jsonInputText by remember { mutableStateOf("") }

    val jsonFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Picked json file
        }
    }

    // Matching wireframe #5 layout: Left side title + project list, Right side sidebar MAKE / IMPORT / COMING SOON buttons
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF262626)) // Dark wireframe background
    ) {
        // Left Panel (FLOW MAKER Title & Game cards list)
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            Text(
                text = "FLOW\nMAKER",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 42.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No projects yet. Tap MAKE to create your first 2D game!", color = Color.LightGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(projects, key = { it.id }) { proj ->
                        // Wireframe #5 Game item card with clone icon and 'X' delete button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF333333), RoundedCornerShape(6.dp))
                                .border(2.dp, Color.Black, RoundedCornerShape(6.dp))
                                .clickable { onSelectProject(proj) }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Games,
                                        contentDescription = null,
                                        tint = Color.Cyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = proj.name.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${proj.scenes.size} Scenes • ${proj.globalVariables.size} Vars",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Clone / Duplicate icon matching wireframe #5 box icon
                                    IconButton(
                                        onClick = { onCloneProject(proj) },
                                        modifier = Modifier.testTag("clone_project_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Clone Project",
                                            tint = Color.LightGray
                                        )
                                    }

                                    // Delete 'X' button matching wireframe #5 [X]
                                    IconButton(
                                        onClick = { onDeleteProject(proj) },
                                        modifier = Modifier.testTag("delete_project_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete Project",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Vertical Divider line matching wireframe #5
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.Black)
        )

        // Right Panel matching wireframe #5 sidebar buttons (MAKE, IMPORT, SAMPLES)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            // MAKE Button
            Button(
                onClick = {
                    newProjectName = "Game ${projects.size + 1}"
                    showNewProjectDialog = true
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .testTag("make_project_button")
            ) {
                Text(
                    text = "MAKE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // IMPORT Button
            Button(
                onClick = { showImportJsonDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .testTag("import_project_button")
            ) {
                Text(
                    text = "IMPORT",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // SAMPLES Button (Wireframe COMING SOON / PRESET TEMPLATES)
            Button(
                onClick = {
                    val sample = SampleProjects.createPlatformerProject()
                    onLoadSample(sample)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .border(2.dp, Color.Cyan, RoundedCornerShape(8.dp))
                    .testTag("samples_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLATFORMER SAMPLE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Cyan
                    )
                    Text(
                        text = "Instant 2D Jump Game",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }

            Button(
                onClick = {
                    val sample = SampleProjects.createSpaceShooterProject()
                    onLoadSample(sample)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .border(2.dp, Color(0xFFF43F5E), RoundedCornerShape(8.dp))
                    .testTag("shooter_sample_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SPACE SHOOTER SAMPLE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF43F5E)
                    )
                    Text(
                        text = "Ships, Bullets & Enemies",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Create New 2D Game") },
            text = {
                Column {
                    Text("Enter project name:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_project_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            onCreateProject(newProjectName)
                            showNewProjectDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_project_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text("Import Game Project JSON") },
            text = {
                Column {
                    Text("Paste JSON content:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonInputText,
                        onValueChange = { jsonInputText = it },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("json_import_text_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonInputText.isNotBlank()) {
                            onImportJson(jsonInputText)
                            showImportJsonDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_import_json_button")
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
