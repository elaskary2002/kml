package com.example.geo

import kotlin.math.*

/**
 * High-precision Geodetic and Map Projection Engine.
 * Supports:
 * - WGS84 Ellipsoid
 * - Universal Transverse Mercator (UTM) with sub-millimeter Krüger / Redfearn expansion
 * - Local Tangent Plane (Local Cartesian project site grid)
 * - Web Mercator (EPSG:3857)
 * - Scale factor calculation (Grid-to-Ground elevation correction)
 */
object CoordinateTransformer {

    // WGS84 Ellipsoid constants
    const val A = 6378137.0 // Semi-major axis in meters
    const val F = 1.0 / 298.257223563 // Flattening
    const val B = A * (1.0 - F) // Semi-minor axis: 6356752.314245 m
    const val E2 = 2.0 * F - F * F // First eccentricity squared: 0.00669437999014
    const val E_PRIME2 = E2 / (1.0 - E2) // Second eccentricity squared
    const val UTM_K0 = 0.9996 // UTM central meridian scale factor
    const val FALSE_EASTING = 500000.0 // False Easting meters
    const val FALSE_NORTHING_SOUTH = 10000000.0 // False Northing meters for southern hemisphere

    /**
     * Determines optimal UTM zone (1 to 60) for a given longitude in degrees.
     */
    fun getUtmZone(lon: Double): Int {
        var normalizedLon = lon
        while (normalizedLon < -180.0) normalizedLon += 360.0
        while (normalizedLon >= 180.0) normalizedLon -= 360.0
        return floor((normalizedLon + 180.0) / 6.0).toInt() + 1
    }

    /**
     * Returns Central Meridian for a given UTM zone in decimal degrees.
     */
    fun getCentralMeridian(zone: Int): Double {
        return (zone - 1) * 6.0 - 180.0 + 3.0
    }

    /**
     * Converts WGS84 geodetic point (lat, lon, alt) to UTM (Easting, Northing, Elevation)
     * using high-precision Redfearn / Krüger series.
     *
     * @param point Geographic coordinate
     * @param overrideZone Optional manual UTM zone (1..60). If null, auto-detected.
     * @param overrideIsNorth Optional manual hemisphere. If null, deduced from latitude >= 0.
     */
    fun wgs84ToUtm(
        point: GeoPoint,
        overrideZone: Int? = null,
        overrideIsNorth: Boolean? = null
    ): Pair<CadPoint, UtmInfo> {
        val latRad = Math.toRadians(point.lat)
        val lonRad = Math.toRadians(point.lon)

        val zone = overrideZone ?: getUtmZone(point.lon)
        val isNorth = overrideIsNorth ?: (point.lat >= 0.0)
        val lon0Rad = Math.toRadians(getCentralMeridian(zone))

        val deltaLon = lonRad - lon0Rad

        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val tanLat = tan(latRad)
        val cosLat2 = cosLat * cosLat
        val cosLat3 = cosLat2 * cosLat
        val cosLat4 = cosLat2 * cosLat2
        val cosLat5 = cosLat4 * cosLat
        val cosLat6 = cosLat3 * cosLat3

        val tanLat2 = tanLat * tanLat
        val tanLat4 = tanLat2 * tanLat2

        val eta2 = E_PRIME2 * cosLat2

        // Radius of curvature in the prime vertical
        val nu = A / sqrt(1.0 - E2 * sinLat * sinLat)

        // Meridian distance M from equator to latitude
        val m = meridianDistance(latRad)

        // Krüger / Redfearn expansion for Easting
        val p = deltaLon
        val p2 = p * p
        val p3 = p2 * p
        val p4 = p2 * p2
        val p5 = p4 * p
        val p6 = p3 * p3

        val eastingTerm1 = p * cosLat
        val eastingTerm2 = (p3 * cosLat3 / 6.0) * (1.0 - tanLat2 + eta2)
        val eastingTerm3 = (p5 * cosLat5 / 120.0) * (5.0 - 18.0 * tanLat2 + tanLat4 + 14.0 * eta2 - 58.0 * tanLat2 * eta2)

        val easting = FALSE_EASTING + UTM_K0 * nu * (eastingTerm1 + eastingTerm2 + eastingTerm3)

        // Krüger / Redfearn expansion for Northing
        val northingTerm1 = (p2 * cosLat2 / 2.0) * tanLat
        val northingTerm2 = (p4 * cosLat4 / 24.0) * tanLat * (5.0 - tanLat2 + 9.0 * eta2 + 4.0 * eta2 * eta2)
        val northingTerm3 = (p6 * cosLat6 / 720.0) * tanLat * (61.0 - 58.0 * tanLat2 + tanLat4 + 270.0 * eta2 - 330.0 * tanLat2 * eta2)

        var northing = UTM_K0 * (m + nu * (northingTerm1 + northingTerm2 + northingTerm3))
        if (!isNorth) {
            northing += FALSE_NORTHING_SOUTH
        }

        // Point scale factor k
        val scaleFactor = UTM_K0 * (1.0 + (p2 * cosLat2 / 2.0) * (1.0 + eta2) + (p4 * cosLat4 / 24.0) * (5.0 - 4.0 * tanLat2))

        // Grid convergence angle (gamma) in degrees
        val convergence = Math.toDegrees(p * sinLat + (p3 * sinLat * cosLat2 / 3.0) * (1.0 + 3.0 * eta2 + 2.0 * eta2 * eta2))

        val utmInfo = UtmInfo(
            zone = zone,
            isNorth = isNorth,
            centralMeridianDeg = getCentralMeridian(zone),
            pointScaleFactor = scaleFactor,
            convergenceDeg = convergence
        )

        return Pair(CadPoint(x = easting, y = northing, z = point.alt), utmInfo)
    }

