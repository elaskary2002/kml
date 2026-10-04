package com.example.engine

import com.example.dxf.*
import com.example.geo.*
import com.example.model.ConversionSettings
import com.example.parser.*

object ConversionProcessor {

    data class ConversionResult(
        val drawing: CadDrawing,
        val detectedUtmZone: Int,
        val detectedIsNorth: Boolean,
        val centralMeridianDeg: Double,
        val gridScaleFactor: Double,
        val combinedScaleFactor: Double,
        val totalVertices: Int,
        val totalLengthMeters: Double,
        val totalAreaSquareMeters: Double
    )

    fun process(document: ParsedKmlDocument, settings: ConversionSettings): ConversionResult {
        val center = document.centerPoint ?: GeoPoint(30.0, 31.0, 0.0)
        val autoZone = CoordinateTransformer.getUtmZone(center.lon)
        val autoIsNorth = center.lat >= 0.0

        val effectiveZone = if (settings.projectionType == ProjectionType.UTM_MANUAL) settings.manualUtmZone else autoZone
        val effectiveIsNorth = if (settings.projectionType == ProjectionType.UTM_MANUAL) settings.manualIsNorth else autoIsNorth

        var centerUtmScale = 0.9996
        val (_, utmInfo) = CoordinateTransformer.wgs84ToUtm(center, effectiveZone, effectiveIsNorth)
        centerUtmScale = utmInfo.pointScaleFactor

        val combinedScale = CoordinateTransformer.computeCombinedScaleFactor(
            centerUtmScale,
            if (settings.applyGridToGround) settings.averageElevationMeters else 0.0
        )

        val unitFactor = settings.cadUnit.scaleFromMeters
        val groundMultiplier = if (settings.applyGridToGround && combinedScale > 0.0) 1.0 / combinedScale else 1.0

        fun transform(geo: GeoPoint): CadPoint {
            val baseCartesian = when (settings.projectionType) {
                ProjectionType.UTM_AUTO, ProjectionType.UTM_MANUAL -> {
                    CoordinateTransformer.wgs84ToUtm(geo, effectiveZone, effectiveIsNorth).first
                }
                ProjectionType.LOCAL_TANGENT -> {
                    CoordinateTransformer.wgs84ToLocalTangent(
                        point = geo,
                        origin = center,
                        localOffsetX = settings.localOriginX,
                        localOffsetY = settings.localOriginY
                    )
                }
                ProjectionType.WEB_MERCATOR -> {
                    CoordinateTransformer.wgs84ToWebMercator(geo)
                }
                ProjectionType.GEODETIC_WGS84 -> {
                    CadPoint(x = geo.lon, y = geo.lat, z = geo.alt)
                }
            }

            // Apply unit scaling and ground correction (if not raw geodetic)
            return if (settings.projectionType == ProjectionType.GEODETIC_WGS84) {
                baseCartesian
            } else {
                CadPoint(
                    x = baseCartesian.x * groundMultiplier * unitFactor,
                    y = baseCartesian.y * groundMultiplier * unitFactor,
                    z = if (settings.is3D) baseCartesian.z * unitFactor else 0.0
                )
            }
        }

        val entities = mutableListOf<CadEntity>()
        val layersMap = mutableMapOf<String, CadLayer>()

        fun registerLayer(name: String, defaultAci: Int): String {
            val key = name.trim().ifBlank { "0_SURVEY" }
            if (!layersMap.containsKey(key)) {
                layersMap[key] = CadLayer(name = key, aciColor = defaultAci)
            }
            return key
        }

        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        var totalVertices = 0
        var totalLength = 0.0
        var totalArea = 0.0

        fun updateBounds(p: CadPoint) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
            if (p.z < minZ) minZ = p.z
            if (p.z > maxZ) maxZ = p.z
            totalVertices++
        }

