package com.example

import com.example.dxf.DxfGenerator
import com.example.engine.ConversionProcessor
import com.example.geo.CoordinateTransformer
import com.example.geo.GeoPoint
import com.example.model.ConversionSettings
import com.example.parser.KmlParser
import com.example.parser.SampleKmlProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testUtmZoneDetection() {
        // Cairo: lon ~ 31.2 -> Zone 36
        assertEquals(36, CoordinateTransformer.getUtmZone(31.235))
        // Riyadh: lon ~ 46.7 -> Zone 38
        assertEquals(38, CoordinateTransformer.getUtmZone(46.713))
        // London: lon ~ -0.1 -> Zone 30
        assertEquals(30, CoordinateTransformer.getUtmZone(-0.127))
    }

    @Test
    fun testUtmProjectionHighPrecision() {
        val pt = GeoPoint(lon = 31.235, lat = 30.044, alt = 25.0)
        val (cadPt, utmInfo) = CoordinateTransformer.wgs84ToUtm(pt)

        assertEquals(36, utmInfo.zone)
        assertTrue(utmInfo.isNorth)
        assertTrue("Easting should be in reasonable range", cadPt.x in 300000.0..400000.0)
        assertTrue("Northing should be in reasonable range", cadPt.y in 3300000.0..3400000.0)
        assertEquals(25.0, cadPt.z, 0.001)
    }

    @Test
    fun testKmlParsingAndDxfGeneration() {
        val sample = SampleKmlProvider.samples.first()
        val doc = KmlParser.parse(ByteArrayInputStream(sample.kmlContent.toByteArray(Charsets.UTF_8)))

        assertNotNull(doc)
        assertTrue(doc.placemarks.isNotEmpty())
        assertTrue(doc.totalPoints > 0)
        assertTrue(doc.totalLines > 0)

        val result = ConversionProcessor.process(doc, ConversionSettings())
        assertNotNull(result)
        assertTrue(result.drawing.entities.isNotEmpty())

        val dxf = DxfGenerator.generateDxf(result.drawing)
        assertTrue(dxf.contains("AC1015"))
        assertTrue(dxf.contains("\$INSUNITS"))
        assertTrue(dxf.contains("ENTITIES"))
        assertTrue(dxf.contains("EOF"))
    }
}
