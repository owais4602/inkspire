package dev.stupifranc.inkspire.ui.gallery

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stupifranc.inkspire.model.DrawingMeta
import dev.stupifranc.inkspire.ui.components.icons.MoreIcon
import dev.stupifranc.inkspire.ui.components.icons.PlusIcon
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

// The gallery is a dark museum wall: near-black so each colorful piece glows against it,
// with one prismatic gradient accent as the app's signature. The editor stays light —
// this palette is scoped to the gallery alone via a local MaterialTheme.
private val WallColor = Color(0xFF101014)
private val WallSurface = Color(0xFF1C1C22)
private val InkWhite = Color(0xFFF2F0EB)
private val InkDim = Color(0xFF908E96)
private val Hairline = Color(0x1FFFFFFF)
private val DeleteRed = Color(0xFFFF7A70)

/** One loop of the kaleidoscope: violet → teal → amber → rose, closing back on violet. */
private val PrismColors = listOf(
    Color(0xFF9C6BFF),
    Color(0xFF2BD9C8),
    Color(0xFFFFC44D),
    Color(0xFFFF6E9A),
    Color(0xFF9C6BFF),
)

private val GalleryColorScheme = darkColorScheme(
    background = WallColor,
    surface = WallSurface,
    surfaceContainer = WallSurface,
    onSurface = InkWhite,
    onSurfaceVariant = InkDim,
    primary = Color(0xFFB9A3FF),
    outline = Hairline,
)

private val PieceCornerShape = RoundedCornerShape(14.dp)

/** Ratio guard so an extreme canvas (e.g. a long banner) can't produce an absurd sliver of a card. */
private const val MIN_CARD_RATIO = 0.58f
private const val MAX_CARD_RATIO = 1.8f
private const val FALLBACK_CARD_RATIO = 0.8f

@Composable
fun GalleryScreen(
    onOpenDrawing: (String) -> Unit,
    viewModel: GalleryViewModel = viewModel(
        factory = GalleryViewModel.factory(LocalContext.current.applicationContext as Application),
    ),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LightSystemBarIcons()

    var renameTarget by remember { mutableStateOf<DrawingMeta?>(null) }

    MaterialTheme(colorScheme = GalleryColorScheme) {
        Box(modifier = Modifier.fillMaxSize().background(WallColor)) {
            val insets = WindowInsets.safeDrawing.asPaddingValues()

            if (viewModel.drawings.isEmpty()) {
                EmptyGallery(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 156.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = insets.calculateTopPadding() + 8.dp,
                        bottom = insets.calculateBottomPadding() + 112.dp,
                    ),
                    verticalItemSpacing = 24.dp,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        GalleryHeader(pieceCount = viewModel.drawings.size)
                    }
                    items(viewModel.drawings, key = { it.id }) { meta ->
                        GalleryPiece(
                            meta = meta,
                            thumbnailFile = viewModel.thumbnailFile(meta.id),
                            onClick = { onOpenDrawing(meta.id) },
                            onRename = { renameTarget = meta },
                            onDuplicate = { viewModel.duplicate(meta.id) },
                            onDelete = { viewModel.delete(meta.id) },
                        )
                    }
                }
            }

            // Empty gallery still shows the wordmark so the first launch isn't a bare wall.
            if (viewModel.drawings.isEmpty()) {
                Box(modifier = Modifier.padding(top = insets.calculateTopPadding() + 8.dp, start = 24.dp)) {
                    GalleryHeader(pieceCount = 0)
                }
            }

            NewPieceButton(
                onClick = { onOpenDrawing(viewModel.createDrawing().id) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = insets.calculateBottomPadding() + 24.dp),
            )
        }

        renameTarget?.let { meta ->
            var name by remember(meta.id) { mutableStateOf(meta.name) }
            AlertDialog(
                onDismissRequest = { renameTarget = null },
                title = { Text("Rename piece") },
                text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.rename(meta.id, name)
                        renameTarget = null
                    }) { Text("Rename") }
                },
                dismissButton = {
                    TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
                },
            )
        }
    }
}

/** The wall is dark regardless of system theme, so status/nav icons must go light while this screen shows. */
@Composable
private fun LightSystemBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousStatus = controller.isAppearanceLightStatusBars
            val previousNav = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
            onDispose {
                controller.isAppearanceLightStatusBars = previousStatus
                controller.isAppearanceLightNavigationBars = previousNav
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun GalleryHeader(pieceCount: Int) {
    Column(modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 12.dp)) {
        Text(
            "Inkspire",
            fontFamily = FontFamily.Serif,
            fontSize = 38.sp,
            color = InkWhite,
            letterSpacing = 0.5.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Brush.horizontalGradient(PrismColors)),
        )
        if (pieceCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                if (pieceCount == 1) "1 piece" else "$pieceCount pieces",
                fontSize = 13.sp,
                color = InkDim,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryPiece(
    meta: DrawingMeta,
    thumbnailFile: File?,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val ratio = if (meta.width > 0f && meta.height > 0f) {
        (meta.width / meta.height).coerceIn(MIN_CARD_RATIO, MAX_CARD_RATIO)
    } else {
        FALLBACK_CARD_RATIO
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // The artwork itself is the card — framed by a hairline, captioned beneath like a museum label.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(PieceCornerShape)
                .background(Color(meta.backgroundColorArgb))
                .border(1.dp, Hairline, PieceCornerShape)
                .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true }),
        ) {
            val bitmap = remember(thumbnailFile?.lastModified()) {
                thumbnailFile?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
            }
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = meta.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meta.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = InkWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    relativeEditTime(meta.updatedAtEpochMillis),
                    fontSize = 11.sp,
                    color = InkDim,
                )
            }
            Box {
                Box(modifier = Modifier.clickable { menuExpanded = true }.padding(6.dp)) {
                    MoreIcon(tint = InkDim)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menuExpanded = false; onRename() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuExpanded = false; onDuplicate() })
                    DropdownMenuItem(
                        text = { Text("Delete", color = DeleteRed) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
    }
}

private fun relativeEditTime(updatedAtEpochMillis: Long): String {
    val now = System.currentTimeMillis()
    return if (now - updatedAtEpochMillis < DateUtils.MINUTE_IN_MILLIS) {
        "Just now"
    } else {
        DateUtils.getRelativeTimeSpanString(updatedAtEpochMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
}

@Composable
private fun NewPieceButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(WallSurface)
            .border(1.dp, Brush.linearGradient(PrismColors), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 22.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIcon(tint = InkWhite)
        Spacer(modifier = Modifier.width(6.dp))
        Text("New", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InkWhite, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun EmptyGallery(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        KaleidoscopeGlyph()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Your gallery awaits",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            color = InkWhite,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap New to craft your first piece",
            fontSize = 14.sp,
            color = InkDim,
        )
    }
}

/** Six prism-colored spokes — a quiet nod to the kaleidoscope mode that gives the app its soul. */
@Composable
private fun KaleidoscopeGlyph() {
    Canvas(modifier = Modifier.size(56.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.44f
        val strokeWidth = 2.dp.toPx()
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60 - 90).toDouble())
            val end = Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat(),
            )
            drawLine(
                color = PrismColors[i % (PrismColors.size - 1)].copy(alpha = 0.85f),
                start = center,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(InkWhite, radius = strokeWidth, center = center)
    }
}
