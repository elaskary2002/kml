package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class ConversionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val projectionName: String,
    val cadUnit: String,
    val entityCountsSummary: String,
    val boundingBoxSummary: String,
    val dxfSnippet: String,
    val totalPoints: Int,
    val totalLines: Int,
    val totalPolygons: Int
)
