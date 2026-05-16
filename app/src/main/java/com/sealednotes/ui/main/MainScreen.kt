package com.sealednotes.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sealednotes.data.entity.NoteWithDetails
import com.sealednotes.ui.components.NoteCard
import com.sealednotes.viewmodel.MainViewModel
import com.sealednotes.viewmodel.NoteFilter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: (checklist: Boolean, preselectedLabelId: Long?) -> Unit,
    onSearchClick: () -> Unit,
    onTrashClick: () -> Unit,
    onLabelsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showFabMenu by remember { mutableStateOf(false) }
    var showBulkLabelSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    val handleNoteClick: (Long) -> Unit = { id ->
        if (uiState.isSelectionMode) viewModel.toggleSelection(id) else onNoteClick(id)
    }
    val handleNoteLongClick: (Long) -> Unit = { id ->
        viewModel.toggleSelection(id)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !uiState.isSelectionMode,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    "SealedNotes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp)
                )
                HorizontalDivider()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Lightbulb, contentDescription = null) },
                    label = { Text("Notes") },
                    selected = uiState.currentFilter == NoteFilter.ALL && uiState.currentLabelId == null,
                    onClick = {
                        viewModel.setFilter(NoteFilter.ALL)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
                    label = { Text("Reminders") },
                    selected = uiState.currentFilter == NoteFilter.REMINDERS,
                    onClick = {
                        viewModel.setFilter(NoteFilter.REMINDERS)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Labels section
                if (uiState.labels.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Labels",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                    uiState.labels.forEach { label ->
                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null) },
                            label = {
                                Text(
                                    label.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = uiState.currentLabelId == label.id,
                            onClick = {
                                viewModel.setLabelFilter(label.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                    label = { Text("Edit labels") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLabelsClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                    label = { Text("Archive") },
                    selected = uiState.currentFilter == NoteFilter.ARCHIVE,
                    onClick = {
                        viewModel.setFilter(NoteFilter.ARCHIVE)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    label = { Text("Trash") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onTrashClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettingsClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (uiState.isSelectionMode) {
                    SelectionTopBar(
                        count = uiState.selectedNoteIds.size,
                        inArchive = uiState.currentFilter == NoteFilter.ARCHIVE,
                        onClose = viewModel::clearSelection,
                        onTogglePin = viewModel::togglePinSelected,
                        onLabels = { showBulkLabelSheet = true },
                        onArchive = { viewModel.setArchivedForSelected(true) },
                        onUnarchive = { viewModel.setArchivedForSelected(false) },
                        onDelete = viewModel::trashSelected,
                    )
                } else {
                    TopAppBar(
                        title = {
                            val title = when {
                                uiState.currentLabelId != null ->
                                    uiState.labels.find { it.id == uiState.currentLabelId }?.name ?: "Notes"
                                uiState.currentFilter == NoteFilter.REMINDERS -> "Reminders"
                                uiState.currentFilter == NoteFilter.ARCHIVE -> "Archive"
                                else -> "SealedNotes"
                            }
                            Text(title)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = onSearchClick) {
                                Icon(Icons.Outlined.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (uiState.isGridView) Icons.Outlined.ViewAgenda
                                    else Icons.Outlined.GridView,
                                    contentDescription = "Toggle view"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            floatingActionButton = {
                if (!uiState.isSelectionMode) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (showFabMenu) {
                            SmallFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    onNewNote(true, uiState.currentLabelId)
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(Icons.Outlined.CheckBox, contentDescription = "New checklist")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        FloatingActionButton(
                            onClick = {
                                if (showFabMenu) {
                                    showFabMenu = false
                                    onNewNote(false, uiState.currentLabelId)
                                } else {
                                    showFabMenu = true
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New note")
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            val allEmpty = uiState.pinnedNotes.isEmpty() && uiState.unpinnedNotes.isEmpty()

            if (allEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No notes yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (uiState.isGridView) {
                NoteGrid(
                    pinnedNotes = uiState.pinnedNotes,
                    unpinnedNotes = uiState.unpinnedNotes,
                    selectedIds = uiState.selectedNoteIds,
                    isInSelectionMode = uiState.isSelectionMode,
                    onNoteClick = handleNoteClick,
                    onNoteLongClick = handleNoteLongClick,
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                NoteList(
                    pinnedNotes = uiState.pinnedNotes,
                    unpinnedNotes = uiState.unpinnedNotes,
                    selectedIds = uiState.selectedNoteIds,
                    isInSelectionMode = uiState.isSelectionMode,
                    onNoteClick = handleNoteClick,
                    onNoteLongClick = handleNoteLongClick,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    if (showBulkLabelSheet && uiState.isSelectionMode) {
        val commonLabelIds: Set<Long> = run {
            val selected = (uiState.pinnedNotes + uiState.unpinnedNotes)
                .filter { it.note.id in uiState.selectedNoteIds }
            if (selected.isEmpty()) emptySet()
            else uiState.labels
                .filter { label -> selected.all { nwd -> nwd.labels.any { it.id == label.id } } }
                .map { it.id }
                .toSet()
        }
        BulkLabelSheet(
            labels = uiState.labels,
            commonLabelIds = commonLabelIds,
            onToggle = viewModel::toggleLabelForSelected,
            onDismiss = { showBulkLabelSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    inArchive: Boolean,
    onClose: () -> Unit,
    onTogglePin: () -> Unit,
    onLabels: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = { Text("$count") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Cancel selection")
            }
        },
        actions = {
            IconButton(onClick = onTogglePin) {
                Icon(Icons.Outlined.PushPin, contentDescription = "Pin")
            }
            IconButton(onClick = onLabels) {
                Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = "Labels")
            }
            if (inArchive) {
                IconButton(onClick = onUnarchive) {
                    Icon(Icons.Outlined.Unarchive, contentDescription = "Unarchive")
                }
            } else {
                IconButton(onClick = onArchive) {
                    Icon(Icons.Outlined.Archive, contentDescription = "Archive")
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkLabelSheet(
    labels: List<com.sealednotes.data.entity.Label>,
    commonLabelIds: Set<Long>,
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
                                checked = label.id in commonLabelIds,
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
private fun NoteGrid(
    pinnedNotes: List<NoteWithDetails>,
    unpinnedNotes: List<NoteWithDetails>,
    selectedIds: Set<Long>,
    isInSelectionMode: Boolean,
    onNoteClick: (Long) -> Unit,
    onNoteLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        if (pinnedNotes.isNotEmpty()) {
            item {
                Text(
                    "Pinned",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            // Empty item to fill the second column for the header
            item { Spacer(modifier = Modifier) }

            items(pinnedNotes, key = { it.note.id }) { nwd ->
                NoteCard(
                    noteWithDetails = nwd,
                    onClick = { onNoteClick(nwd.note.id) },
                    onLongClick = { onNoteLongClick(nwd.note.id) },
                    isSelected = nwd.note.id in selectedIds,
                    isInSelectionMode = isInSelectionMode,
                )
            }
        }

        if (pinnedNotes.isNotEmpty() && unpinnedNotes.isNotEmpty()) {
            item {
                Text(
                    "Others",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            item { Spacer(modifier = Modifier) }
        }

        items(unpinnedNotes, key = { it.note.id }) { nwd ->
            NoteCard(
                noteWithDetails = nwd,
                onClick = { onNoteClick(nwd.note.id) },
                onLongClick = { onNoteLongClick(nwd.note.id) },
                isSelected = nwd.note.id in selectedIds,
                isInSelectionMode = isInSelectionMode,
            )
        }
    }
}

@Composable
private fun NoteList(
    pinnedNotes: List<NoteWithDetails>,
    unpinnedNotes: List<NoteWithDetails>,
    selectedIds: Set<Long>,
    isInSelectionMode: Boolean,
    onNoteClick: (Long) -> Unit,
    onNoteLongClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pinnedNotes.isNotEmpty()) {
            item {
                Text(
                    "Pinned",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            items(pinnedNotes, key = { it.note.id }) { nwd ->
                NoteCard(
                    noteWithDetails = nwd,
                    onClick = { onNoteClick(nwd.note.id) },
                    onLongClick = { onNoteLongClick(nwd.note.id) },
                    isSelected = nwd.note.id in selectedIds,
                    isInSelectionMode = isInSelectionMode,
                )
            }
        }

        if (pinnedNotes.isNotEmpty() && unpinnedNotes.isNotEmpty()) {
            item {
                Text(
                    "Others",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }

        items(unpinnedNotes, key = { it.note.id }) { nwd ->
            NoteCard(
                noteWithDetails = nwd,
                onClick = { onNoteClick(nwd.note.id) },
                onLongClick = { onNoteLongClick(nwd.note.id) },
                isSelected = nwd.note.id in selectedIds,
                isInSelectionMode = isInSelectionMode,
            )
        }
    }
}
