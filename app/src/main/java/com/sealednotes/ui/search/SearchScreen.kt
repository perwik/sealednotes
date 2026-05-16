package com.sealednotes.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sealednotes.ui.components.NoteCard
import com.sealednotes.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNoteClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = viewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = viewModel::updateQuery,
                        placeholder = { Text("Search your notes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            query.isBlank() -> EmptyState(
                icon = Icons.Outlined.Search,
                text = "Search notes, checklists, and titles",
                modifier = Modifier.padding(padding),
            )
            results.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Search,
                text = "No notes matching \"$query\"",
                modifier = Modifier.padding(padding),
            )
            else -> LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                items(results, key = { it.note.id }) { nwd ->
                    NoteCard(
                        noteWithDetails = nwd,
                        onClick = { onNoteClick(nwd.note.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
