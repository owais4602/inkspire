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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import dev.stupifranc.inkspire.core.PinchStep
import dev.stupifranc.inkspire.core.PinchSteps
import dev.stupifranc.inkspire.model.CanvasShape
import dev.stupifranc.inkspire.model.CornerStyle
import dev.stupifranc.inkspire.model.DrawingMeta
import dev.stupifranc.inkspire.model.AppPrefs
import dev.stupifranc.inkspire.model.ThumbSize
import dev.stupifranc.inkspire.model.AppTheme
import dev.stupifranc.inkspire.ui.components.canvasOutlineShape
import dev.stupifranc.inkspire.ui.components.icons.MoreIcon
import dev.stupifranc.inkspire.ui.components.icons.PlusIcon
import dev.stupifranc.inkspire.ui.components.icons.TuneIcon
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

// Minimal, premium, zero-chroma gallery: strict monochrome tonal sets, no gradients. The editor stays
// light; this palette is scoped to the gallery alone via a local MaterialTheme. Wall tone (dark/light)
// is user-selectable via the customization sheet — dark is the default.
private data class GalleryTokens(
    val wall: Color,
    val surface: Color,
    val textPrimary: Color,
    val textDim: Color,
    val hairline: Color,
    val isDark: Boolean,
)

private val DarkGalleryTokens = GalleryTokens(
    wall = Color(0xFF0D0D0F),
    surface = Color(0xFF17171A),
    textPrimary = Color(0xFFF5F4F1),
    textDim = Color(0xFF8A8A90),
    hairline = Color(0x24FFFFFF),
    isDark = true,
)

private val LightGalleryTokens = GalleryTokens(
    wall = Color(0xFFFAFAF8),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF161616),
    textDim = Color(0xFF7A7A7E),
    hairline = Color(0x1F000000),
    isDark = false,
)

@Composable
private fun tokensFor(theme: AppTheme): GalleryTokens {
    val isSystemDark = isSystemInDarkTheme()
    return if (theme == AppTheme.DARK || (theme == AppTheme.SYSTEM && isSystemDark)) DarkGalleryTokens else LightGalleryTokens
}

/** The only non-grayscale pixel allowed on this screen — the destructive-action convention, desaturated. */
private val DeleteRed = Color(0xFFE05C52)

/** Builds a fully explicit color scheme from [tokens] so no Material3 default role (e.g. a purple primary) leaks chroma. */
private fun galleryColorScheme(tokens: GalleryTokens): ColorScheme {
    val base = if (tokens.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        background = tokens.wall,
        onBackground = tokens.textPrimary,
        surface = tokens.surface,
        onSurface = tokens.textPrimary,
        surfaceVariant = tokens.surface,
        onSurfaceVariant = tokens.textDim,
        surfaceContainer = tokens.surface,
        primary = tokens.textPrimary,
        onPrimary = tokens.wall,
        secondary = tokens.textDim,
        onSecondary = tokens.wall,
        secondaryContainer = tokens.surface,
        onSecondaryContainer = tokens.textPrimary,
        outline = tokens.hairline,
        outlineVariant = tokens.hairline,
        error = DeleteRed,
        onError = tokens.wall,
    )
}

/** Fallback ratio if dimensions are missing or invalid. */
private const val FALLBACK_CARD_RATIO = 0.8f

