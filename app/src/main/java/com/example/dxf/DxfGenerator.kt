package com.example.dxf

import com.example.geo.CadUnit
import java.util.Locale

object DxfGenerator {

    /**
     * Generates standard AutoCAD DXF AC1015 (AutoCAD 2000+) ASCII content.
     * Compatible with all versions of AutoCAD, Civil 3D, QCAD, LibreCAD, Revit.
     */
    fun generateDxf(
        drawing: CadDrawing,
        unit: CadUnit = CadUnit.METERS,
        precision: Int = 6,
        is3D: Boolean = true,
        markerSize: Double = 2.0
    ): String {
        val sb = StringBuilder(64 * 1024)

        fun fmt(d: Double): String = String.format(Locale.US, "%.${precision}f", d)

        val bb = drawing.boundingBox
        val minX = if (bb.minX.isFinite()) bb.minX else 0.0
        val minY = if (bb.minY.isFinite()) bb.minY else 0.0
        val minZ = if (bb.minZ.isFinite()) bb.minZ else 0.0
        val maxX = if (bb.maxX.isFinite()) bb.maxX else 100.0
        val maxY = if (bb.maxY.isFinite()) bb.maxY else 100.0
        val maxZ = if (bb.maxZ.isFinite()) bb.maxZ else 0.0

        // SECTION: HEADER
        sb.append("0\nSECTION\n2\nHEADER\n")
        sb.append("9\n\$ACADVER\n1\nAC1015\n")
        sb.append("9\n\$INSUNITS\n70\n${unit.dxfInsUnits}\n")
        sb.append("9\n\$MEASUREMENT\n70\n1\n") // 1 = Metric, 0 = Imperial
        sb.append("9\n\$LUNITS\n70\n2\n") // 2 = Decimal
        sb.append("9\n\$LUPREC\n70\n$precision\n")
        sb.append("9\n\$AUNITS\n70\n0\n") // Degrees
        sb.append("9\n\$ANGBASE\n50\n0.0\n")
        sb.append("9\n\$ANGDIR\n70\n0\n")
        sb.append("9\n\$EXTMIN\n10\n${fmt(minX)}\n20\n${fmt(minY)}\n30\n${fmt(minZ)}\n")
        sb.append("9\n\$EXTMAX\n10\n${fmt(maxX)}\n20\n${fmt(maxY)}\n30\n${fmt(maxZ)}\n")
        sb.append("9\n\$PDMODE\n70\n34\n") // Survey circle with cross mark
        sb.append("9\n\$PDSIZE\n40\n${fmt(markerSize)}\n")
        sb.append("0\nENDSEC\n")

        // SECTION: TABLES
        sb.append("0\nSECTION\n2\nTABLES\n")

        // LTYPE table
        sb.append("0\nTABLE\n2\nLTYPE\n70\n1\n")
        sb.append("0\nLTYPE\n2\nCONTINUOUS\n70\n0\n3\nSolid line\n72\n65\n73\n0\n40\n0.0\n")
        sb.append("0\nENDTAB\n")

        // LAYER table
        val layers = drawing.layers.ifEmpty {
            listOf(
                CadLayer("POINTS", aciColor = 1),
                CadLayer("LINES", aciColor = 4),
                CadLayer("POLYGONS", aciColor = 3),
                CadLayer("LABELS", aciColor = 2)
            )
        }

        sb.append("0\nTABLE\n2\nLAYER\n70\n${layers.size}\n")
        for (layer in layers) {
            val safeName = layer.name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            sb.append("0\nLAYER\n2\n$safeName\n70\n0\n62\n${layer.aciColor}\n6\n${layer.lineType}\n")
        }
        sb.append("0\nENDTAB\n")

        // STYLE table
        sb.append("0\nTABLE\n2\nSTYLE\n70\n1\n")
        sb.append("0\nSTYLE\n2\nSTANDARD\n70\n0\n40\n0.0\n41\n1.0\n50\n0.0\n71\n0\n42\n2.5\n3\ntxt\n4\n\n")
        sb.append("0\nENDTAB\n")

        sb.append("0\nENDSEC\n")

        // SECTION: BLOCKS
        sb.append("0\nSECTION\n2\nBLOCKS\n0\nENDSEC\n")

        // SECTION: ENTITIES
        sb.append("0\nSECTION\n2\nENTITIES\n")

        for (entity in drawing.entities) {
            val layerName = entity.layer.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            when (entity) {
                is CadPointEntity -> {
                    val p = entity.point
                    sb.append("0\nPOINT\n")
                    sb.append("8\n$layerName\n")
                    sb.append("62\n${entity.colorAci}\n")
                    sb.append("10\n${fmt(p.x)}\n")
                    sb.append("20\n${fmt(p.y)}\n")
                    sb.append("30\n${fmt(if (is3D) p.z else 0.0)}\n")
                }
                is CadPolylineEntity -> {
                    if (entity.points.size >= 2) {
                        if (is3D) {
                            // 3D Polyline
                            val polyFlag = if (entity.isClosed) 9 else 8
                            sb.append("0\nPOLYLINE\n")
                            sb.append("8\n$layerName\n")
                            sb.append("62\n${entity.colorAci}\n")
                            sb.append("66\n1\n") // Vertices follow flag
                            sb.append("70\n$polyFlag\n") // 8 = 3D Polyline, 9 = 3D Polyline Closed
                            sb.append("10\n0.0\n20\n0.0\n30\n0.0\n")

                            for (pt in entity.points) {
                                sb.append("0\nVERTEX\n")
                                sb.append("8\n$layerName\n")
                                sb.append("10\n${fmt(pt.x)}\n")
                                sb.append("20\n${fmt(pt.y)}\n")
                                sb.append("30\n${fmt(pt.z)}\n")
                                sb.append("70\n32\n") // 3D polyline vertex flag
                            }
                            sb.append("0\nSEQEND\n8\n$layerName\n")
                        } else {
                            // 2D Lightweight Polyline (LWPOLYLINE)
                            val closedVal = if (entity.isClosed) 1 else 0
                            sb.append("0\nLWPOLYLINE\n")
                            sb.append("8\n$layerName\n")
                            sb.append("62\n${entity.colorAci}\n")
                            sb.append("90\n${entity.points.size}\n")
                            sb.append("70\n$closedVal\n")
                            sb.append("38\n${fmt(entity.points.first().z)}\n") // Elevation
                            for (pt in entity.points) {
                                sb.append("10\n${fmt(pt.x)}\n")
                                sb.append("20\n${fmt(pt.y)}\n")
                            }
                        }
                    }
                }
                is CadTextEntity -> {
                    val p = entity.position
                    val sanitizedText = entity.text.replace("\n", " ").trim()
                    sb.append("0\nTEXT\n")
                    sb.append("8\n$layerName\n")
                    sb.append("62\n${entity.colorAci}\n")
                    sb.append("10\n${fmt(p.x)}\n")
                    sb.append("20\n${fmt(p.y)}\n")
                    sb.append("30\n${fmt(if (is3D) p.z else 0.0)}\n")
                    sb.append("40\n${fmt(entity.textHeight)}\n")
                    sb.append("1\n$sanitizedText\n")
                    sb.append("50\n${fmt(entity.rotationDeg)}\n")
                    sb.append("7\nSTANDARD\n")
                }
            }
        }

        sb.append("0\nENDSEC\n")
        sb.append("0\nEOF\n")

        return sb.toString()
    }
}
