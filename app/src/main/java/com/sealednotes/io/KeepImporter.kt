package com.sealednotes.io

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.sealednotes.data.entity.ChecklistItem
import com.sealednotes.data.entity.Label
import com.sealednotes.data.entity.Note
import com.sealednotes.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val skippedReasons: List<String>,
)

/**
 * Imports notes from a Google Takeout export of Google Keep.
 * Accepts either the raw .zip file or an extracted folder containing the JSON files.
 *
 * Only `.json` files under a `Keep/` directory are considered. The companion `.html`
 * files Keep also exports are ignored. Trashed notes and attachment-only notes are skipped.
 *
 * Keep's Takeout JSON does not contain reminder data, so imported notes start without reminders.
 */
class KeepImporter(
    private val app: Application,
    private val repository: NoteRepository,
) {

    suspend fun importZip(
        uri: Uri,
        onProgress: suspend (imported: Int, skipped: Int) -> Unit = { _, _ -> },
    ): ImportResult = withContext(Dispatchers.IO) {
        val stream = app.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Could not open file")
        stream.use { importFromZipStream(it, onProgress) }
    }

    suspend fun importFolder(
        uri: Uri,
        onProgress: suspend (imported: Int, skipped: Int) -> Unit = { _, _ -> },
    ): ImportResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(app, uri)
            ?: throw IllegalArgumentException("Could not open folder")
        val jsonFiles = mutableListOf<DocumentFile>()
        collectJsonFiles(root, jsonFiles)
        importFromJsonFiles(jsonFiles, onProgress)
    }

    private fun collectJsonFiles(dir: DocumentFile, out: MutableList<DocumentFile>) {
        dir.listFiles().forEach { child ->
            if (child.isDirectory) {
                collectJsonFiles(child, out)
            } else if (child.name?.endsWith(".json", ignoreCase = true) == true) {
                out.add(child)
            }
        }
    }

    private suspend fun importFromZipStream(
        stream: InputStream,
        onProgress: suspend (Int, Int) -> Unit,
    ): ImportResult {
        val labelCache = preloadLabels()
        var imported = 0
        var skipped = 0
        val reasons = mutableListOf<String>()

        ZipInputStream(stream).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                try {
                    if (entry.isDirectory || !isKeepJsonEntry(entry.name)) continue
                    val text = String(zip.readBytes(), Charsets.UTF_8)
                    val outcome = importOne(text, labelCache)
                    if (outcome != null) {
                        skipped++
                        reasons.add("${entry.name}: $outcome")
                    } else {
                        imported++
                    }
                    onProgress(imported, skipped)
                } finally {
                    zip.closeEntry()
                }
            }
        }
        return ImportResult(imported, skipped, reasons.take(MAX_REASONS_RETURNED))
    }

    private suspend fun importFromJsonFiles(
        files: List<DocumentFile>,
        onProgress: suspend (Int, Int) -> Unit,
    ): ImportResult {
        val labelCache = preloadLabels()
        var imported = 0
        var skipped = 0
        val reasons = mutableListOf<String>()

        files.forEach { file ->
            val text = try {
                app.contentResolver.openInputStream(file.uri)?.use {
                    String(it.readBytes(), Charsets.UTF_8)
                }
            } catch (e: Exception) {
                null
            }
            if (text == null) {
                skipped++
                reasons.add("${file.name ?: "?"}: could not read")
                onProgress(imported, skipped)
                return@forEach
            }
            val outcome = importOne(text, labelCache)
            if (outcome != null) {
                skipped++
                reasons.add("${file.name ?: "?"}: $outcome")
            } else {
                imported++
            }
            onProgress(imported, skipped)
        }
        return ImportResult(imported, skipped, reasons.take(MAX_REASONS_RETURNED))
    }

    private fun isKeepJsonEntry(name: String): Boolean {
        if (!name.endsWith(".json", ignoreCase = true)) return false
        return name.contains("/Keep/", ignoreCase = true) || name.startsWith("Keep/", ignoreCase = true)
    }

    private suspend fun preloadLabels(): MutableMap<String, Long> {
        val cache = HashMap<String, Long>()
        repository.getAllLabels().first().forEach { cache[it.name.lowercase()] = it.id }
        return cache
    }

    private suspend fun getOrCreateLabel(name: String, cache: MutableMap<String, Long>): Long {
        val key = name.lowercase()
        cache[key]?.let { return it }
        val id = repository.createLabel(Label(name = name))
        cache[key] = id
        return id
    }

    /**
     * Imports one note. Returns null on success, or a short skip-reason string.
     */
    private suspend fun importOne(json: String, labelCache: MutableMap<String, Long>): String? {
        val obj = try {
            JSONObject(json)
        } catch (e: Exception) {
            return "malformed JSON"
        }

        if (obj.optBoolean("isTrashed", false)) return "in Keep trash"

        val title = obj.safeString("title")
        val textContent = obj.safeString("textContent")
        val listJson = obj.optJSONArray("listContent")
        val isList = listJson != null && listJson.length() > 0

        val checklistItems = if (isList) {
            (0 until listJson!!.length()).mapNotNull { i ->
                val item = listJson.optJSONObject(i) ?: return@mapNotNull null
                val text = item.safeString("text").trim()
                if (text.isEmpty()) null
                else ChecklistDraft(text = text, isChecked = item.optBoolean("isChecked", false))
            }
        } else emptyList()

        if (title.isBlank() && textContent.isBlank() && checklistItems.isEmpty()) {
            val hasAttachments = (obj.optJSONArray("attachments")?.length() ?: 0) > 0
            return if (hasAttachments) "attachment-only note (images not imported)" else "empty"
        }

        val createdUsec = obj.optLong("createdTimestampUsec", 0L)
        val updatedUsec = obj.optLong("userEditedTimestampUsec", 0L)
        val now = System.currentTimeMillis()
        val createdMs = if (createdUsec > 0) createdUsec / 1000 else now
        val updatedMs = if (updatedUsec > 0) updatedUsec / 1000 else createdMs

        val note = Note(
            title = title,
            content = if (isList) "" else textContent,
            isChecklist = isList,
            isPinned = obj.optBoolean("isPinned", false),
            isArchived = obj.optBoolean("isArchived", false),
            isTrashed = false,
            createdAt = createdMs,
            updatedAt = updatedMs,
        )
        val newId = repository.createNote(note)

        if (isList) {
            repository.addChecklistItems(checklistItems.mapIndexed { index, draft ->
                ChecklistItem(
                    noteId = newId,
                    content = draft.text,
                    isChecked = draft.isChecked,
                    sortOrder = index,
                )
            })
        }

        val labelsArr = obj.optJSONArray("labels")
        if (labelsArr != null) {
            for (i in 0 until labelsArr.length()) {
                val labelObj = labelsArr.optJSONObject(i) ?: continue
                val labelName = labelObj.safeString("name").trim()
                if (labelName.isEmpty()) continue
                val labelId = getOrCreateLabel(labelName, labelCache)
                repository.addLabelToNote(newId, labelId)
            }
        }

        return null
    }

    private data class ChecklistDraft(val text: String, val isChecked: Boolean)

    companion object {
        private const val MAX_REASONS_RETURNED = 50
    }
}

private fun JSONObject.safeString(key: String): String =
    if (has(key) && !isNull(key)) optString(key) else ""