        for (pm in document.placemarks) {
            val baseLayerName = when (settings.layerScheme) {
                LayerScheme.BY_KML_FOLDER -> pm.folderName.ifBlank { "Folder_Default" }
                LayerScheme.SINGLE_LAYER -> "0_SURVEY"
                LayerScheme.BY_GEOMETRY -> "" // dynamically set per geometry
            }

            fun addGeometry(geom: KmlGeometry, depth: Int = 0) {
                when (geom) {
                    is KmlPointGeometry -> {
                        val pt = transform(geom.point)
                        updateBounds(pt)
                        val layer = if (settings.layerScheme == LayerScheme.BY_GEOMETRY) {
                            registerLayer("POINTS", 1) // Red
                        } else {
                            registerLayer(baseLayerName, 1)
                        }
                        entities.add(
                            CadPointEntity(
                                name = pm.name,
                                point = pt,
                                layer = layer,
                                colorAci = 1,
                                description = pm.description
                            )
                        )
                        if (settings.exportLabels) {
                            val labelLayer = if (settings.layerScheme == LayerScheme.BY_GEOMETRY) {
                                registerLayer("LABELS", 2) // Yellow
                            } else {
                                layer
                            }
                            val textPos = CadPoint(
                                x = pt.x + (settings.textHeight * 0.4),
                                y = pt.y + (settings.textHeight * 0.4),
                                z = pt.z
                            )
                            entities.add(
                                CadTextEntity(
                                    name = "LBL_${pm.name}",
                                    text = pm.name,
                                    position = textPos,
                                    textHeight = settings.textHeight,
                                    layer = labelLayer,
                                    colorAci = 2
                                )
                            )
                        }
                    }
                    is KmlLineStringGeometry -> {
                        val pts = geom.points.map {
                            val cp = transform(it)
                            updateBounds(cp)
                            cp
                        }
                        if (pts.size >= 2) {
                            val layer = if (settings.layerScheme == LayerScheme.BY_GEOMETRY) {
                                registerLayer("LINES", 4) // Cyan
                            } else {
                                registerLayer(baseLayerName, 4)
                            }
                            val polyline = CadPolylineEntity(
                                name = pm.name,
                                points = pts,
                                isClosed = false,
                                layer = layer,
                                colorAci = 4,
                                description = pm.description
                            )
                            entities.add(polyline)
                            totalLength += polyline.length

                            if (settings.exportLabels && pts.isNotEmpty()) {
                                val midIndex = pts.size / 2
                                val midPt = pts[midIndex]
                                val labelLayer = if (settings.layerScheme == LayerScheme.BY_GEOMETRY) {
                                    registerLayer("LABELS", 2)
                                } else layer
                                entities.add(
                                    CadTextEntity(
                                        name = "LBL_${pm.name}",
                                        text = pm.name,
                                        position = CadPoint(midPt.x, midPt.y + settings.textHeight, midPt.z),
                                        textHeight = settings.textHeight,
                                        layer = labelLayer,
                                        colorAci = 2
                                    )
                                )
                            }
                        }
                    }
                    is KmlPolygonGeometry -> {
                        val outerPts = geom.outerRing.map {
                            val cp = transform(it)
                            updateBounds(cp)
                            cp
                        }
                        if (outerPts.size >= 3) {
                            val layer = if (settings.layerScheme == LayerScheme.BY_GEOMETRY) {
                                registerLayer("POLYGONS", 3) // Green
                            } else {
                                registerLayer(baseLayerName, 3)
                            }
                            val poly = CadPolylineEntity(
                                name = pm.name,
                                points = outerPts,
                                isClosed = true,
                                layer = layer,
                                colorAci = 3,
                                description = pm.description
                            )
                            entities.add(poly)
                            totalArea += poly.area
                            totalLength += poly.length

                            if (settings.exportLabels && outerPts.isNotEmpty()) {
                                var cx = 0.0
                                var cy = 0.0
                                for (p in outerPts) {
                                    cx += p.x
                                    cy += p.y
                                }
                                val centroid = CadPoint(cx / outerPts.size, cy / outerPts.size, outerPts.first().z)
                                val labelLayer = if (settings.layerScheme == LayerScheme.BY_GEOMETRY) {
                                    registerLayer("LABELS", 2)
                                } else layer
                                entities.add(
                                    CadTextEntity(
                                        name = "LBL_${pm.name}",
                                        text = pm.name,
                                        position = centroid,
                                        textHeight = settings.textHeight,
                                        layer = labelLayer,
                                        colorAci = 2
                                    )
                                )
                            }
                        }
                    }
                    is KmlMultiGeometry -> {
                        for (subGeom in geom.geometries) {
                            addGeometry(subGeom, depth + 1)
                        }
                    }
                }
            }

            addGeometry(pm.geometry)
        }

        val boundingBox = if (minX.isFinite()) {
            BoundingBox3D(minX, minY, minZ, maxX, maxY, maxZ)
        } else {
            BoundingBox3D(0.0, 0.0, 0.0, 100.0, 100.0, 0.0)
        }

        val crsDescription = when (settings.projectionType) {
            ProjectionType.UTM_AUTO, ProjectionType.UTM_MANUAL ->
                "UTM Zone ${effectiveZone}${if (effectiveIsNorth) "N" else "S"} (WGS84) - EPSG:${if (effectiveIsNorth) 32600 + effectiveZone else 32700 + effectiveZone}"
            ProjectionType.LOCAL_TANGENT ->
                "Local Tangent Plane Grid (Center: ${String.format(java.util.Locale.US, "%.5f, %.5f", center.lat, center.lon)})"
            ProjectionType.WEB_MERCATOR ->
                "Web Mercator EPSG:3857 (Meters)"
            ProjectionType.GEODETIC_WGS84 ->
                "WGS84 Geographic Lat/Lon (Decimal Degrees)"
        }

        val drawing = CadDrawing(
            title = document.title,
            layers = layersMap.values.toList(),
            entities = entities,
            boundingBox = boundingBox,
            utmZoneText = "UTM ${effectiveZone}${if (effectiveIsNorth) "N" else "S"}",
            crsDescription = crsDescription
        )

        return ConversionResult(
            drawing = drawing,
            detectedUtmZone = effectiveZone,
            detectedIsNorth = effectiveIsNorth,
            centralMeridianDeg = CoordinateTransformer.getCentralMeridian(effectiveZone),
            gridScaleFactor = centerUtmScale,
            combinedScaleFactor = combinedScale,
            totalVertices = totalVertices,
            totalLengthMeters = totalLength / unitFactor,
            totalAreaSquareMeters = totalArea / (unitFactor * unitFactor)
        )
    }
}