@OptIn(ExperimentalMaterial3Api::class)
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

    val prefs = viewModel.prefs
    val tokens = tokensFor(prefs.theme)
    SystemBarIcons(lightWall = !tokens.isDark)

    var renameTarget by remember { mutableStateOf<DrawingMeta?>(null) }
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var focusedDrawingId by remember { mutableStateOf<String?>(null) }

    MaterialTheme(colorScheme = galleryColorScheme(tokens)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.wall)
                .pointerInput(Unit) {
                    detectPinchToStepThumbnails { step ->
                        viewModel.updatePrefs(viewModel.prefs.copy(thumbSize = viewModel.prefs.thumbSize.stepped(step)))
                    }
                },
        ) {
            val insets = WindowInsets.safeDrawing.asPaddingValues()

            if (viewModel.drawings.isEmpty()) {
                EmptyGallery(tokens = tokens, modifier = Modifier.align(Alignment.Center))
                Box(modifier = Modifier.padding(top = insets.calculateTopPadding() + 8.dp, start = 24.dp, end = 8.dp)) {
                    GalleryHeader(pieceCount = 0, tokens = tokens, onOpenCustomize = { showCustomizeSheet = true })
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = prefs.thumbSize.minCellDp.dp),
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
                        GalleryHeader(
                            pieceCount = viewModel.drawings.size,
                            tokens = tokens,
                            onOpenCustomize = { showCustomizeSheet = true },
                        )
                    }
                    items(viewModel.drawings, key = { it.id }) { meta ->
                        var cumulativeDragX by remember { mutableStateOf(0f) }
                        var cumulativeDragY by remember { mutableStateOf(0f) }
                        
                        Box(modifier = Modifier
                            .animateItem()
                            .pointerInput(meta.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { 
                                        focusedDrawingId = meta.id
                                        cumulativeDragX = 0f
                                        cumulativeDragY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (focusedDrawingId == meta.id) {
                                            focusedDrawingId = null
                                        }
                                        cumulativeDragX += dragAmount.x
                                        cumulativeDragY += dragAmount.y
                                        
                                        val currentIndex = viewModel.drawings.indexOfFirst { it.id == meta.id }
                                        if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                        
                                        if (cumulativeDragX > 150f && currentIndex < viewModel.drawings.lastIndex) {
                                            viewModel.reorderDrawings(currentIndex, currentIndex + 1)
                                            cumulativeDragX = 0f
                                        } else if (cumulativeDragX < -150f && currentIndex > 0) {
                                            viewModel.reorderDrawings(currentIndex, currentIndex - 1)
                                            cumulativeDragX = 0f
                                        }
                                        
                                        if (cumulativeDragY > 200f && currentIndex < viewModel.drawings.lastIndex - 1) {
                                            viewModel.reorderDrawings(currentIndex, minOf(currentIndex + 2, viewModel.drawings.lastIndex))
                                            cumulativeDragY = 0f
                                        } else if (cumulativeDragY < -200f && currentIndex > 1) {
                                            viewModel.reorderDrawings(currentIndex, maxOf(currentIndex - 2, 0))
                                            cumulativeDragY = 0f
                                        }
                                    },
                                    onDragEnd = {
                                        cumulativeDragX = 0f
                                        cumulativeDragY = 0f
                                    },
                                    onDragCancel = {
                                        cumulativeDragX = 0f
                                        cumulativeDragY = 0f
                                    }
                                )
                            }
                        ) {
                            GalleryPiece(
                                meta = meta,
                                thumbnailPath = viewModel.thumbnailFile(meta.id)?.path,
                                prefs = prefs,
                                tokens = tokens,
                                onClick = { onOpenDrawing(meta.id) }
                            )
                        }
                    }
                }
            }

            NewPieceButton(
                tokens = tokens,
                onClick = { onOpenDrawing(viewModel.createDrawing().id) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = insets.calculateBottomPadding() + 24.dp),
            )
        }

        focusedDrawingId?.let { focusId ->
            val meta = viewModel.drawings.find { it.id == focusId }
            if (meta != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { focusedDrawingId = null },
                    contentAlignment = Alignment.Center
                ) {
                     Box(modifier = Modifier
                         .fillMaxWidth(0.8f)
                         .aspectRatio(if(meta.width > 0f && meta.height > 0f) meta.width/meta.height else FALLBACK_CARD_RATIO)
                     ) {
                         GalleryPiece(
                            meta = meta,
                            thumbnailPath = viewModel.thumbnailFile(meta.id)?.path,
                            prefs = prefs,
                            tokens = tokens,
                            onClick = { onOpenDrawing(meta.id) }
                        )
                     }
                     
                     Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)) {
                         TextButton(onClick = { onOpenDrawing(meta.id); focusedDrawingId = null }) { Text("Open", color = Color.White) }
                         TextButton(
                             onClick = {
                                 viewModel.togglePin(meta.id)
                                 focusedDrawingId = null
                             }
                         ) {
                             Text(if (meta.isPinned) "Unpin" else "Pin", color = Color.White)
                         }
                         TextButton(onClick = { renameTarget = meta; focusedDrawingId = null }) { Text("Rename", color = Color.White) }
                         TextButton(onClick = { viewModel.duplicate(meta.id); focusedDrawingId = null }) { Text("Duplicate", color = Color.White) }
                         TextButton(onClick = { viewModel.delete(meta.id); focusedDrawingId = null }) { Text("Delete", color = DeleteRed) }
                     }
                }
            }
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

        if (showCustomizeSheet) {
            GalleryCustomizationSheet(
                prefs = prefs,
                tokens = tokens,
                onPrefsChange = viewModel::updatePrefs,
                onDismiss = { showCustomizeSheet = false },
            )
        }
    }
}

