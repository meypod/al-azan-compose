package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

/**
 * A settings row that opens an HSV color picker (hue + saturation/value panel, alpha slider and a hex
 * field). [colorArgb] is the stored ARGB value, or null to follow [defaultArgb] (the theme-adaptive
 * default); [onColorChanged] emits null when the user chooses "match theme".
 */
@Composable
fun ColorPickerField(
    label: String,
    colorArgb: Int?,
    defaultArgb: Int,
    onColorChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            if (colorArgb == null) {
                Text(
                    stringResource(R.string.color_picker_use_default),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ColorSwatch(colorArgb ?: defaultArgb, Modifier.size(36.dp))
    }
    if (showDialog) {
        ColorPickerDialog(
            title = label,
            initialArgb = colorArgb,
            defaultArgb = defaultArgb,
            onDismiss = { showDialog = false },
            onConfirm = {
                onColorChanged(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun ColorPickerDialog(
    title: String,
    initialArgb: Int?,
    defaultArgb: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
) {
    val start = initialArgb ?: defaultArgb
    val hsv = remember { FloatArray(3).also { AndroidColor.colorToHSV(start, it) } }
    var hue by remember { mutableStateOf(hsv[0]) }
    var sat by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }
    var alpha by remember { mutableStateOf(AndroidColor.alpha(start) / 255f) }
    var hexText by remember { mutableStateOf(argbToHex(start)) }
    var hexEditing by remember { mutableStateOf(false) }

    val argb = AndroidColor.HSVToColor((alpha * 255).roundToInt(), floatArrayOf(hue, sat, value))

    // Keep the hex field mirrored to the pickers unless the user is actively typing into it.
    LaunchedEffect(argb) {
        if (!hexEditing) hexText = argbToHex(argb)
    }

    fun applyColor(color: Int) {
        val out = FloatArray(3)
        AndroidColor.colorToHSV(color, out)
        hue = out[0]
        sat = out[1]
        value = out[2]
        alpha = AndroidColor.alpha(color) / 255f
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SaturationValuePanel(
                        hue = hue,
                        saturation = sat,
                        value = value,
                        onChange = { s, v ->
                            sat = s
                            value = v
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(150.dp),
                    )
                    HueBar(
                        hue = hue,
                        onChange = { hue = it },
                        modifier = Modifier
                            .width(28.dp)
                            .height(150.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                AlphaSlider(
                    alpha = alpha,
                    opaqueArgb = AndroidColor.HSVToColor(255, floatArrayOf(hue, sat, value)),
                    onChange = { alpha = it },
                )
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexEditing = true
                        hexText = input
                        parseHex(input)?.let { applyColor(it) }
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.color_picker_hex)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (!it.isFocused) hexEditing = false },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(argb) }) { Text(stringResource(R.string.okay)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onConfirm(null) }) {
                    Text(stringResource(R.string.color_picker_use_default), overflow = TextOverflow.Ellipsis, maxLines = 1)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val hueColor = Color(AndroidColor.HSVToColor(255, floatArrayOf(hue, 1f, 1f)))
    fun update(offset: Offset) {
        if (size.width == 0 || size.height == 0) return
        val s = (offset.x / size.width).coerceIn(0f, 1f)
        val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
        onChange(s, v)
    }
    Box(
        modifier
            .clip(MaterialTheme.shapes.small)
            .onSizeChanged { size = it }
            .pointerInput(Unit) { detectTapGestures { update(it) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> update(change.position) } },
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val cx = saturation * this.size.width
            val cy = (1f - value) * this.size.height
            drawCircle(Color.White, radius = 6.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val hueColors = remember {
        (0..6).map { Color(AndroidColor.HSVToColor(255, floatArrayOf(it * 60f, 1f, 1f))) }
    }

    fun update(offset: Offset) {
        if (size.height == 0) return
        onChange((offset.y / size.height).coerceIn(0f, 1f) * 360f)
    }
    Box(
        modifier
            .clip(MaterialTheme.shapes.small)
            .onSizeChanged { size = it }
            .pointerInput(Unit) { detectTapGestures { update(it) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> update(change.position) } },
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            drawRect(Brush.verticalGradient(hueColors))
            val y = (hue / 360f) * this.size.height
            drawRect(
                color = Color.White,
                topLeft = Offset(0f, y - 1.5.dp.toPx()),
                size = Size(this.size.width, 3.dp.toPx()),
            )
        }
    }
}

@Composable
private fun AlphaSlider(
    alpha: Float,
    opaqueArgb: Int,
    onChange: (Float) -> Unit,
) {
    val opaque = Color(opaqueArgb)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.color_picker_alpha), style = MaterialTheme.typography.bodyMedium)
            Text(
                "${(alpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(Modifier.fillMaxWidth()) {
            // Checkerboard + transparent→opaque gradient: the universal "this controls transparency" cue.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.small)
                    .drawBehind {
                        drawRect(Color.White)
                        val cell = 5.dp.toPx()
                        var y = 0f
                        var rowIndex = 0
                        while (y < size.height) {
                            var x = 0f
                            var colIndex = 0
                            while (x < size.width) {
                                if ((rowIndex + colIndex) % 2 == 0) {
                                    drawRect(Color(0xFFBFBFBF), topLeft = Offset(x, y), size = Size(cell, cell))
                                }
                                x += cell
                                colIndex++
                            }
                            y += cell
                            rowIndex++
                        }
                    }
                    .background(Brush.horizontalGradient(listOf(opaque.copy(alpha = 0f), opaque))),
            )
            Slider(
                value = alpha,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                // Hide the default track so the checkerboard gradient shows; keep the thumb.
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    argb: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(Color(argb))
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
    )
}

private fun argbToHex(argb: Int): String = "#%08X".format(argb)

/** Parses "#AARRGGBB", "#RRGGBB" (opaque) or the same without the leading '#'; null when malformed. */
private fun parseHex(text: String): Int? {
    val hex = text.trim().removePrefix("#")
    return when (hex.length) {
        6 -> hex.toLongOrNull(16)?.let { (0xFF000000L or it).toInt() }
        8 -> hex.toLongOrNull(16)?.toInt()
        else -> null
    }
}
