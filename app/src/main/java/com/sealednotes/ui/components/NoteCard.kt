package com.sealednotes.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sealednotes.data.entity.NoteWithDetails
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    noteWithDetails: NoteWithDetails,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    isInSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val note = noteWithDetails.note
    val checklistItems = noteWithDetails.checklistItems.sortedBy { it.sortOrder }
    val labels = noteWithDetails.labels

    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh

    val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            if (isInSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        modifier = Modifier.size(18.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Title
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Content or checklist preview
            if (note.isChecklist) {
                // Show first few checklist items
                val previewItems = checklistItems.take(5)
                previewItems.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isChecked) Icons.Outlined.CheckBox
                            else Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (checklistItems.size > 5) {
                    Text(
                        text = "+${checklistItems.size - 5} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            } else if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Labels and indicators
            val hasLabels = labels.isNotEmpty()
            val hasReminder = note.reminderTime != null
            val isPinned = note.isPinned

            if (hasLabels || hasReminder || isPinned) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (hasReminder) {
                        val isOverdue = (note.reminderTime ?: 0) < System.currentTimeMillis()
                        ReminderChip(
                            time = note.reminderTime!!,
                            isOverdue = isOverdue
                        )
                    }

                    labels.take(3).forEach { label ->
                        LabelChip(label.name)
                    }
                    if (labels.size > 3) {
                        Text(
                            text = "+${labels.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderChip(time: Long, isOverdue: Boolean) {
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isOverdue) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = formatter.format(Date(time)),
                style = MaterialTheme.typography.labelSmall,
                color = if (isOverdue) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabelChip(name: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