private fun ThumbSize.stepped(step: PinchStep): ThumbSize {
    val values = ThumbSize.entries
    val delta = if (step == PinchStep.StepUp) 1 else -1
    val index = (values.indexOf(this) + delta).coerceIn(0, values.lastIndex)
    return values[index]
}

/**
 * Two-finger pinch drives [PinchSteps]; deliberately only inspects/consumes events once 2+ pointers are
 * down (checked on [PointerEventPass.Initial], ahead of the grid's own scroll handling in the Main pass)
 * so ordinary one-finger vertical scroll is never touched — the known trap of layering a transform
 * gesture over a `LazyVerticalStaggeredGrid`.
 */
private suspend fun PointerInputScope.detectPinchToStepThumbnails(onStep: (PinchStep) -> Unit) {
    val pinchSteps = PinchSteps()
    awaitEachGesture {
        var pinching = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedCount = event.changes.count { it.pressed }
            when {
                pressedCount >= 2 -> {
                    if (!pinching) {
                        pinching = true
                        pinchSteps.reset()
                    }
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) {
                        pinchSteps.onScaleFactor(zoom)?.let(onStep)
                    }
                    event.changes.forEach { it.consume() }
                }
                pinching -> {
                    pinching = false
                    pinchSteps.reset()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

/** The wall is a fixed tone regardless of system theme, so status/nav icons must be set explicitly while this screen shows. */
@Composable
private fun SystemBarIcons(lightWall: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, lightWall) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousStatus = controller.isAppearanceLightStatusBars
            val previousNav = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = lightWall
            controller.isAppearanceLightNavigationBars = lightWall
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
private fun GalleryHeader(pieceCount: Int, tokens: GalleryTokens, onOpenCustomize: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.weight(1f)) // Spacer to push the tune icon to the right
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenCustomize)
                .padding(10.dp),
        ) {
            TuneIcon(tint = tokens.textDim)
        }
    }
}

