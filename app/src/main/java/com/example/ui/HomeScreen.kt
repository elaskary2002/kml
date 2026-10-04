package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.cad.CadCanvasView
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyRecords by viewModel.historyRecords.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: CAD, 1: Settings, 2: Report
    var showSamplesDialog by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showDxfDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotification()
        }
    }

    LaunchedEffect(uiState.successNotification) {
        uiState.successNotification?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotification()
        }
    }

    // SAF Document Open Launcher for KML and KMZ files
    val openKmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "imported.kml"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: "imported.kml"
                }
            }
            viewModel.loadKmlFromUri(uri, fileName)
        }
    }

    // SAF Document Create Launcher for exporting DXF
    val saveDxfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/dxf")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveDxfToUri(
                targetUri = uri,
                onSuccess = {
                    Toast.makeText(context, "تم حفظ ملف DXF بنجاح", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Share DXF Intent function
    fun shareDxfFile() {
        val file = viewModel.getShareableDxfFile()
        if (file != null) {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/dxf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "AutoCAD Drawing - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة ملف DXF مع أوتوكاد أو التطبيقات"))
        } else {
            Toast.makeText(context, "لا يوجد ملف DXF لتصديره حالياً", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KML to DXF Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        }
                        Text(
                            text = uiState.currentFileName.ifBlank { "جاهز للتحويل" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }
                },
                actions = {
                    // Samples Button
                    IconButton(
                        onClick = { showSamplesDialog = true },
                        modifier = Modifier.testTag("btn_samples")
                    ) {
                        Icon(imageVector = Icons.Default.FolderSpecial, contentDescription = "نماذج هندسية", tint = Color(0xFF38BDF8))
                    }
                    // DXF Code Inspector Button
                    IconButton(
                        onClick = { showDxfDialog = true },
                        enabled = uiState.generatedDxf != null,
                        modifier = Modifier.testTag("btn_view_dxf")
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = "كود DXF", tint = Color(0xFF10B981))
                    }
                    // History Button
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("btn_history")
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "السجل", tint = Color(0xFFCBD5E1))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color(0xFFF8FAFC)
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E293B),
                shadowElevation = 12.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Import Button
                        OutlinedButton(
                            onClick = {
                                openKmlLauncher.launch(arrayOf("*/*", "application/vnd.google-earth.kml+xml", "application/vnd.google-earth.kmz"))
                            },
                            modifier = Modifier.weight(1f).testTag("btn_open_kml"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فتح KML/KMZ", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }

                        // Save DXF Button
                        Button(
                            onClick = {
                                val defName = uiState.currentFileName.substringBeforeLast(".").ifBlank { "drawing" } + ".dxf"
                                saveDxfLauncher.launch(defName)
                            },
                            enabled = uiState.generatedDxf != null,
                            modifier = Modifier.weight(1.1f).testTag("btn_save_dxf"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ DXF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Share Button
                        IconButton(
                            onClick = { shareDxfFile() },
                            enabled = uiState.generatedDxf != null,
                            modifier = Modifier.testTag("btn_share_dxf")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFF34D399))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Tabs (CAD View, CRS Settings, Survey Report)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("معاينة CAD", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الإسقاط والدقة", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تقرير الإحداثيات", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            // Loading overlay indicator
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF38BDF8),
                    trackColor = Color(0xFF1E293B)
                )
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> {
                        // Interactive CAD View
                        val result = uiState.conversionResult
                        if (result != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CadCanvasView(
                                    drawing = result.drawing,
                                    cadUnit = uiState.settings.cadUnit,
                                    modifier = Modifier.fillMaxSize(),
                                    onEntitySelected = { ent -> viewModel.selectEntity(ent) }
                                )

                                // Selected Entity Bottom Detail Sheet / Card
                                uiState.selectedEntity?.let { ent ->
                                    EntityDetailCard(
                                        entity = ent,
                                        cadUnit = uiState.settings.cadUnit,
                                        onClose = { viewModel.selectEntity(null) },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 60.dp, start = 12.dp, end = 12.dp)
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("لا يوجد رسم مفتوح حالياً", color = Color(0xFF64748B))
                            }
                        }
                    }
                    1 -> {
                        // Coordinate Reference System Settings
                        val result = uiState.conversionResult
                        ProjectionSettingsSheet(
                            currentSettings = uiState.settings,
                            detectedUtmZone = result?.detectedUtmZone ?: 36,
                            detectedIsNorth = result?.detectedIsNorth ?: true,
                            onApplySettings = { newSettings -> viewModel.updateSettings(newSettings) }
                        )
                    }
                    2 -> {
                        // Survey Metrics & Bounding Box Report
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            EngineeringStatsCard(
                                result = uiState.conversionResult,
                                cadUnit = uiState.settings.cadUnit
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showSamplesDialog) {
        SampleSelectionDialog(
            onSelectSample = { sampleId -> viewModel.loadSample(sampleId) },
            onDismiss = { showSamplesDialog = false }
        )
    }

    if (showHistorySheet) {
        HistoryDrawerSheet(
            records = historyRecords,
            onDeleteRecord = { id -> viewModel.deleteHistoryRecord(id) },
            onClearAll = { viewModel.clearAllHistory() },
            onDismiss = { showHistorySheet = false }
        )
    }

    if (showDxfDialog) {
        DxfViewerDialog(
            dxfContent = uiState.generatedDxf,
            fileName = uiState.currentFileName,
            onDismiss = { showDxfDialog = false }
        )
    }
}
