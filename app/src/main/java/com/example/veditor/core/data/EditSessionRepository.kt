package com.example.veditor.core.data

import com.example.veditor.core.model.Timeline
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.IOException

interface EditSessionRepository {
    @Throws(IOException::class)
    fun save(timeline: Timeline, file: File)

    @Throws(IOException::class)
    fun load(file: File): Timeline
}

class FileEditSessionRepository(
    private val json: Json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    },
) : EditSessionRepository {

    @OptIn(ExperimentalSerializationApi::class)
    override fun save(timeline: Timeline, file: File) {
        file.parentFile?.mkdirs()
        file.outputStream().use { os ->
            json.encodeToStream(timeline, os)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun load(file: File): Timeline {
        file.inputStream().use { ins ->
            return json.decodeFromStream(ins)
        }
    }
}


