package com.sealednotes.ui.editor

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sealednotes.data.entity.ReminderRecurrence
import com.sealednotes.viewmodel.EditableChecklistItem
import com.sealednotes.viewmodel.NoteEditorViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long?,
    isNewNote: Boolean,
    isChecklist: Boolean,
    onBack: () -> Unit,
    preselectedLabelId: Long? = null,
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: NoteEditorViewModel = viewModel(
        factory = NoteEditorViewModel.Factory(
            application, noteId, isNewNote, isChecklist, preselectedLabelId
        ),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allLabels by viewModel.allLabels.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showLabelPicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showExactAlarmHelp by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // After the OS permission dialog returns, check the exact-alarm policy. This avoids
    // showing our dialog on top of the system permission prompt.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) showExactAlarmHelp = true
        }
    }

    val openReminderPicker: () -> Unit = {
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (needsNotificationPermission) {
            // The launcher's callback handles the exact-alarm check afterwards.
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) showExactAlarmHelp = true
        }
        showReminderPicker = true
    }

    val saveAndExit: () -> Unit = {
        scope.launch {
            viewModel.save()
            onBack()
        }
    }

    val trashAndExit: () -> Unit = {
        scope.launch {
            viewModel.save()       // persist any new content first so trash has something to point at
            viewModel.moveToTrash()
            onBack()
        }
    }

    BackHandler { saveAndExit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = saveAndExit) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::togglePinned) {
                        Icon(
                            imageVector = if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (state.isPinned) "Unpin" else "Pin",
                        )
                    }
                    IconButton(onClick = { showLabelPicker = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Label,
                            contentDescription = "Labels",
                        )
                    }
                    IconButton(onClick = openReminderPicker) {
                        Icon(
                            imageVector = if (state.reminderTime != null) Icons.Filled.Notifications
                            else Icons.Outlined.Notifications,
                            contentDescription = "Reminder",
                            tint = if (state.reminderTime != null) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current,
                        )
                    }
                    IconButton(onClick = {
                        viewModel.toggleArchived()
                        saveAndExit()
                    }) {
                        Icon(
                            imageVector = if (state.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                            contentDescription = if (state.isArchived) "Unarchive" else "Archive",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = trashAndExit) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val layoutDirection = LocalLayoutDirection.current
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = maxOf(padding.calculateBottomPadding(), imeBottom),
                )
                .padding(horizontal = 16.dp),
        ) {
            TextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                placeholder = { Text("Title") },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = transparentTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            state.reminderTime?.let { time ->
                ReminderChip(
                    timeMillis = time,
                    recurrence = state.reminderRecurrence,
                    isOverdue = time < System.currentTimeMillis(),
                    onClick = openReminderPicker,
                )
            }

            if (state.isChecklist) {
                ChecklistEditor(
                    items = state.checklistItems,
                    onContentChange = viewModel::updateChecklistItem,
                    onCheckedChange = viewModel::setChecklistItemChecked,
                    onDelete = viewModel::deleteChecklistItem,
                    onAdd = viewModel::addChecklistItem,
                    modifier = Modifier.weight(1f),
                )
            } else {
                TextField(
                    value = state.content,
                    onValueChange = viewModel::updateContent,
                    placeholder = { Text("Note") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = transparentTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }

    if (showLabelPicker) {
        LabelPickerSheet(
            labels = allLabels,
            selectedIds = state.selectedLabelIds,
            onToggle = viewModel::toggleLabel,
            onDismiss = { showLabelPicker = false },
        )
    }

    if (showReminderPicker) {
        ReminderPickerDialog(
            initialTime = state.reminderTime,
            initialRecurrence = state.reminderRecurrence,
            onSave = { time, recurrence ->
                viewModel.setReminder(time, recurrence)
                showReminderPicker = false
            },
            onRemove = {
                viewModel.clearReminder()
                showReminderPicker = false
            },
            onDismiss = { showReminderPicker = false },
        )
    }

    if (showExactAlarmHelp) {
        AlertDialog(
            onDismissRequest = { showExactAlarmHelp = false },
            title = { Text("Allow exact alarms") },
            text = {
                Text(
                    "For reminders to fire on time, SealedNotes needs permission to schedule exact alarms. " +
                        "Without it, the system may delay reminders by minutes or skip them while the device is idle."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmHelp = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            )
                        }
                    }
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmHelp = false }) { Text("Later") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelPickerSheet(
    labels: List<com.sealednotes.data.entity.Label>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "Labels",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (labels.isEmpty()) {
                Text(
                    "No labels yet. Create one from the drawer → Edit labels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(labels, key = { it.id }) { label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(label.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = label.id in selectedIds,
                                onCheckedChange = { onToggle(label.id) },
                            )
                            Text(
                                text = label.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChecklistEditor(
    items: List<EditableChecklistItem>,
    onContentChange: (index: Int, content: String) -> Unit,
    onCheckedChange: (index: Int, checked: Boolean) -> Unit,
    onDelete: (index: Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = items,
            key = { index, item -> if (item.id != 0L) item.id else -(index.toLong() + 1) },
        ) { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { onCheckedChange(index, it) },
                )
                TextField(
                    value = item.content,
                    onValueChange = { onContentChange(index, it) },
                    placeholder = { Text("List item") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = transparentTextFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onDelete(index) }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove item")
                }
            }
        }

        item {
            TextButton(
                onClick = onAdd,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("List item")
            }
        }
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.background,
    unfocusedContainerColor = MaterialTheme.colorScheme.background,
    focusedIndicatorColor = MaterialTheme.colorScheme.background,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderChip(
    timeMillis: Long,
    recurrence: ReminderRecurrence,
    isOverdue: Boolean,
    onClick: () -> Unit,
) {
    val fmt = remember { SimpleDateFormat("EEE MMM d, HH:mm", Locale.getDefault()) }
    val label = buildString {
        append(fmt.format(Date(timeMillis)))
        if (recurrence != ReminderRecurrence.NONE) {
            append(" · ")
            append(recurrence.displayLabel)
        }
    }
    AssistChip(
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        label = { Text(label) },
        colors = if (isOverdue) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                labelColor = MaterialTheme.colorScheme.onErrorContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else {
            AssistChipDefaults.assistChipColors()
        },
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

private val ReminderRecurrence.displayLabel: String
    get() = when (this) {
        ReminderRecurrence.NONE -> "Does not repeat"
        ReminderRecurrence.DAILY -> "Daily"
        ReminderRecurrence.WEEKLY -> "Weekly"
        ReminderRecurrence.MONTHLY -> "Monthly"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderPickerDialog(
    initialTime: Long?,
    initialRecurrence: ReminderRecurrence,
    onSave: (timeMillis: Long, recurrence: ReminderRecurrence) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val seedCal = remember(initialTime) {
        Calendar.getInstance().apply {
            timeInMillis = initialTime ?: (System.currentTimeMillis() + 60 * 60 * 1000L)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    var year by remember { mutableStateOf(seedCal.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(seedCal.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(seedCal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableStateOf(seedCal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(seedCal.get(Calendar.MINUTE)) }
    var recurrence by remember { mutableStateOf(initialRecurrence) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun computeMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val displayedMillis = computeMillis()
    val dateFmt = remember { SimpleDateFormat("EEE MMM d, yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Reminder", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Date", style = MaterialTheme.typography.labelMedium)
                        Text(
                            dateFmt.format(Date(displayedMillis)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    TextButton(onClick = { showDatePicker = true }) { Text("Change") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Time", style = MaterialTheme.typography.labelMedium)
                        Text(
                            timeFmt.format(Date(displayedMillis)),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    TextButton(onClick = { showTimePicker = true }) { Text("Change") }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Repeats", style = MaterialTheme.typography.labelMedium)
                ReminderRecurrence.entries.forEach { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { recurrence = rec }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = recurrence == rec,
                            onClick = { recurrence = rec },
                        )
                        Text(rec.displayLabel, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (initialTime != null) {
                        TextButton(onClick = onRemove) { Text("Remove") }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = { onSave(displayedMillis, recurrence) }) { Text("Save") }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = displayedMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { picked ->
                        val cal = Calendar.getInstance().apply { timeInMillis = picked }
                        year = cal.get(Calendar.YEAR)
                        month = cal.get(Calendar.MONTH)
                        day = cal.get(Calendar.DAY_OF_MONTH)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    hour = timePickerState.hour
                    minute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
        )
    }
}
