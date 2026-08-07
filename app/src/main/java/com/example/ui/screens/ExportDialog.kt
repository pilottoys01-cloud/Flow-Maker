package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.FileProvider
import com.example.model.GameProject
import com.example.repository.ProjectRepository

@Composable
fun ExportDialog(
    project: GameProject,
    repository: ProjectRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var exportStatusText by remember { mutableStateOf("") }

    // System Folder Picker for Android ZIP
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    repository.writeProjectZipToStream(project, outputStream)
                }
                exportStatusText = "Guardado ZIP correctamente en la carpeta seleccionada!"
                Toast.makeText(context, "ZIP guardado exitosamente!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                exportStatusText = "Error al guardar el archivo ZIP: ${e.message}"
            }
        }
    }

    // System Folder Picker for JSON File
    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(project.toJsonString(2).toByteArray(Charsets.UTF_8))
                }
                exportStatusText = "Guardado JSON correctamente en la carpeta seleccionada!"
                Toast.makeText(context, "JSON guardado exitosamente!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                exportStatusText = "Error al guardar el archivo JSON: ${e.message}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF262626),
        shape = RoundedCornerShape(12.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // EXPORT GAME Title
                Text(
                    text = "EXPORT\nGAME",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    lineHeight = 34.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // ANDROID ZIP - SELECT FOLDER BUTTON
                Button(
                    onClick = {
                        val fileName = "${project.name.lowercase().replace(" ", "_")}_android.zip"
                        createZipLauncher.launch(fileName)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                        .testTag("export_android_zip_button")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GUARDAR ZIP EN CARPETA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // EXPORT JSON - SELECT FOLDER BUTTON
                Button(
                    onClick = {
                        val fileName = "${project.name.lowercase().replace(" ", "_")}_project.json"
                        createJsonLauncher.launch(fileName)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                        .testTag("export_json_button")
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GUARDAR JSON EN CARPETA",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // SECONDARY UTILITIES (Share & Copy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share Zip File
                    OutlinedButton(
                        onClick = {
                            val zipFile = repository.exportProjectToZip(project)
                            if (zipFile != null && zipFile.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        zipFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir ZIP"))
                                    exportStatusText = "Abriendo menú de compartir..."
                                } catch (e: Exception) {
                                    exportStatusText = "Generado ZIP en cache: ${zipFile.absolutePath}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Compartir", fontSize = 12.sp)
                    }

                    // Copy JSON
                    OutlinedButton(
                        onClick = {
                            val jsonStr = project.toJsonString(2)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Flow Maker Game JSON", jsonStr)
                            clipboard.setPrimaryClip(clip)
                            exportStatusText = "¡JSON copiado al Portapapeles!"
                            Toast.makeText(context, "¡Copiado al portapapeles!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Copiar JSON", fontSize = 12.sp)
                    }
                }

                if (exportStatusText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = exportStatusText,
                            fontSize = 12.sp,
                            color = Color.Cyan
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Cerrar", fontWeight = FontWeight.Bold)
            }
        }
    )
}

