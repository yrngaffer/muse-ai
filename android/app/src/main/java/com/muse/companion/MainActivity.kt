package com.muse.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.muse.companion.ui.MuseApp
import com.muse.companion.ui.theme.MuseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MuseApplication).container
        setContent {
            MuseTheme {
                MuseApp(container)
            }
        }
    }
}
