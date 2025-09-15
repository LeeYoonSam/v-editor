package com.example.veditor.core.domain

import com.example.veditor.core.model.Timeline
import kotlinx.coroutines.delay

data class ExportParams(
    val timeline: Timeline,
)

sealed class ExportResult {
    data class Success(val outputUri: String) : ExportResult()
    data class Failure(val errorMessage: String) : ExportResult()
    data object Cancelled : ExportResult()
}

fun interface ExportUseCase {
    suspend operator fun invoke(params: ExportParams, onProgress: (Int) -> Unit): ExportResult
}

class FakeExportUseCase : ExportUseCase {
    override suspend fun invoke(params: ExportParams, onProgress: (Int) -> Unit): ExportResult {
        // Simulate work 0..100%
        for (p in 0..100 step 20) {
            onProgress(p)
            delay(50)
        }
        return ExportResult.Success(outputUri = "content://export/result.mp4")
    }
}


