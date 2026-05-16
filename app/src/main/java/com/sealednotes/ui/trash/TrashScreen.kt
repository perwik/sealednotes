package com.sealednotes.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sealednotes.SealedNotesApp
import com.sealednotes.ui.components.NoteCard
import com.sealednotes.viewmodel.TrashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = state.isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedNoteIds.size}") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Outlined.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::restoreSelected) {
                            Icon(Icons.Outlined.RestoreFromTrash, contentDescription = "Restore")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete permanently")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            } else {
                TopAppBar(
                    title = { Text("Trash") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (state.notes.isNotEmpty()) {
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Empty trash") },
                                        onClick = {
                                            showOverflowMenu = false
                                            showEmptyTrashConfirm = true
                                        },
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "Notes in Trash are deleted automatically after ${SealedNotesApp.TRASH_RETENTION_DAYS} days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (state.notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No notes in trash",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                ) {
                    items(state.notes, key = { it.note.id }) { nwd ->
                        NoteCard(
                            noteWithDetails = nwd,
                            onClick = { viewModel.toggleSelection(nwd.note.id) },
                            onLongClick = { viewModel.toggleSelection(nwd.note.id) },
                            isSelected = nwd.note.id in state.selectedNoteIds,
                            isInSelectionMode = state.isSelectionMode,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val n = state.selectedNoteIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete permanently?") },
            text = {
                Text(
                    if (n == 1) "This note will be deleted permanently. This cannot be undone."
                    else "$n notes will be deleted permanently. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deletePermanentlySelected()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty trash?") },
            text = {
                Text("All ${state.notes.size} notes in trash will be deleted permanently. This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showEmptyTrashConfirm = false
                    viewModel.emptyTrash()
                }) { Text("Empty trash") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
