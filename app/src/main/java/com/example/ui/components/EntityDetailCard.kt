package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dxf.CadEntity
import com.example.dxf.CadPointEntity
import com.example.dxf.CadPolylineEntity
import com.example.dxf.CadTextEntity
import com.example.geo.CadUnit
import java.util.Locale

@Composable
fun EntityDetailCard(
    entity: CadEntity?,
    cadUnit: CadUnit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (entity == null) return

    val uName = cadUnit.displayName.substringBefore(" ")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF01E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entity.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0x3338BDF8)) {
                        Text(
                            text = "Layer: ${entity.layer}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (entity) {
                is CadPointEntity -> {
                    val p = entity.point
                    Text(
                        text = "Easting (X): ${String.format(Locale.US, "%,.3f", p.x)} $uName",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Northing (Y): ${String.format(Locale.US, "%,.3f", p.y)} $uName",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF34D399)
                    )
                    Text(
                        text = "Elevation (Z): ${String.format(Locale.US, "%,.3f", p.z)} $uName",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFFF59E0B)
                    )
                }
                is CadPolylineEntity -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "عدد النقاط (Vertices): ${entity.points.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = if (entity.isClosed) "مضلع مغلق (Closed Polygon)" else "مسار مفتوح (Open Line)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Text(
                        text = "الطول الإجمالي (Length): ${String.format(Locale.US, "%,.2f", entity.length)} $uName",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (entity.isClosed && entity.area > 0) {
                        Text(
                            text = "المساحة (Area): ${String.format(Locale.US, "%,.2f", entity.area)} $uName²",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFFFDE047),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is CadTextEntity -> {
                    Text(
                        text = "النص: ${entity.text}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFDE047)
                    )
                    Text(
                        text = "الموقع: X=${String.format(Locale.US, "%.2f", entity.position.x)}, Y=${String.format(Locale.US, "%.2f", entity.position.y)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            if (!entity.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الوصف: ${entity.description}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