@Composable
private fun GalleryPiece(
    meta: DrawingMeta,
    thumbnailPath: String?,
    prefs: AppPrefs,
    tokens: GalleryTokens,
    onClick: () -> Unit,
) {
    val ratio = if (meta.width > 0f && meta.height > 0f) {
        meta.width / meta.height
    } else {
        FALLBACK_CARD_RATIO
    }
    // A drawing's own canvas shape (rounded/circle) is a deliberate per-piece choice and takes
    // priority over the gallery-wide corner-style preference, which only applies to plain rectangles.
    val cornerShape = if (meta.shape == CanvasShape.RECTANGLE) {
        RoundedCornerShape(prefs.cornerStyle.radiusDp.dp)
    } else {
        canvasOutlineShape(meta.shape)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // The artwork itself is the card — framed by an optional hairline, captioned beneath like a museum label.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(cornerShape)
                .background(if (meta.backgroundColorArgb == 0) tokens.surface else Color(meta.backgroundColorArgb))
                .then(if (prefs.borderEnabled) Modifier.border(1.dp, tokens.hairline, cornerShape) else Modifier)
                .clickable(onClick = onClick),
        ) {
            val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = thumbnailPath, key2 = meta.updatedAtEpochMillis) {
                if (thumbnailPath != null) {
                    value = withContext(Dispatchers.IO) {
                        val file = File(thumbnailPath)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.path)
                        } else null
                    }
                } else {
                    value = null
                }
            }
            if (bitmap != null) {
                Image(
                    painter = BitmapPainter(bitmap!!.asImageBitmap()),
                    contentDescription = meta.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
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
private fun NewPieceButton(tokens: GalleryTokens, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tokens.surface)
            .border(1.dp, tokens.hairline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 22.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIcon(tint = tokens.textPrimary)
        Spacer(modifier = Modifier.width(6.dp))
        Text("New", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = tokens.textPrimary, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun EmptyGallery(tokens: GalleryTokens, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        KaleidoscopeGlyph(tokens)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Your gallery awaits",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            color = tokens.textPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap New to craft your first piece",
            fontSize = 14.sp,
            color = tokens.textDim,
        )
    }
}

/** Six spokes — a quiet nod to the kaleidoscope mode that gives the app its soul, kept strictly monochrome. */
@Composable
private fun KaleidoscopeGlyph(tokens: GalleryTokens) {
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
                color = tokens.textDim.copy(alpha = 0.6f),
                start = center,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(tokens.textPrimary, radius = strokeWidth, center = center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryCustomizationSheet(
    prefs: AppPrefs,
    tokens: GalleryTokens,
    onPrefsChange: (AppPrefs) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        contentColor = tokens.textPrimary,
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(
                "Customize gallery",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.textPrimary,
            )
            Spacer(modifier = Modifier.height(24.dp))

            SheetSectionLabel("Thumbnail size", tokens)
            SegmentedRow(
                options = ThumbSize.entries,
                selected = prefs.thumbSize,
                tokens = tokens,
                label = ::thumbSizeLabel,
                onSelect = { onPrefsChange(prefs.copy(thumbSize = it)) },
            )
            Spacer(modifier = Modifier.height(24.dp))

            SheetSwitchRow("Card border", prefs.borderEnabled, tokens) {
                onPrefsChange(prefs.copy(borderEnabled = it))
            }
            Spacer(modifier = Modifier.height(20.dp))

            SheetSectionLabel("Corners", tokens)
            SegmentedRow(
                options = CornerStyle.entries,
                selected = prefs.cornerStyle,
                tokens = tokens,
                label = ::cornerStyleLabel,
                onSelect = { onPrefsChange(prefs.copy(cornerStyle = it)) },
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Removed Captions toggle

            SheetSectionLabel("Wall", tokens)
            SegmentedRow(
                options = AppTheme.entries,
                selected = prefs.theme,
                tokens = tokens,
                label = ::themeLabel,
                onSelect = { onPrefsChange(prefs.copy(theme = it)) },
            )
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String, tokens: GalleryTokens) {
    Text(text, fontSize = 12.sp, color = tokens.textDim, letterSpacing = 0.3.sp)
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SheetSwitchRow(label: String, checked: Boolean, tokens: GalleryTokens, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = tokens.textPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tokens.wall,
                checkedTrackColor = tokens.textPrimary,
                checkedBorderColor = tokens.textPrimary,
                uncheckedThumbColor = tokens.textDim,
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = tokens.hairline,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    tokens: GalleryTokens,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = tokens.textPrimary,
                    activeContentColor = tokens.wall,
                    activeBorderColor = tokens.hairline,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = tokens.textDim,
                    inactiveBorderColor = tokens.hairline,
                ),
                label = { Text(label(option)) },
            )
        }
    }
}

private fun thumbSizeLabel(size: ThumbSize) = when (size) {
    ThumbSize.COMPACT -> "Compact"
    ThumbSize.MEDIUM -> "Medium"
    ThumbSize.LARGE -> "Large"
}

private fun cornerStyleLabel(style: CornerStyle) = when (style) {
    CornerStyle.SQUARE -> "Square"
    CornerStyle.ROUNDED -> "Rounded"
}

private fun themeLabel(theme: AppTheme) = when (theme) {
    AppTheme.DARK -> "Dark"
    AppTheme.LIGHT -> "Light"
    AppTheme.SYSTEM -> "System"
}
