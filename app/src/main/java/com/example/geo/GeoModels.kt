package com.example.geo

import kotlin.math.*

/**
 * High-precision WGS84 Geographic coordinate.
 * Lat/Lon in decimal degrees, altitude in meters above WGS84 ellipsoid.
 */
data class GeoPoint(
    val lon: Double,
    val lat: Double,
    val alt: Double = 0.0
)

/**
 * Projected Cartesian CAD coordinate (e.g. UTM Easting/Northing or Local Grid).
 */
data class CadPoint(
    val x: Double,
    val y: Double,
    val z: Double = 0.0
)

/**
 * 3D Bounding Box in CAD coordinate units.
 */
data class BoundingBox3D(
    val minX: Double,
    val minY: Double,
    val minZ: Double,
    val maxX: Double,
    val maxY: Double,
    val maxZ: Double
) {
    val deltaX: Double get() = maxX - minX
    val deltaY: Double get() = maxY - minY
    val deltaZ: Double get() = maxZ - minZ
    val centerX: Double get() = (minX + maxX) / 2.0
    val centerY: Double get() = (minY + maxY) / 2.0
    val centerZ: Double get() = (minZ + maxZ) / 2.0
}

/**
 * Supported CAD Units for DXF $INSUNITS.
 */
enum class CadUnit(val displayName: String, val arabicName: String, val scaleFromMeters: Double, val dxfInsUnits: Int) {
    METERS("Meters (m)", "أمتار (م)", 1.0, 4),
    MILLIMETERS("Millimeters (mm)", "ميليمتر (مم)", 1000.0, 6),
    CENTIMETERS("Centimeters (cm)", "سنتيمتر (سم)", 100.0, 5),
    US_SURVEY_FEET("US Survey Feet (ft)", "قدم مساحي أمريكي", 3.280833333333333, 2),
    INTERNATIONAL_FEET("International Feet (ft)", "قدم دولي", 3.280839895013123, 2),
    INCHES("Inches (in)", "بوصة", 39.37007874015748, 1)
}

/**
 * Supported Projection Systems.
 */
enum class ProjectionType(val displayName: String, val arabicName: String, val description: String) {
    UTM_AUTO("UTM (WGS84 Auto-Zone)", "UTM (كشف تلقائي للنطاق)", "Universal Transverse Mercator with sub-millimeter Krüger formula"),
    UTM_MANUAL("UTM (Manual Zone & Hem.)", "UTM (تحديد يدوي للنطاق)", "Specify custom UTM Zone (1-60) and North/South hemisphere"),
    LOCAL_TANGENT("Local Tangent / Site Grid", "نظام محلي (شبكة الموقع)", "Local Cartesian tangent plane centered at project benchmark (0,0) or (E0,N0)"),
    WEB_MERCATOR("Web Mercator (EPSG:3857)", "ويب ميركاتور (EPSG:3857)", "Spherical Mercator projected in meters"),
    GEODETIC_WGS84("WGS84 Geographic (Lon/Lat)", "إحداثيات جغرافية (درجات)", "Raw longitude and latitude degrees for GIS CAD imports")
}

/**
 * Layer structuring strategy in AutoCAD.
 */
enum class LayerScheme(val displayName: String, val arabicName: String) {
    BY_GEOMETRY("By Geometry (POINTS, LINES, POLYGONS)", "حسب نوع العنصر (نقاط، خطوط، مضلعات)"),
    BY_KML_FOLDER("By KML Folder / Feature Name", "حسب مجلد أو اسم العنصر في KML"),
    SINGLE_LAYER("Single Layer (0_SURVEY)", "طبقة واحدة موحدة (0_SURVEY)")
}
