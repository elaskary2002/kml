package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ConversionProcessor
import com.example.geo.CadUnit
import java.util.Locale

@Composable
fun EngineeringStatsCard(
    result: ConversionProcessor.ConversionResult?,
    cadUnit: CadUnit,
    modifier: Modifier = Modifier
) {
    if (result == null) return

    val bb = result.drawing.boundingBox
    val uName = cadUnit.displayName.substringBefore(" ")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تقرير وبيانات الإحداثيات الهندسية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1F5F9)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x330284C7)
                ) {
                    Text(
                        text = result.drawing.utmZoneText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = result.drawing.crsDescription,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(12.dp))

            // Grid Bounding Box Table
            Text(
                text = "نطاق الرسم الهندسي (Bounding Box):",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                CoordRow("Easting (X Min/Max)", "%,.2f .. %,.2f %s".format(Locale.US, bb.minX, bb.maxX, uName), "ΔX = %,.2f %s".format(Locale.US, bb.deltaX, uName))
                Spacer(modifier = Modifier.height(6.dp))
                CoordRow("Northing (Y Min/Max)", "%,.2f .. %,.2f %s".format(Locale.US, bb.minY, bb.maxY, uName), "ΔY = %,.2f %s".format(Locale.US, bb.deltaY, uName))
                Spacer(modifier = Modifier.height(6.dp))
                CoordRow("Elevation (Z Min/Max)", "%,.2f .. %,.2f %s".format(Locale.US, bb.minZ, bb.maxZ, uName), "ΔZ = %,.2f %s".format(Locale.US, bb.deltaZ, uName))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Survey Metrics (Length, Area, Vertices)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile(
                    title = "إجمالي الأطوال",
                    value = "%,.1f %s".format(Locale.US, result.totalLengthMeters * cadUnit.scaleFromMeters, uName),
                    subtitle = if (cadUnit == CadUnit.METERS) "%,.3f كم".format(Locale.US, result.totalLengthMeters / 1000.0) else "",
                    icon = Icons.Default.Timeline,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "المساحة المحصورة",
                    value = "%,.1f %s²".format(Locale.US, result.totalAreaSquareMeters * cadUnit.scaleFromMeters * cadUnit.scaleFromMeters, uName),
                    subtitle = if (cadUnit == CadUnit.METERS && result.totalAreaSquareMeters > 0) {
                        "%,.2f هكتار / %,.2f فدان".format(Locale.US, result.totalAreaSquareMeters / 10000.0, result.totalAreaSquareMeters / 4200.83)
                    } else "",
                    icon = Icons.Default.CropFree,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile(
                    title = "مقياس UTM (k)",
                    value = String.format(Locale.US, "%.7f", result.gridScaleFactor),
                    subtitle = "معامل انحراف الإسقاط",
                    icon = Icons.Default.Tune,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "خط الزوال المركزي",
                    value = "%.1f° E".format(Locale.US, result.centralMeridianDeg),
                    subtitle = "Central Meridian",
                    icon = Icons.Default.Explore,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CoordRow(label: String, span: String, delta: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.5f)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            Text(
                text = span,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = delta,
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = Color(0xFF10B981),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }
        }
    }
}
