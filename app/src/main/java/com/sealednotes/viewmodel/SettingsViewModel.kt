package com.sealednotes.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sealednotes.SealedNotesApp
import com.sealednotes.crypto.KeyManager
import com.sealednotes.crypto.QRCode
import com.sealednotes.io.DatabaseTransfer
import com.sealednotes.io.KeepImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ImportState {
    object Idle : ImportState()
    data class Running(val imported: Int, val skipped: Int) : ImportState()
    data class Done(val imported: Int, val skipped: Int, val reasons: List<String>) : ImportState()
    data class Error(val message: String) : ImportState()
}

data class KeyExportState(
    val visible: Boolean = false,
    val keyText: String = "",
    val qrBitmap: Bitmap? = null,
)

data class KeyImportState(
    val visible: Boolean = false,
    val draft: String = "",
    val error: String? = null,
)

sealed class DbTransferState {
    object Idle : DbTransferState()
    object ExportRunning : DbTransferState()
    object ExportDone : DbTransferState()
    object ImportRunning : DbTransferState()
    /** Successful key OR DB import — app must restart for changes to take effect. */
    object NeedsRestart : DbTransferState()
    data class Error(val message: String) : DbTransferState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val keepImporter = KeepImporter(app, (app as SealedNotesApp).repository)
    private val dbTransfer = DatabaseTransfer(app)
    private val keyManager = KeyManager(app)

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _keyExport = MutableStateFlow(KeyExportState())
    val keyExport: StateFlow<KeyExportState> = _keyExport.asStateFlow()

    private val _keyImport = MutableStateFlow(KeyImportState())
    val keyImport: StateFlow<KeyImportState> = _keyImport.asStateFlow()

    private val _dbState = MutableStateFlow<DbTransferState>(DbTransferState.Idle)
    val dbState: StateFlow<DbTransferState> = _dbState.asStateFlow()

    // ── Keep import (unchanged) ──

    fun importZip(uri: Uri) {
        if (_importState.value is ImportState.Running) return
        _importState.value = ImportState.Running(0, 0)
        viewModelScope.launch {
            try {
                val result = keepImporter.importZip(uri) { imported, skipped ->
                    _importState.value = ImportState.Running(imported, skipped)
                }
                _importState.value = ImportState.Done(result.imported, result.skipped, result.skippedReasons)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun importFolder(uri: Uri) {
        if (_importState.value is ImportState.Running) return
        _importState.value = ImportState.Running(0, 0)
        viewModelScope.launch {
            try {
                val result = keepImporter.importFolder(uri) { imported, skipped ->
                    _importState.value = ImportState.Running(imported, skipped)
                }
                _importState.value = ImportState.Done(result.imported, result.skipped, result.skippedReasons)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun dismissResult() {
        _importState.value = ImportState.Idle
    }

    // ── Key export ──

    fun showKeyExport() {
        viewModelScope.launch {
            val text = keyManager.exportPassphraseAsString()
            val qr = withContext(Dispatchers.Default) { QRCode.encode(text) }
            _keyExport.value = KeyExportState(visible = true, keyText = text, qrBitmap = qr)
        }
    }

    fun hideKeyExport() {
        _keyExport.value = KeyExportState()
    }

    // ── Key import ──

    fun showKeyImport() {
        _keyImport.value = KeyImportState(visible = true)
    }

    fun hideKeyImport() {
        _keyImport.value = KeyImportState()
    }

    fun updateKeyImportDraft(value: String) {
        _keyImport.value = _keyImport.value.copy(draft = value, error = null)
    }

    fun submitKeyImport() {
        val draft = _keyImport.value.draft.trim()
        if (draft.isEmpty()) {
            _keyImport.value = _keyImport.value.copy(error = "Paste or scan a key first")
            return
        }
        val ok = keyManager.importPassphraseFromString(draft)
        if (!ok) {
            _keyImport.value = _keyImport.value.copy(
                error = "Invalid key — should be a 32-byte Base64 string",
            )
            return
        }
        _keyImport.value = KeyImportState()
        _dbState.value = DbTransferState.NeedsRestart
    }

    /** Called when the QR scanner returns a value. */
    fun applyScannedKey(scanned: String) {
        _keyImport.value = KeyImportState(visible = true, draft = scanned)
        submitKeyImport()
    }

    // ── DB export / import ──

    fun exportDatabase(uri: Uri) {
        if (_dbState.value is DbTransferState.ExportRunning ||
            _dbState.value is DbTransferState.ImportRunning) return
        _dbState.value = DbTransferState.ExportRunning
        viewModelScope.launch {
            try {
                dbTransfer.exportTo(uri)
                _dbState.value = DbTransferState.ExportDone
            } catch (e: Exception) {
                _dbState.value = DbTransferState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun importDatabase(uri: Uri) {
        if (_dbState.value is DbTransferState.ExportRunning ||
            _dbState.value is DbTransferState.ImportRunning) return
        _dbState.value = DbTransferState.ImportRunning
        viewModelScope.launch {
            try {
                dbTransfer.importFrom(uri)
                _dbState.value = DbTransferState.NeedsRestart
            } catch (e: Exception) {
                _dbState.value = DbTransferState.Error(e.message ?: "Import failed")
            }
        }
    }

    fun dismissDbState() {
        _dbState.value = DbTransferState.Idle
    }
}
