package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ConversionRecord
import com.example.data.ConversionRepository
import com.example.dxf.CadDrawing
import com.example.dxf.CadEntity
import com.example.dxf.DxfGenerator
import com.example.engine.ConversionProcessor
import com.example.geo.BoundingBox3D
import com.example.geo.CadUnit
import com.example.geo.ProjectionType
import com.example.model.ConversionSettings
import com.example.parser.KmlParser
import com.example.parser.ParsedKmlDocument
import com.example.parser.SampleKmlProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val currentFileName: String = "",
    val parsedDocument: ParsedKmlDocument? = null,
    val settings: ConversionSettings = ConversionSettings(),
    val conversionResult: ConversionProcessor.ConversionResult? = null,
    val generatedDxf: String? = null,
    val selectedEntity: CadEntity? = null,
    val errorMessage: String? = null,
    val successNotification: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConversionRepository(AppDatabase.getDatabase(application).conversionDao())

    val historyRecords: StateFlow<List<ConversionRecord>> = repository.history
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Load default highway alignment sample so the app opens with rich, instant CAD visualization
        loadSample("highway_alignment")
    }

    fun loadSample(sampleId: String) {
        val sample = SampleKmlProvider.samples.find { it.id == sampleId } ?: SampleKmlProvider.samples.first()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "تحميل وتجهيز النموذج الهندسي..."
            )
            try {
                withContext(Dispatchers.IO) {
                    val stream = ByteArrayInputStream(sample.kmlContent.toByteArray(Charsets.UTF_8))
                    val doc = KmlParser.parse(stream, isKmz = false)
                    applyParsedDocument(doc, "${sample.id}.kml")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "خطأ في قراءة ملف النموذج: ${e.localizedMessage}"
                )
            }
        }
    }

    fun loadKmlFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "قراءة وفحص ملف KML/KMZ..."
            )
            try {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val isKmz = fileName.endsWith(".kmz", ignoreCase = true)
                    val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("تعذر فتح مسار الملف")

                    val doc = inputStream.use { KmlParser.parse(it, isKmz = isKmz) }
                    applyParsedDocument(doc, fileName)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "فشل في قراءة الملف: ${e.localizedMessage}"
                )
            }
        }
    }

    private suspend fun applyParsedDocument(doc: ParsedKmlDocument, fileName: String) {
        // Auto-configure recommended UTM zone for this coordinate cluster
        val center = doc.centerPoint
        val autoZone = center?.let { com.example.geo.CoordinateTransformer.getUtmZone(it.lon) } ?: 36
        val autoIsNorth = center?.let { it.lat >= 0.0 } ?: true

        val initialSettings = _uiState.value.settings.copy(
            manualUtmZone = autoZone,
            manualIsNorth = autoIsNorth
        )

        val result = ConversionProcessor.process(doc, initialSettings)
        val dxf = DxfGenerator.generateDxf(
            drawing = result.drawing,
            unit = initialSettings.cadUnit,
            precision = initialSettings.precisionDecimals,
            is3D = initialSettings.is3D,
            markerSize = initialSettings.textHeight
        )

        // Save in Room DB history
        saveToHistory(fileName, initialSettings, result, dxf, doc)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            currentFileName = fileName,
            parsedDocument = doc,
            settings = initialSettings,
            conversionResult = result,
            generatedDxf = dxf,
            selectedEntity = null,
            errorMessage = null,
            successNotification = "تم تحويل الملف بنجاح (${result.drawing.entities.size} عنصر كاد)"
        )
    }

    fun updateSettings(newSettings: ConversionSettings) {
        val currentDoc = _uiState.value.parsedDocument ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, loadingMessage = "تحديث الإحداثيات وإعادة التحويل...")
            withContext(Dispatchers.Default) {
                val result = ConversionProcessor.process(currentDoc, newSettings)
                val dxf = DxfGenerator.generateDxf(
                    drawing = result.drawing,
                    unit = newSettings.cadUnit,
                    precision = newSettings.precisionDecimals,
                    is3D = newSettings.is3D,
                    markerSize = newSettings.textHeight
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settings = newSettings,
                    conversionResult = result,
                    generatedDxf = dxf,
                    successNotification = "تم تحديث إحداثيات الرسم (${result.drawing.crsDescription})"
                )
            }
        }
    }

    fun selectEntity(entity: CadEntity?) {
        _uiState.value = _uiState.value.copy(selectedEntity = entity)
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(successNotification = null, errorMessage = null)
    }

    /**
     * Saves DXF to user-selected destination URI via SAF.
     */
    fun saveDxfToUri(targetUri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val dxf = _uiState.value.generatedDxf ?: return onError("لا يوجد محتوى DXF للحفظ")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openOutputStream(targetUri)?.use { out ->
                    out.write(dxf.toByteArray(Charsets.US_ASCII))
                    out.flush()
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        successNotification = "تم حفظ ملف DXF بنجاح"
                    )
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("خطأ أثناء حفظ الملف: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Generates a temporary DXF file in the app cache for direct Share Intent.
     */
    fun getShareableDxfFile(): File? {
        val dxf = _uiState.value.generatedDxf ?: return null
        val context = getApplication<Application>()
        val dir = File(context.cacheDir, "dxf")
        if (!dir.exists()) dir.mkdirs()

        val rawName = _uiState.value.currentFileName.substringBeforeLast(".")
        val safeName = rawName.ifBlank { "drawing" }.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(dir, "${safeName}_cad.dxf")
        file.writeText(dxf, Charsets.US_ASCII)
        return file
    }

    fun deleteHistoryRecord(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private suspend fun saveToHistory(
        fileName: String,
        settings: ConversionSettings,
        result: ConversionProcessor.ConversionResult,
        dxf: String,
        doc: ParsedKmlDocument
    ) {
        try {
            val snippet = dxf.take(1500)
            val counts = "P:${doc.totalPoints} | L:${doc.totalLines} | Poly:${doc.totalPolygons}"
            val bb = result.drawing.boundingBox
            val bbSummary = "X:[${String.format(Locale.US, "%.1f", bb.minX)}..${String.format(Locale.US, "%.1f", bb.maxX)}]"

            repository.recordConversion(
                ConversionRecord(
                    fileName = fileName,
                    timestamp = System.currentTimeMillis(),
                    projectionName = result.drawing.utmZoneText,
                    cadUnit = settings.cadUnit.displayName,
                    entityCountsSummary = counts,
                    boundingBoxSummary = bbSummary,
                    dxfSnippet = snippet,
                    totalPoints = doc.totalPoints,
                    totalLines = doc.totalLines,
                    totalPolygons = doc.totalPolygons
                )
            )
        } catch (_: Exception) {}
    }
}
