package com.sealednotes.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sealednotes.ui.editor.NoteEditorScreen
import com.sealednotes.ui.labels.ManageLabelsScreen
import com.sealednotes.ui.main.MainScreen
import com.sealednotes.ui.search.SearchScreen
import com.sealednotes.ui.settings.SettingsScreen
import com.sealednotes.ui.trash.TrashScreen

object Routes {
    const val MAIN = "main"
    const val EDITOR = "editor/{noteId}"
    const val NEW_NOTE = "editor/new?checklist={checklist}&labelId={labelId}"
    const val SEARCH = "search"
    const val TRASH = "trash"
    const val LABELS = "labels"
    const val SETTINGS = "settings"

    private const val NO_LABEL = -1L

    fun editor(noteId: Long) = "editor/$noteId"
    fun newNote(checklist: Boolean = false, labelId: Long? = null): String {
        val l = labelId ?: NO_LABEL
        return "editor/new?checklist=$checklist&labelId=$l"
    }
}

@Composable
fun SealedNotesNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.MAIN) {

        composable(Routes.MAIN) {
            MainScreen(
                onNoteClick = { noteId -> navController.navigate(Routes.editor(noteId)) },
                onNewNote = { checklist, labelId ->
                    navController.navigate(Routes.newNote(checklist, labelId))
                },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onTrashClick = { navController.navigate(Routes.TRASH) },
                onLabelsClick = { navController.navigate(Routes.LABELS) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = "editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
            NoteEditorScreen(
                noteId = noteId,
                isNewNote = false,
                isChecklist = false,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.NEW_NOTE,
            arguments = listOf(
                navArgument("checklist") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("labelId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            )
        ) { backStackEntry ->
            val isChecklist = backStackEntry.arguments?.getBoolean("checklist") ?: false
            val rawLabelId = backStackEntry.arguments?.getLong("labelId") ?: -1L
            val preselectedLabelId = if (rawLabelId == -1L) null else rawLabelId
            NoteEditorScreen(
                noteId = null,
                isNewNote = true,
                isChecklist = isChecklist,
                preselectedLabelId = preselectedLabelId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onNoteClick = { noteId -> navController.navigate(Routes.editor(noteId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TRASH) {
            TrashScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.LABELS) {
            ManageLabelsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
