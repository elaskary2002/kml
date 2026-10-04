package com.example.model

import com.example.geo.CadUnit
import com.example.geo.LayerScheme
import com.example.geo.ProjectionType

data class ConversionSettings(
    val projectionType: ProjectionType = ProjectionType.UTM_AUTO,
    val manualUtmZone: Int = 36,
    val manualIsNorth: Boolean = true,
    val cadUnit: CadUnit = CadUnit.METERS,
    val precisionDecimals: Int = 6,
    val is3D: Boolean = true,
    val layerScheme: LayerScheme = LayerScheme.BY_GEOMETRY,
    val exportLabels: Boolean = true,
    val textHeight: Double = 2.5,
    val applyGridToGround: Boolean = false,
    val averageElevationMeters: Double = 0.0,
    val localOriginX: Double = 1000.0,
    val localOriginY: Double = 1000.0
)
