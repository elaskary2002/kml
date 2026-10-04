package com.example.data

import kotlinx.coroutines.flow.Flow

class ConversionRepository(private val dao: ConversionDao) {
    val history: Flow<List<ConversionRecord>> = dao.getAllRecords()

    suspend fun recordConversion(record: ConversionRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
