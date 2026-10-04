package com.example.parser

import com.example.geo.GeoPoint
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object KmlParser {

    /**
     * Parses either KML or KMZ input stream.
     */
    fun parse(inputStream: InputStream, isKmz: Boolean = false): ParsedKmlDocument {
        val kmlStream: InputStream = if (isKmz) {
            extractKmlFromKmz(inputStream) ?: throw IllegalArgumentException("No valid KML file found inside KMZ archive")
        } else {
            inputStream
        }

        return parseKmlStream(kmlStream)
    }

    /**
     * Extracts first .kml entry from KMZ stream.
     */
    fun extractKmlFromKmz(kmzStream: InputStream): InputStream? {
        val zip = ZipInputStream(kmzStream)
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name.endsWith(".kml", ignoreCase = true)) {
                val bytes = zip.readBytes()
                return ByteArrayInputStream(bytes)
            }
            entry = zip.nextEntry
        }
        return null
    }

    private fun parseKmlStream(stream: InputStream): ParsedKmlDocument {
        val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(stream, "UTF-8")

        var docTitle = "KML Survey Document"
        val placemarks = mutableListOf<KmlPlacemark>()
        val folderStack = mutableListOf<String>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                val tagName = parser.name
                when {
                    tagName.equals("Document", ignoreCase = true) -> {
                        // In document
                    }
                    tagName.equals("Folder", ignoreCase = true) -> {
                        // Will check folder name inside
                    }
                    tagName.equals("Placemark", ignoreCase = true) -> {
                        val currentFolder = if (folderStack.isNotEmpty()) folderStack.last() else "Default"
                        val pm = parsePlacemark(parser, currentFolder)
                        if (pm != null) {
                            placemarks.add(pm)
                        }
                    }
                    tagName.equals("name", ignoreCase = true) && folderStack.isEmpty() -> {
                        val title = parser.nextText()
                        if (title.isNotBlank()) {
                            docTitle = title.trim()
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        var totalPoints = 0
        var totalLines = 0
        var totalPolys = 0
        var sumLat = 0.0
        var sumLon = 0.0
        var coordCount = 0

        fun collectStats(geom: KmlGeometry) {
            when (geom) {
                is KmlPointGeometry -> {
                    totalPoints++
                    sumLat += geom.point.lat
                    sumLon += geom.point.lon
                    coordCount++
                }
                is KmlLineStringGeometry -> {
                    totalLines++
                    geom.points.forEach {
                        sumLat += it.lat
                        sumLon += it.lon
                        coordCount++
                    }
                }
                is KmlPolygonGeometry -> {
                    totalPolys++
                    geom.outerRing.forEach {
                        sumLat += it.lat
                        sumLon += it.lon
                        coordCount++
                    }
                }
                is KmlMultiGeometry -> {
                    geom.geometries.forEach { collectStats(it) }
                }
            }
        }

        placemarks.forEach { collectStats(it.geometry) }

        val centerPoint = if (coordCount > 0) {
            GeoPoint(lon = sumLon / coordCount, lat = sumLat / coordCount)
        } else null

        return ParsedKmlDocument(
            title = docTitle,
            placemarks = placemarks,
            totalPoints = totalPoints,
            totalLines = totalLines,
            totalPolygons = totalPolys,
            centerPoint = centerPoint
        )
    }

    private fun parsePlacemark(parser: XmlPullParser, folderName: String): KmlPlacemark? {
        var name = ""
        var description: String? = null
        val extendedData = mutableMapOf<String, String>()
        val geometries = mutableListOf<KmlGeometry>()

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name.equals("Placemark", ignoreCase = true))) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                when {
                    tag.equals("name", ignoreCase = true) -> {
                        name = parser.nextText().trim()
                    }
                    tag.equals("description", ignoreCase = true) -> {
                        description = parser.nextText().trim()
                    }
                    tag.equals("Point", ignoreCase = true) -> {
                        val pt = parsePoint(parser)
                        if (pt != null) geometries.add(pt)
                    }
                    tag.equals("LineString", ignoreCase = true) -> {
                        val ls = parseLineString(parser)
                        if (ls != null) geometries.add(ls)
                    }
                    tag.equals("Polygon", ignoreCase = true) -> {
                        val poly = parsePolygon(parser)
                        if (poly != null) geometries.add(poly)
                    }
                    tag.equals("MultiGeometry", ignoreCase = true) -> {
                        val multi = parseMultiGeometry(parser)
                        if (multi != null) geometries.add(multi)
                    }
                    tag.equals("Data", ignoreCase = true) || tag.equals("SimpleData", ignoreCase = true) -> {
                        val attrName = parser.getAttributeValue(null, "name") ?: "attr"
                        val value = parser.nextText().trim()
                        extendedData[attrName] = value
                    }
                }
            }
            eventType = parser.next()
        }

        if (geometries.isEmpty()) return null

        val finalGeometry = if (geometries.size == 1) {
            geometries.first()
        } else {
            KmlMultiGeometry(geometries)
        }

        return KmlPlacemark(
            name = if (name.isBlank()) "Placemark_${System.currentTimeMillis() % 1000}" else name,
            description = description,
            folderName = folderName,
            geometry = finalGeometry,
            extendedData = extendedData
        )
    }

    private fun parsePoint(parser: XmlPullParser): KmlPointGeometry? {
        var coords: GeoPoint? = null
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name.equals("Point", ignoreCase = true))) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("coordinates", ignoreCase = true)) {
                val text = parser.nextText()
                val list = parseCoordinatesText(text)
                if (list.isNotEmpty()) {
                    coords = list.first()
                }
            }
            eventType = parser.next()
        }
        return coords?.let { KmlPointGeometry(it) }
    }

    private fun parseLineString(parser: XmlPullParser): KmlLineStringGeometry? {
        val points = mutableListOf<GeoPoint>()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name.equals("LineString", ignoreCase = true))) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("coordinates", ignoreCase = true)) {
                val text = parser.nextText()
                points.addAll(parseCoordinatesText(text))
            }
            eventType = parser.next()
        }
        return if (points.isNotEmpty()) KmlLineStringGeometry(points) else null
    }

    private fun parsePolygon(parser: XmlPullParser): KmlPolygonGeometry? {
        val outer = mutableListOf<GeoPoint>()
        val inners = mutableListOf<List<GeoPoint>>()
        var inOuter = false
        var inInner = false

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name.equals("Polygon", ignoreCase = true))) {
            if (eventType == XmlPullParser.START_TAG) {
                val tag = parser.name
                when {
                    tag.equals("outerBoundaryIs", ignoreCase = true) -> inOuter = true
                    tag.equals("innerBoundaryIs", ignoreCase = true) -> inInner = true
                    tag.equals("coordinates", ignoreCase = true) -> {
                        val text = parser.nextText()
                        val pts = parseCoordinatesText(text)
                        if (inOuter) {
                            outer.addAll(pts)
                        } else if (inInner) {
                            inners.add(pts)
                        } else if (outer.isEmpty()) {
                            outer.addAll(pts)
                        }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                val tag = parser.name
                if (tag.equals("outerBoundaryIs", ignoreCase = true)) inOuter = false
                if (tag.equals("innerBoundaryIs", ignoreCase = true)) inInner = false
            }
            eventType = parser.next()
        }

        return if (outer.isNotEmpty()) KmlPolygonGeometry(outer, inners) else null
    }

    private fun parseMultiGeometry(parser: XmlPullParser): KmlMultiGeometry? {
        val list = mutableListOf<KmlGeometry>()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name.equals("MultiGeometry", ignoreCase = true))) {
            if (eventType == XmlPullParser.START_TAG) {
                when {
                    parser.name.equals("Point", ignoreCase = true) -> parsePoint(parser)?.let { list.add(it) }
                    parser.name.equals("LineString", ignoreCase = true) -> parseLineString(parser)?.let { list.add(it) }
                    parser.name.equals("Polygon", ignoreCase = true) -> parsePolygon(parser)?.let { list.add(it) }
                }
            }
            eventType = parser.next()
        }
        return if (list.isNotEmpty()) KmlMultiGeometry(list) else null
    }

    /**
     * Parses standard KML coordinates format:
     * lon,lat[,alt] whitespace lon,lat[,alt] ...
     */
    fun parseCoordinatesText(raw: String): List<GeoPoint> {
        val result = mutableListOf<GeoPoint>()
        // Normalize whitespace and newlines
        val tokens = raw.trim().split("\\s+".toRegex())
        for (token in tokens) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue
            val parts = trimmed.split(",")
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull() ?: continue
                val lat = parts[1].toDoubleOrNull() ?: continue
                val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() ?: 0.0 else 0.0
                result.add(GeoPoint(lon = lon, lat = lat, alt = alt))
            }
        }
        return result
    }
}
