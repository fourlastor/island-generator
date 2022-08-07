package io.github.fourlastor.ldtk

import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
class LDtkReader {

  private val json = Json { ignoreUnknownKeys = true }

  fun data(stream: InputStream): LDtkMapData {
    return json.decodeFromStream(stream)
  }
}
