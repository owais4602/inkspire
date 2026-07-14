package dev.stupifranc.inkspire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.stupifranc.inkspire.ui.editor.EditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InkspireApp()
        }
    }
}

@Composable
fun InkspireApp() {
    MaterialTheme {
        // targetSdk 35 enforces edge-to-edge; without this the toolbar draws under the status bar / nav bar.
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            EditorScreen()
        }
    }
}
