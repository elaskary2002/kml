package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DxfViewerDialog(
    dxfContent: String?,
    fileName: String,
    onDismiss: () -> Unit
) {
    if (dxfContent == null) return

    val context = LocalContext.current
    val lines = remember(dxfContent) { dxfContent.lines() }
    val totalLines = lines.size
    val sizeKb = (dxfContent.toByteArray(Charsets.US_ASCII).size / 1024.0)

    var searchQuery by remember { mutableStateOf("") }
    val displayedLines = remember(lines, searchQuery) {
        if (searchQuery.isBlank()) {
            lines.take(800) // Display first 800 lines for high performance
        } else {
            lines.filter { it.contains(searchQuery, ignoreCase = true) }.take(500)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "معاينة كود أوتوكاد DXF (ASCII)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                        Text(
                            text = "$fileName • %,d سطر • %.1f KB • AC1015 (R2000)".format(totalLines, sizeKb),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("AutoCAD DXF", dxfContent)
                                cm.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود DXF للحافظة", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ DXF", tint = Color(0xFF38BDF8))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = Color(0xFF94A3B8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث في وسوم DXF (مثل: HEADER, POINT, LWPOLYLINE, LAYER...)") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    } else null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // DXF Code Listing
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF030712), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(displayedLines) { index, line ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%4d ", index + 1),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = Color(0xFF475569)
                                )
                                val isGroupCode = line.trim().toIntOrNull() != null
                                val isSection = line.contains("SECTION") || line.contains("ENDSEC") || line.contains("EOF")
                                val isEntity = line == "POINT" || line == "LWPOLYLINE" || line == "POLYLINE" || line == "TEXT"

                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = when {
                                        isSection -> Color(0xFFF59E0B)
                                        isEntity -> Color(0xFF10B981)
                                        isGroupCode -> Color(0xFF38BDF8)
                                        else -> Color(0xFFE2E8F0)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (totalLines > 800 && searchQuery.isBlank()) {
                    Text(
                        text = "يتم عرض أول 800 سطر من أصل %,d سطر. الملف الكامل جاهز للتصدير والمشاركة بأكمله.".format(totalLines),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
