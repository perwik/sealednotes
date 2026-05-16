package com.sealednotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sealednotes.ui.SealedNotesNavHost
import com.sealednotes.ui.theme.SealedNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SealedNotesTheme {
                SealedNotesNavHost()
            }
        }
    }
}
