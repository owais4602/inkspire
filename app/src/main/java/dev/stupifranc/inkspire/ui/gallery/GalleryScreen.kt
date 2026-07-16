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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch
import dev.stupifranc.inkspire.core.ItemBox
import dev.stupifranc.inkspire.core.reorderTarget
import dev.stupifranc.inkspire.ui.components.ShakeDetector

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

    var showCustomizeSheet by remember { mutableStateOf(false) }
    
    val gridState = rememberLazyStaggeredGridState()
    var draggedId by remember { mutableStateOf<String?>(null) }
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var dropTargetId by remember { mutableStateOf<String?>(null) }
    var isHoveringDeleteBin by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    ShakeDetector {
        viewModel.shuffleDrawings()
    }

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
                    state = gridState,
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
                        val isDragged = draggedId == meta.id
                        val isDropTarget = dropTargetId == meta.id
                        
                        val liftScale by animateFloatAsState(if (isDragged) 1.06f else 1f, spring(), label = "scale")
                        val liftElevation by animateDpAsState(if (isDragged) 12.dp else 0.dp, spring(), label = "elevation")
                        val targetScale by animateFloatAsState(if (isDropTarget && !isDragged) 0.94f else 1f, label = "targetScale")
                        val targetAlpha by animateFloatAsState(if (isDropTarget && !isDragged) 0.4f else 1f, label = "targetAlpha")
                        
                        val cornerShape = if (meta.shape == CanvasShape.RECTANGLE) {
                            RoundedCornerShape(prefs.cornerStyle.radiusDp.dp)
                        } else {
                            canvasOutlineShape(meta.shape)
                        }
                        
                        Box(modifier = Modifier
                            .animateItem()
                            .zIndex(if (isDragged) 1f else 0f)
                            .pointerInput(meta.id) {
                                detectTapGestures(
                                    onTap = { onOpenDrawing(meta.id) }
                                )
                            }
                            .pointerInput(meta.id, "drag") {
                                var dragStart = Offset.Zero
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { startPosition -> 
                                        draggedId = meta.id
                                        dragStart = startPosition
                                        scope.launch { dragOffset.snapTo(Offset.Zero) }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        scope.launch { dragOffset.snapTo(dragOffset.value + dragAmount) }
                                        
                                        val layoutInfo = gridState.layoutInfo
                                        val visibleItems = layoutInfo.visibleItemsInfo
                                        
                                        val myItem = visibleItems.find { it.key == meta.id }
                                        if (myItem != null) {
                                            val fingerX = myItem.offset.x + dragStart.x + dragOffset.value.x
                                            val fingerY = myItem.offset.y + dragStart.y + dragOffset.value.y
                                            
                                            val binTop = 0f
                                            val binBottom = with(density) { 150.dp.toPx() }
                                            val binWidth = with(density) { 150.dp.toPx() }
                                            val binLeft = (layoutInfo.viewportSize.width - binWidth) / 2f
                                            val binRight = binLeft + binWidth
                                            
                                            if (fingerY < binBottom && fingerX in binLeft..binRight) {
                                                if (!isHoveringDeleteBin) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                isHoveringDeleteBin = true
                                                dropTargetId = null
                                            } else {
                                                if (isHoveringDeleteBin) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                isHoveringDeleteBin = false
                                                
                                                val boxes = visibleItems
                                                    .filter { it.key is String && viewModel.drawings.any { d -> d.id == it.key } }
                                                    .map {
                                                        ItemBox(
                                                            key = it.key as String,
                                                            left = it.offset.x.toFloat(),
                                                            top = it.offset.y.toFloat(),
                                                            width = it.size.width.toFloat(),
                                                            height = it.size.height.toFloat()
                                                        )
                                                    }
                                                
                                                val newTarget = reorderTarget(fingerX, fingerY, boxes, meta.id)
                                                if (newTarget != dropTargetId && newTarget != null) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                                dropTargetId = newTarget
                                            }
                                            
                                            val topBound = layoutInfo.viewportStartOffset.toFloat()
                                            val bottomBound = layoutInfo.viewportEndOffset.toFloat()
                                            val edgeThreshold = with(density) { 64.dp.toPx() }
                                            if (fingerY < topBound + edgeThreshold) {
                                                scope.launch { gridState.scrollBy(-16f) }
                                            } else if (fingerY > bottomBound - edgeThreshold) {
                                                scope.launch { gridState.scrollBy(16f) }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (isHoveringDeleteBin) {
                                            val deletedId = meta.id
                                            viewModel.hideDrawing(deletedId)
                                            draggedId = null
                                            dropTargetId = null
                                            isHoveringDeleteBin = false

                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Drawing deleted",
                                                    actionLabel = "Undo",
                                                    duration = androidx.compose.material3.SnackbarDuration.Short
                                                )
                                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    viewModel.restoreDrawing(deletedId)
                                                } else {
                                                    viewModel.delete(deletedId)
                                                }
                                            }
                                        } else if (dragOffset.value == Offset.Zero) {
                                            draggedId = null
                                            dropTargetId = null
                                        } else {
                                            if (dropTargetId != null) {
                                                viewModel.dropOnto(meta.id, dropTargetId!!)
                                                draggedId = null
                                                dropTargetId = null
                                            } else {
                                                scope.launch { 
                                                    dragOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.7f))
                                                    draggedId = null
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isHoveringDeleteBin = false
                                        scope.launch { 
                                            dragOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.7f))
                                            draggedId = null
                                            dropTargetId = null
                                        }
                                    }
                                )
                            }
                            .graphicsLayer {
                                if (isDragged) {
                                    translationX = dragOffset.value.x
                                    translationY = dragOffset.value.y
                                    scaleX = liftScale
                                    scaleY = liftScale
                                    shadowElevation = liftElevation.toPx()
                                    alpha = 0.9f
                                } else {
                                    scaleX = targetScale
                                    scaleY = targetScale
                                    alpha = targetAlpha
                                }
                            }
                        ) {
                            GalleryPiece(
                                meta = meta,
                                thumbnailPath = viewModel.thumbnailFile(meta.id)?.path,
                                prefs = prefs,
                                tokens = tokens,
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
            
            androidx.compose.animation.AnimatedVisibility(
                visible = draggedId != null,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = insets.calculateTopPadding() + 24.dp)
            ) {
                val binScale by animateFloatAsState(if (isHoveringDeleteBin) 1.2f else 1f, label = "binScale")
                val binColor = if (isHoveringDeleteBin) DeleteRed else tokens.textPrimary
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = binScale
                            scaleY = binScale
                        }
                        .background(tokens.surface.copy(alpha = 0.9f), RoundedCornerShape(50))
                        .border(1.dp, tokens.hairline, RoundedCornerShape(50))
                        .padding(16.dp)
                ) {
                    CrossIcon(tint = binColor)
                }
            }

            androidx.compose.material3.SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = insets.calculateBottomPadding() + 80.dp)
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
                .background(
                    when {
                        meta.background != null && meta.background.colors.isNotEmpty() -> Color(meta.background.colors.first())
                        meta.backgroundColorArgb == 0 -> tokens.surface
                        else -> Color(meta.backgroundColorArgb)
                    }
                )
                .then(if (prefs.borderEnabled) Modifier.border(1.dp, tokens.hairline, cornerShape) else Modifier),
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

@Composable
private fun CrossIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = tint,
            start = Offset(4.dp.toPx(), 4.dp.toPx()),
            end = Offset(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(size.width - 4.dp.toPx(), 4.dp.toPx()),
            end = Offset(4.dp.toPx(), size.height - 4.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
