package dev.stupifranc.inkspire.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.stupifranc.inkspire.core.argbToHex
import dev.stupifranc.inkspire.core.argbToHsva
import dev.stupifranc.inkspire.core.hexToArgb
import dev.stupifranc.inkspire.core.hsvaToArgb

@Composable
fun ColorPicker(
    colorArgb: Int,
    recentColors: List<Int>,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hsva = argbToHsva(colorArgb)

    Column(modifier = modifier) {
        SaturationValueSquare(
            hue = hsva.hue,
            saturation = hsva.saturation,
            value = hsva.value,
            onChange = { s, v -> onColorChange(hsvaToArgb(hsva.copy(saturation = s, value = v))) },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f),
        )

        Spacer(modifier = Modifier.height(12.dp))

        HueSlider(
            hue = hsva.hue,
            onChange = { h -> onColorChange(hsvaToArgb(hsva.copy(hue = h))) },
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        AlphaSlider(
            alpha = hsva.alpha,
            onChange = { a -> onColorChange(hsvaToArgb(hsva.copy(alpha = a))) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        HexField(colorArgb = colorArgb, onColorChange = onColorChange)

        if (recentColors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Recent")
            Row(modifier = Modifier.padding(top = 4.dp)) {
                recentColors.forEach { recent ->
                    ColorSwatch(
                        colorArgb = recent,
                        onClick = { onColorChange(recent) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SaturationValueSquare(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (s: Float, v: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onChangeState = rememberUpdatedState(onChange)
    val hueColor = Color.hsv(((hue % 360f) + 360f) % 360f, 1f, 1f)

    fun updateFrom(offset: Offset, size: IntSize) {
        val s = (offset.x / size.width).coerceIn(0f, 1f)
        val v = (1f - offset.y / size.height).coerceIn(0f, 1f)
        onChangeState.value(s, v)
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset -> updateFrom(offset, size) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> updateFrom(change.position, size) }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(saturation * size.width, (1f - value) * size.height),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun HueSlider(hue: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val onChangeState = rememberUpdatedState(onChange)
    val hueColors = remember {
        (0..6).map { Color.hsv(it * 60f, 1f, 1f) }
    }

    fun updateFrom(x: Float, width: Int) {
        onChangeState.value((x / width).coerceIn(0f, 1f) * 360f)
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures { offset -> updateFrom(offset.x, size.width) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> updateFrom(change.position.x, size.width) }
            },
    ) {
        drawRect(Brush.horizontalGradient(hueColors))
        val thumbX = (hue / 360f).coerceIn(0f, 1f) * size.width
        drawCircle(
            color = Color.White,
            radius = size.height / 2f,
            center = Offset(thumbX, size.height / 2f),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun AlphaSlider(alpha: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val onChangeState = rememberUpdatedState(onChange)

    fun updateFrom(x: Float, width: Int) {
        onChangeState.value((x / width).coerceIn(0f, 1f))
    }

    Canvas(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures { offset -> updateFrom(offset.x, size.width) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> updateFrom(change.position.x, size.width) }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0f), Color.Black)))
        val thumbX = alpha.coerceIn(0f, 1f) * size.width
        drawCircle(
            color = Color.White,
            radius = size.height / 2f,
            center = Offset(thumbX, size.height / 2f),
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun HexField(colorArgb: Int, onColorChange: (Int) -> Unit) {
    var text by remember(colorArgb) { mutableStateOf(argbToHex(colorArgb)) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            hexToArgb(newText)?.let(onColorChange)
        },
        label = { Text("Hex") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColorSwatch(colorArgb: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
    ) {
        drawRect(Color(colorArgb))
    }
}
