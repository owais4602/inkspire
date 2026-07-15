package dev.stupifranc.inkspire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.stupifranc.inkspire.ui.editor.EditorScreen
import dev.stupifranc.inkspire.ui.gallery.GalleryScreen

private const val ROUTE_GALLERY = "gallery"
private const val ROUTE_EDITOR = "editor/{drawingId}"
private const val ARG_DRAWING_ID = "drawingId"

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
        // targetSdk 35 enforces edge-to-edge; insets are handled per-screen (the editor pads itself,
        // the gallery draws its dark wall full-bleed and pads only its content).
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = ROUTE_GALLERY) {
                composable(ROUTE_GALLERY) {
                    GalleryScreen(onOpenDrawing = { drawingId -> navController.navigate("editor/$drawingId") })
                }
                composable(
                    ROUTE_EDITOR,
                    arguments = listOf(navArgument(ARG_DRAWING_ID) { type = NavType.StringType }),
                ) { backStackEntry ->
                    val drawingId = backStackEntry.arguments?.getString(ARG_DRAWING_ID) ?: return@composable
                    EditorScreen(drawingId = drawingId)
                }
            }
        }
    }
}