    /**
     * Meridian distance from equator to latitude phi along the WGS84 ellipsoid.
     */
    private fun meridianDistance(phi: Double): Double {
        val n = (A - B) / (A + B)
        val n2 = n * n
        val n3 = n2 * n
        val n4 = n2 * n2
        val n5 = n4 * n

        val a0 = 1.0 + n2 / 4.0 + n4 / 64.0
        val a2 = (3.0 / 2.0) * (n - n3 / 8.0 - n5 / 64.0)
        val a4 = (15.0 / 16.0) * (n2 - n4 / 4.0)
        val a6 = (35.0 / 48.0) * (n3 - (5.0 / 16.0) * n5)
        val a8 = (315.0 / 512.0) * n4

        val alpha = (A / (1.0 + n)) * a0
        return alpha * (phi - a2 * sin(2.0 * phi) + a4 * sin(4.0 * phi) - a6 * sin(6.0 * phi) + a8 * sin(8.0 * phi))
    }

    /**
     * Converts to Local Tangent Plane (Local Cartesian Site Grid) centered at origin.
     * Perfect for architectural / construction sites to prevent coordinate jitter.
     */
    fun wgs84ToLocalTangent(
        point: GeoPoint,
        origin: GeoPoint,
        localOffsetX: Double = 0.0,
        localOffsetY: Double = 0.0
    ): CadPoint {
        val lat0 = Math.toRadians(origin.lat)
        val lon0 = Math.toRadians(origin.lon)
        val lat = Math.toRadians(point.lat)
        val lon = Math.toRadians(point.lon)

        // Radius of curvature in the prime vertical & meridian at origin
        val sinLat0 = sin(lat0)
        val nu0 = A / sqrt(1.0 - E2 * sinLat0 * sinLat0)
        val rho0 = (A * (1.0 - E2)) / (1.0 - E2 * sinLat0 * sinLat0).pow(1.5)

        val dLon = lon - lon0
        val dLat = lat - lat0

        val easting = (nu0 * cos(lat0) * dLon) + localOffsetX
        val northing = (rho0 * dLat) + localOffsetY
        val dz = point.alt - origin.alt

        return CadPoint(x = easting, y = northing, z = dz)
    }

    /**
     * Converts to Web Mercator (EPSG:3857).
     */
    fun wgs84ToWebMercator(point: GeoPoint): CadPoint {
        val x = A * Math.toRadians(point.lon)
        val latClamped = point.lat.coerceIn(-85.05112878, 85.05112878)
        val latRad = Math.toRadians(latClamped)
        val y = A * ln(tan(Math.PI / 4.0 + latRad / 2.0))
        return CadPoint(x = x, y = y, z = point.alt)
    }

    /**
     * Combined scale factor (Grid-to-Ground) for survey engineering:
     * Combined Factor = Elevation Factor (Kh) * Point Grid Scale Factor (k)
     */
    fun computeCombinedScaleFactor(pointScaleFactor: Double, elevationMeters: Double): Double {
        val meanRadius = sqrt(A * B) // ~6,367,449 m
        val elevationFactor = meanRadius / (meanRadius + elevationMeters.coerceAtLeast(0.0))
        return pointScaleFactor * elevationFactor
    }
}

/**
 * Metadata about the UTM projection used.
 */
data class UtmInfo(
    val zone: Int,
    val isNorth: Boolean,
    val centralMeridianDeg: Double,
    val pointScaleFactor: Double,
    val convergenceDeg: Double
) {
    val epsgCode: Int get() = if (isNorth) 32600 + zone else 32700 + zone
    val formattedZone: String get() = "${zone}${if (isNorth) "N" else "S"}"
}
