package com.example.ui.cad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dxf.*
import com.example.geo.BoundingBox3D
import com.example.geo.CadPoint
import com.example.geo.CadUnit
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun CadCanvasView(
    drawing: CadDrawing,
    cadUnit: CadUnit,
    modifier: Modifier = Modifier,
    onEntitySelected: ((CadEntity?) -> Unit)? = null
) {
    var scale by remember(drawing) { mutableFloatStateOf(1f) }
    var panOffset by remember(drawing) { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    var showGrid by remember { mutableStateOf(true) }
    var showLabels by remember { mutableStateOf(true) }
    var showPoints by remember { mutableStateOf(true) }
    var showLines by remember { mutableStateOf(true) }
    var showPolygons by remember { mutableStateOf(true) }

    var touchedCadCoord by remember { mutableStateOf<CadPoint?>(null) }
    var selectedEntity by remember { mutableStateOf<CadEntity?>(null) }

    val textMeasurer = rememberTextMeasurer()

    val bb = drawing.boundingBox
    val bbDx = if (bb.deltaX > 0.0001) bb.deltaX else 100.0
    val bbDy = if (bb.deltaY > 0.0001) bb.deltaY else 100.0
    val bbCenterX = bb.centerX
    val bbCenterY = bb.centerY

    // Fit to extents function
    fun fitToExtents(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        val padding = 48f
        val availW = (w - padding * 2).coerceAtLeast(100f)
        val availH = (h - padding * 2).coerceAtLeast(100f)

        val scaleX = availW / bbDx.toFloat()
        val scaleY = availH / bbDy.toFloat()
        val optimalScale = min(scaleX, scaleY).coerceIn(0.00001f, 5000f)

        scale = optimalScale
        // Center the CAD bounding box in the viewport
        panOffset = Offset(
            x = (w / 2f) - (bbCenterX.toFloat() * optimalScale),
            y = (h / 2f) + (bbCenterY.toFloat() * optimalScale) // In CAD, Y increases upward, in screen Y increases downward
        )
    }

    // Auto-fit when canvas size is acquired
    LaunchedEffect(canvasSize, drawing) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            fitToExtents(canvasSize.width, canvasSize.height)
        }
    }

    // Screen to CAD coordinate mapping
    fun screenToCad(screen: Offset): CadPoint {
        val cadX = (screen.x - panOffset.x) / scale
        val cadY = (panOffset.y - screen.y) / scale
        return CadPoint(cadX.toDouble(), cadY.toDouble(), 0.0)
    }

    // CAD to Screen coordinate mapping
    fun cadToScreen(cad: CadPoint): Offset {
        val sx = panOffset.x + (cad.x.toFloat() * scale)
        val sy = panOffset.y - (cad.y.toFloat() * scale)
        return Offset(sx, sy)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B1320))
            .clipToBounds()
            .pointerInput(drawing) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(0.00001f, 100000f)
                    // Zoom towards centroid
                    panOffset = Offset(
                        x = centroid.x - (centroid.x - panOffset.x) * (newScale / oldScale) + pan.x,
                        y = centroid.y - (centroid.y - panOffset.y) * (newScale / oldScale) + pan.y
                    )
                    scale = newScale
                }
            }
            .pointerInput(drawing) {
                detectTapGestures(
                    onTap = { screenOffset ->
                        val cadPt = screenToCad(screenOffset)
                        touchedCadCoord = cadPt

                        // Find closest entity within hit tolerance (15 dp)
                        var closest: CadEntity? = null
                        var minDist = 40.0 * 40.0 // in screen pixels squared

                        for (ent in drawing.entities) {
                            when (ent) {
                                is CadPointEntity -> {
                                    val sp = cadToScreen(ent.point)
                                    val dx = sp.x - screenOffset.x
                                    val dy = sp.y - screenOffset.y
                                    val distSq = (dx * dx + dy * dy).toDouble()
                                    if (distSq < minDist) {
                                        minDist = distSq
                                        closest = ent
                                    }
                                }
                                is CadPolylineEntity -> {
                                    for (pt in ent.points) {
                                        val sp = cadToScreen(pt)
                                        val dx = sp.x - screenOffset.x
                                        val dy = sp.y - screenOffset.y
                                        val distSq = (dx * dx + dy * dy).toDouble()
                                        if (distSq < minDist) {
                                            minDist = distSq
                                            closest = ent
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                        selectedEntity = closest
                        onEntitySelected?.invoke(closest)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().testTag("cad_canvas")) {
            canvasSize = size

            // 1. Draw Engineering Grid Lines & Axis Ticks
            if (showGrid && scale > 0) {
                drawCadGrid(
                    size = size,
                    panOffset = panOffset,
                    scale = scale,
                    textMeasurer = textMeasurer,
                    unit = cadUnit
                )
            }

            // 2. Draw Polygons
            if (showPolygons) {
                for (ent in drawing.entities) {
                    if (ent is CadPolylineEntity && ent.isClosed && ent.points.size >= 3) {
                        val path = Path()
                        val firstScreen = cadToScreen(ent.points.first())
                        path.moveTo(firstScreen.x, firstScreen.y)
                        for (i in 1 until ent.points.size) {
                            val sp = cadToScreen(ent.points[i])
                            path.lineTo(sp.x, sp.y)
                        }
                        path.close()

                        val isSel = ent == selectedEntity
                        // Fill with translucent layer color
                        val fillColor = if (isSel) Color(0x60F59E0B) else Color(0x3510B981)
                        val strokeColor = if (isSel) Color(0xFFF59E0B) else Color(0xFF10B981)

                        drawPath(path, color = fillColor)
                        drawPath(path, color = strokeColor, style = Stroke(width = if (isSel) 4f else 2.5f))
                    }
                }
            }

            // 3. Draw Lines / Alignments
            if (showLines) {
                for (ent in drawing.entities) {
                    if (ent is CadPolylineEntity && !ent.isClosed && ent.points.size >= 2) {
                        val isSel = ent == selectedEntity
                        val strokeColor = if (isSel) Color(0xFFF59E0B) else Color(0xFF38BDF8)
                        val strokeWidth = if (isSel) 4f else 2.5f

                        for (i in 0 until ent.points.size - 1) {
                            val p1 = cadToScreen(ent.points[i])
                            val p2 = cadToScreen(ent.points[i + 1])
                            drawLine(
                                color = strokeColor,
                                start = p1,
                                end = p2,
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // 4. Draw Survey Points (PDMODE 34 style: Circle with Cross)
            if (showPoints) {
                for (ent in drawing.entities) {
                    if (ent is CadPointEntity) {
                        val sp = cadToScreen(ent.point)
                        val isSel = ent == selectedEntity
                        val pointColor = if (isSel) Color(0xFFF59E0B) else Color(0xFFEF4444)
                        val r = if (isSel) 8f else 5.5f

                        // Circle
                        drawCircle(
                            color = pointColor,
                            radius = r,
                            center = sp,
                            style = Stroke(width = 2.2f)
                        )
                        // Cross
                        drawLine(
                            color = pointColor,
                            start = Offset(sp.x - r - 3f, sp.y),
                            end = Offset(sp.x + r + 3f, sp.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = pointColor,
                            start = Offset(sp.x, sp.y - r - 3f),
                            end = Offset(sp.x, sp.y + r + 3f),
                            strokeWidth = 2f
                        )
                    }
                }
            }

            // 5. Draw Text Labels
            if (showLabels) {
                for (ent in drawing.entities) {
                    if (ent is CadTextEntity) {
                        val sp = cadToScreen(ent.position)
                        // Only draw if within visible canvas bounds
                        if (sp.x in -100f..(size.width + 100f) && sp.y in -100f..(size.height + 100f)) {
                            val measured = textMeasurer.measure(
                                text = AnnotatedString(ent.text),
                                style = TextStyle(
                                    color = Color(0xFFFDE047),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(sp.x + 8f, sp.y - 14f)
                            )
                        }
                    }
                }
            }

            // 6. Draw Touched Crosshair Cursor
            touchedCadCoord?.let { coord ->
                val sp = cadToScreen(coord)
                val crossColor = Color(0x8038BDF8)
                // Full canvas crosshair
                drawLine(
                    color = crossColor,
                    start = Offset(0f, sp.y),
                    end = Offset(size.width, sp.y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
                drawLine(
                    color = crossColor,
                    start = Offset(sp.x, 0f),
                    end = Offset(sp.x, size.height),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 4f,
                    center = sp
                )
            }
        }

        // Top-left: Floating Engineering Crosshair HUD Coordinates
        touchedCadCoord?.let { coord ->
            Surface(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xEB0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                shadowElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        text = "CAD Coordinate Inspector",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "E: ${String.format(Locale.US, "%,.3f", coord.x)} ${cadUnit.displayName.substringBefore(" ")}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "N: ${String.format(Locale.US, "%,.3f", coord.y)} ${cadUnit.displayName.substringBefore(" ")}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom CAD Control Bar (Zoom Extents, Toggle Grid, Filter Layers)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xEE1E293B),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fit to Extents Action
                IconButton(
                    onClick = { fitToExtents(canvasSize.width, canvasSize.height) },
                    modifier = Modifier.testTag("btn_zoom_extents")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = "Zoom Extents (احتواء الكل)",
                        tint = Color(0xFF38BDF8)
                    )
                }

                // Grid Toggle
                IconButton(onClick = { showGrid = !showGrid }) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Toggle Grid",
                        tint = if (showGrid) Color(0xFF38BDF8) else Color(0xFF64748B)
                    )
                }

                // Layer Filter FilterChips
                FilterChip(
                    selected = showPoints,
                    onClick = { showPoints = !showPoints },
                    label = { Text("Points", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x33EF4444),
                        selectedLabelColor = Color(0xFFFCA5A5)
                    )
                )

                FilterChip(
                    selected = showLines,
                    onClick = { showLines = !showLines },
                    label = { Text("Lines", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x330284C7),
                        selectedLabelColor = Color(0xFF7DD3FC)
                    )
                )

                FilterChip(
                    selected = showPolygons,
                    onClick = { showPolygons = !showPolygons },
                    label = { Text("Polys", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x3310B981),
                        selectedLabelColor = Color(0xFF6EE7B7)
                    )
                )

                FilterChip(
                    selected = showLabels,
                    onClick = { showLabels = !showLabels },
                    label = { Text("Labels", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x33EAB308),
                        selectedLabelColor = Color(0xFFFDE047)
                    )
                )
            }
        }
    }
}

/**
 * Draws CAD Cartesian grid lines with coordinates along the edges.
 */
@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawCadGrid(
    size: Size,
    panOffset: Offset,
    scale: Float,
    textMeasurer: TextMeasurer,
    unit: CadUnit
) {
    if (scale <= 0.000001f) return

    val minVisibleCadX = (0f - panOffset.x) / scale
    val maxVisibleCadX = (size.width - panOffset.x) / scale
    val minVisibleCadY = (panOffset.y - size.height) / scale
    val maxVisibleCadY = (panOffset.y - 0f) / scale

    val visibleRangeX = maxVisibleCadX - minVisibleCadX
    if (visibleRangeX <= 0) return

    // Choose grid step size dynamically (powers of 10, e.g. 1, 5, 10, 50, 100, 500, 1000m)
    val rawStep = visibleRangeX / 6.0
    val exponent = floor(log10(rawStep))
    val base = 10.0.pow(exponent)
    val fraction = rawStep / base
    val step = when {
        fraction < 2.0 -> 1.0 * base
        fraction < 5.0 -> 2.0 * base
        else -> 5.0 * base
    }

    val gridColor = Color(0x1F38BDF8)
    val axisColor = Color(0x4038BDF8)
    val textStyle = TextStyle(
        color = Color(0x8094A3B8),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
    )

    // Vertical lines (constant X / Easting)
    val startX = floor(minVisibleCadX / step) * step
    var currX = startX
    while (currX <= maxVisibleCadX + step) {
        val sx = panOffset.x + (currX.toFloat() * scale)
        if (sx in 0f..size.width) {
            drawLine(
                color = if (abs(currX) < step * 0.1) axisColor else gridColor,
                start = Offset(sx, 0f),
                end = Offset(sx, size.height),
                strokeWidth = if (abs(currX) < step * 0.1) 1.5f else 0.8f
            )
            // Draw coordinate text label at top
            val labelText = String.format(Locale.US, "%,.0f", currX)
            val measured = textMeasurer.measure(AnnotatedString(labelText), textStyle)
            drawText(measured, topLeft = Offset(sx + 3f, 4f))
        }
        currX += step
    }

    // Horizontal lines (constant Y / Northing)
    val startY = floor(minVisibleCadY / step) * step
    var currY = startY
    while (currY <= maxVisibleCadY + step) {
        val sy = panOffset.y - (currY.toFloat() * scale)
        if (sy in 0f..size.height) {
            drawLine(
                color = if (abs(currY) < step * 0.1) axisColor else gridColor,
                start = Offset(0f, sy),
                end = Offset(size.width, sy),
                strokeWidth = if (abs(currY) < step * 0.1) 1.5f else 0.8f
            )
            val labelText = String.format(Locale.US, "%,.0f", currY)
            val measured = textMeasurer.measure(AnnotatedString(labelText), textStyle)
            drawText(measured, topLeft = Offset(4f, sy - 14f))
        }
        currY += step
    }
}
