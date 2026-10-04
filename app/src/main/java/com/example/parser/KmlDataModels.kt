package com.example.parser

import com.example.geo.GeoPoint

sealed interface KmlGeometry

data class KmlPointGeometry(
    val point: GeoPoint
) : KmlGeometry

data class KmlLineStringGeometry(
    val points: List<GeoPoint>
) : KmlGeometry

data class KmlPolygonGeometry(
    val outerRing: List<GeoPoint>,
    val innerRings: List<List<GeoPoint>> = emptyList()
) : KmlGeometry

data class KmlMultiGeometry(
    val geometries: List<KmlGeometry>
) : KmlGeometry

data class KmlPlacemark(
    val name: String,
    val description: String? = null,
    val folderName: String = "Default",
    val styleColor: String? = null,
    val geometry: KmlGeometry,
    val extendedData: Map<String, String> = emptyMap()
)

data class ParsedKmlDocument(
    val title: String,
    val placemarks: List<KmlPlacemark>,
    val totalPoints: Int,
    val totalLines: Int,
    val totalPolygons: Int,
    val centerPoint: GeoPoint?
)
