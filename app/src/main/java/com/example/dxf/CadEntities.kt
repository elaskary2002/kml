package com.example.dxf

import com.example.geo.BoundingBox3D
import com.example.geo.CadPoint

data class CadLayer(
    val name: String,
    val aciColor: Int = 7, // AutoCAD Color Index (1=Red, 2=Yellow, 3=Green, 4=Cyan, 5=Blue, 6=Magenta, 7=White/Black)
    val lineType: String = "CONTINUOUS"
)

sealed interface CadEntity {
    val name: String
    val layer: String
    val colorAci: Int
    val description: String?
}

data class CadPointEntity(
    override val name: String,
    val point: CadPoint,
    override val layer: String = "POINTS",
    override val colorAci: Int = 1, // Red
    override val description: String? = null,
    val pointCode: String? = null
) : CadEntity

data class CadPolylineEntity(
    override val name: String,
    val points: List<CadPoint>,
    val isClosed: Boolean,
    override val layer: String = "LINES",
    override val colorAci: Int = 4, // Cyan
    override val description: String? = null
) : CadEntity {
    val length: Double get() {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val dz = p2.z - p1.z
            total += kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        }
        if (isClosed && points.size > 2) {
            val p1 = points.last()
            val p2 = points.first()
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val dz = p2.z - p1.z
            total += kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        }
        return total
    }

    /**
     * 2D polygon area using Shoelace formula.
     */
    val area: Double get() {
        if (!isClosed || points.size < 3) return 0.0
        var sum = 0.0
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            sum += (p1.x * p2.y) - (p2.x * p1.y)
        }
        return kotlin.math.abs(sum) / 2.0
    }
}

data class CadTextEntity(
    override val name: String,
    val text: String,
    val position: CadPoint,
    val textHeight: Double,
    override val layer: String = "LABELS",
    override val colorAci: Int = 2, // Yellow
    val rotationDeg: Double = 0.0,
    override val description: String? = null
) : CadEntity

data class CadDrawing(
    val title: String,
    val layers: List<CadLayer>,
    val entities: List<CadEntity>,
    val boundingBox: BoundingBox3D,
    val utmZoneText: String,
    val crsDescription: String
)
