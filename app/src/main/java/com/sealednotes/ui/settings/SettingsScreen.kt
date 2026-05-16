package com.sealednotes.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.sealednotes.viewmodel.DbTransferState
import com.sealednotes.viewmodel.ImportState
import com.sealednotes.viewmodel.KeyExportState
import com.sealednotes.viewmodel.KeyImportState
import com.sealednotes.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val keyExport by viewModel.keyExport.collectAsStateWithLifecycle()
    val keyImport by viewModel.keyImport.collectAsStateWithLifecycle()
    val dbState by viewModel.dbState.collectAsStateWithLifecycle()

    val zipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> uri?.let(viewModel::importZip) }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? -> uri?.let(viewModel::importFolder) }

    val exportDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? -> uri?.let(viewModel::exportDatabase) }

    val importDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> uri?.let(viewModel::importDatabase) }

    val qrScanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
    ) { result ->
        result.contents?.let { viewModel.applyScannedKey(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Backup & Restore")
            SettingItem(
                icon = Icons.Outlined.Key,
                title = "Export encryption key",
                subtitle = "View as text or QR code. Keep this somewhere safe.",
                onClick = viewModel::showKeyExport,
            )
            SettingItem(
                icon = Icons.Outlined.Download,
                title = "Export database",
                subtitle = "Save the encrypted .db file.",
                onClick = {
                    exportDbLauncher.launch("sealednotes-backup.db")
                },
            )
            SettingItem(
                icon = Icons.Outlined.QrCodeScanner,
                title = "Import encryption key",
                subtitle = "Paste or scan the key from another device.",
                onClick = viewModel::showKeyImport,
            )
            SettingItem(
                icon = Icons.Outlined.Upload,
                title = "Import database",
                subtitle = "Replace local notes with an encrypted .db file.",
                onClick = {
                    importDbLauncher.launch(arrayOf("*/*"))
                },
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("Import from Google Keep")
            SettingItem(
                icon = Icons.Outlined.FileUpload,
                title = "Import from Google Takeout (.zip)",
                subtitle = "Pick the Takeout zip directly; Keep notes are imported.",
                onClick = {
                    zipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                },
            )
            SettingItem(
                icon = Icons.Outlined.FolderOpen,
                title = "Import from a Keep folder",
                subtitle = "Pick a folder that contains the Keep JSON files.",
                onClick = { folderLauncher.launch(null) },
            )
            Text(
                text = "Reminders are not stored in Keep's Takeout export, so imported notes start without reminders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Keep import dialogs (unchanged)
        when (val s = importState) {
            ImportState.Idle -> Unit
            is ImportState.Running -> ImportProgressDialog(s.imported, s.skipped)
            is ImportState.Done -> ImportResultDialog(s, viewModel::dismissResult)
            is ImportState.Error -> ImportErrorDialog(s.message, viewModel::dismissResult)
        }

        if (keyExport.visible) {
            KeyExportDialog(state = keyExport, onDismiss = viewModel::hideKeyExport)
        }
        if (keyImport.visible) {
            KeyImportDialog(
                state = keyImport,
                onDraftChange = viewModel::updateKeyImportDraft,
                onSubmit = viewModel::submitKeyImport,
                onScan = {
                    qrScanLauncher.launch(ScanOptions().apply {
                        setPrompt("Scan SealedNotes encryption key")
                        setOrientationLocked(false)
                        setBeepEnabled(false)
                    })
                },
                onDismiss = viewModel::hideKeyImport,
            )
        }

        when (dbState) {
            DbTransferState.Idle -> Unit
            DbTransferState.ExportRunning -> InProgressDialog("Exporting database…")
            DbTransferState.ImportRunning -> InProgressDialog("Importing database…")
            DbTransferState.ExportDone -> SimpleResultDialog(
                title = "Database exported",
                message = "The encrypted .db file was saved.",
                onDismiss = viewModel::dismissDbState,
            )
            DbTransferState.NeedsRestart -> RestartRequiredDialog(onRestart = {
                viewModel.dismissDbState()
                killProcess()
            })
            is DbTransferState.Error -> ImportErrorDialog(
                (dbState as DbTransferState.Error).message,
                viewModel::dismissDbState,
            )
        }
    }
}

private fun killProcess() {
    android.os.Process.killProcess(android.os.Process.myPid())
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImportProgressDialog(imported: Int, skipped: Int) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Importing…", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("$imported imported, $skipped skipped")
            }
        }
    }
}

@Composable
private fun InProgressDialog(label: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ImportResultDialog(state: ImportState.Done, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Import complete", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text("${state.imported} notes imported", style = MaterialTheme.typography.bodyLarge)
                if (state.skipped > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${state.skipped} skipped:", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        state.reasons.forEach { reason ->
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("OK") }
                }
            }
        }
    }
}

@Composable
private fun ImportErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Something went wrong") },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun SimpleResultDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun RestartRequiredDialog(onRestart: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Restart required") },
        text = {
            Text(
                "The app needs to restart so the new key or database takes effect. " +
                    "Notes won't be readable until the key and database match.",
            )
        },
        confirmButton = { TextButton(onClick = onRestart) { Text("Restart now") } },
    )
}

@Composable
private fun KeyExportDialog(state: KeyExportState, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Your encryption key", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Without this key, your exported database cannot be restored. Save it somewhere only you can access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))

                state.qrBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Key as QR code",
                            modifier = Modifier.size(240.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = state.keyText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(state.keyText))
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun KeyImportDialog(
    state: KeyImportState,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import encryption key") },
        text = {
            Column {
                Text(
                    "Paste the Base64 key from another device, or scan the QR code.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text("Base64 key") },
                    isError = state.error != null,
                    supportingText = state.error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onScan) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan QR code")
                }
            }
        },
        confirmButton = { TextButton(onClick = onSubmit) { Text("Import") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
