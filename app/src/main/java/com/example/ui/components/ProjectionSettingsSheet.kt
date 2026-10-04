package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geo.CadUnit
import com.example.geo.LayerScheme
import com.example.geo.ProjectionType
import com.example.model.ConversionSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionSettingsSheet(
    currentSettings: ConversionSettings,
    detectedUtmZone: Int,
    detectedIsNorth: Boolean,
    onApplySettings: (ConversionSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var projectionType by remember(currentSettings) { mutableStateOf(currentSettings.projectionType) }
    var manualZone by remember(currentSettings) { mutableIntStateOf(currentSettings.manualUtmZone) }
    var manualIsNorth by remember(currentSettings) { mutableStateOf(currentSettings.manualIsNorth) }
    var cadUnit by remember(currentSettings) { mutableStateOf(currentSettings.cadUnit) }
    var precision by remember(currentSettings) { mutableIntStateOf(currentSettings.precisionDecimals) }
    var is3D by remember(currentSettings) { mutableStateOf(currentSettings.is3D) }
    var layerScheme by remember(currentSettings) { mutableStateOf(currentSettings.layerScheme) }
    var exportLabels by remember(currentSettings) { mutableStateOf(currentSettings.exportLabels) }
    var textHeightStr by remember(currentSettings) { mutableStateOf(currentSettings.textHeight.toString()) }
    var applyGridToGround by remember(currentSettings) { mutableStateOf(currentSettings.applyGridToGround) }
    var groundElevationStr by remember(currentSettings) { mutableStateOf(currentSettings.averageElevationMeters.toString()) }
    var localOriginXStr by remember(currentSettings) { mutableStateOf(currentSettings.localOriginX.toString()) }
    var localOriginYStr by remember(currentSettings) { mutableStateOf(currentSettings.localOriginY.toString()) }

    fun buildAndApply() {
        val th = textHeightStr.toDoubleOrNull() ?: 2.5
        val elev = groundElevationStr.toDoubleOrNull() ?: 0.0
        val ox = localOriginXStr.toDoubleOrNull() ?: 1000.0
        val oy = localOriginYStr.toDoubleOrNull() ?: 1000.0

        val newSettings = currentSettings.copy(
            projectionType = projectionType,
            manualUtmZone = manualZone,
            manualIsNorth = manualIsNorth,
            cadUnit = cadUnit,
            precisionDecimals = precision,
            is3D = is3D,
            layerScheme = layerScheme,
            exportLabels = exportLabels,
            textHeight = th,
            applyGridToGround = applyGridToGround,
            averageElevationMeters = elev,
            localOriginX = ox,
            localOriginY = oy
        )
        onApplySettings(newSettings)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "نظام الإسقاط والإحداثيات الجغرافية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = "دقة هندسية عالية لرسومات أوتوكاد وCivil 3D",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            Button(
                onClick = { buildAndApply() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تطبيق (Apply)")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Projection Selector
        Text(
            text = "نظام الإسقاط (Coordinate Reference System):",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProjectionType.values().forEach { type ->
                val isSelected = projectionType == type
                Surface(
                    onClick = {
                        projectionType = type
                        buildAndApply()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0x330284C7) else Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                projectionType = type
                                buildAndApply()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = type.arabicName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF8FAFC)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (type == ProjectionType.UTM_AUTO) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0x3310B981)) {
                                        Text(
                                            text = "موصى به: Zone ${detectedUtmZone}${if (detectedIsNorth) "N" else "S"}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF34D399),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = type.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Manual UTM Zone selection if UTM_MANUAL
        if (projectionType == ProjectionType.UTM_MANUAL) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "تحديد نطاق UTM يدوياً (Manual Zone 1..60):",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualZone.toString(),
                            onValueChange = {
                                val z = it.toIntOrNull()
                                if (z != null && z in 1..60) {
                                    manualZone = z
                                }
                            },
                            label = { Text("النطاق (Zone 1-60)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("شمال (N)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                            Switch(
                                checked = manualIsNorth,
                                onCheckedChange = { manualIsNorth = it }
                            )
                            Text("جنوب (S)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                        }
                    }
                }
            }
        }

        // Local Tangent Offset options
        if (projectionType == ProjectionType.LOCAL_TANGENT) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "إحداثيات نقطة الصفر المحلية للمشروع (Site Origin Offset):",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = localOriginXStr,
                            onValueChange = { localOriginXStr = it },
                            label = { Text("Easting X0 (م)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = localOriginYStr,
                            onValueChange = { localOriginYStr = it },
                            label = { Text("Northing Y0 (م)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. CAD Drawing Units
        Text(
            text = "وحدات الرسم في أوتوكاد (\$INSUNITS):",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CadUnit.values().take(3).forEach { u ->
                val sel = cadUnit == u
                FilterChip(
                    selected = sel,
                    onClick = {
                        cadUnit = u
                        buildAndApply()
                    },
                    label = { Text(u.arabicName, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CadUnit.values().drop(3).forEach { u ->
                val sel = cadUnit == u
                FilterChip(
                    selected = sel,
                    onClick = {
                        cadUnit = u
                        buildAndApply()
                    },
                    label = { Text(u.arabicName, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Precision & 3D Elevations
        Text(
            text = "الدقة والارتفاعات الهندسية (3D Coordinates):",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Precision decimal slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عدد الخانات العشرية للإحداثيات: $precision",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF1F5F9)
                    )
                    Text(
                        text = when (precision) {
                            4 -> "0.0001 (دقة عشر ملم)"
                            6 -> "0.000001 (دقة ميكرومتر)"
                            8 -> "0.00000001 (فائقة الدقة)"
                            else -> "دقة هندسية"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF38BDF8)
                    )
                }
                Slider(
                    value = precision.toFloat(),
                    onValueChange = { precision = it.toInt() },
                    valueRange = 4f..8f,
                    steps = 3,
                    onValueChangeFinished = { buildAndApply() }
                )

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                // 3D / 2D Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تصدير ثلاثي الأبعاد 3D (Z Elevation)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = if (is3D) "الحفاظ على مناسيب الارتفاع لكل نقطة" else "تسطيح الرسم إلى 2D (Z = 0)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = is3D,
                        onCheckedChange = {
                            is3D = it
                            buildAndApply()
                        }
                    )
                }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                // Grid-to-Ground elevation scale factor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تصحيح مقياس الارتفاع (Grid to Ground)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = "تطبيق Combined Factor للمشاريع المساحية الدقيقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = applyGridToGround,
                        onCheckedChange = {
                            applyGridToGround = it
                            buildAndApply()
                        }
                    )
                }

                if (applyGridToGround) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = groundElevationStr,
                        onValueChange = { groundElevationStr = it },
                        label = { Text("متوسط منسوب سطح الأرض (متر)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Layers & Labels
        Text(
            text = "تنظيم طبقات أوتوكاد (CAD Layers & Labels):",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E293B),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                LayerScheme.values().forEach { scheme ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = layerScheme == scheme,
                            onClick = {
                                layerScheme = scheme
                                buildAndApply()
                            }
                        )
                        Text(
                            text = scheme.arabicName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تصدير نصوص وأسماء العناصر (TEXT Labels)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF1F5F9)
                    )
                    Switch(
                        checked = exportLabels,
                        onCheckedChange = {
                            exportLabels = it
                            buildAndApply()
                        }
                    )
                }

                if (exportLabels) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = textHeightStr,
                        onValueChange = { textHeightStr = it },
                        label = { Text("ارتفاع الخط بوحدات الرسم (Text Height)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
