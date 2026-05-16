package com.sealednotes.ui.labels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sealednotes.data.entity.Label
import com.sealednotes.viewmodel.LabelsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLabelsScreen(
    onBack: () -> Unit,
    viewModel: LabelsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.errors.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit labels") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            CreateLabelRow(
                draft = state.newLabelDraft,
                onDraftChange = viewModel::updateNewLabelDraft,
                onSubmit = viewModel::submitNewLabel,
            )
            HorizontalDivider()

            if (state.labels.isEmpty() && state.newLabelDraft.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No labels yet. Type above to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.labels, key = { it.id }) { label ->
                        if (label.id == state.editingLabelId) {
                            EditingLabelRow(
                                draft = state.editingDraft,
                                onDraftChange = viewModel::updateEditingDraft,
                                onConfirm = viewModel::submitEdit,
                                onCancel = viewModel::cancelEditing,
                                onDelete = { viewModel.deleteLabel(label.id) },
                            )
                        } else {
                            LabelRow(
                                label = label,
                                onEdit = { viewModel.startEditing(label) },
                                onDelete = { viewModel.deleteLabel(label.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateLabelRow(
    draft: String,
    onDraftChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (draft.isBlank()) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            IconButton(onClick = { onDraftChange("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear")
            }
        }
        TextField(
            value = draft,
            onValueChange = onDraftChange,
            placeholder = { Text("Create new label") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            colors = transparentTextFieldColors(),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSubmit, enabled = draft.isNotBlank()) {
            Icon(Icons.Outlined.Check, contentDescription = "Add label")
        }
    }
}

@Composable
private fun LabelRow(
    label: Label,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = "Rename")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete label")
        }
    }
}

@Composable
private fun EditingLabelRow(
    draft: String,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete label")
        }
        TextField(
            value = draft,
            onValueChange = onDraftChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            colors = transparentTextFieldColors(),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        IconButton(onClick = onCancel) {
            Icon(Icons.Outlined.Close, contentDescription = "Cancel")
        }
        IconButton(onClick = onConfirm, enabled = draft.isNotBlank()) {
            Icon(Icons.Outlined.Check, contentDescription = "Save")
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
