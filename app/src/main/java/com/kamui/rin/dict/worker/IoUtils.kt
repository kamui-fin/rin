package com.kamui.rin.dict.worker

import java.io.InputStream
import java.util.zip.ZipInputStream

fun mapFilenameToBytes(input: InputStream): Map<String, ByteArray> {
    return ZipInputStream(input).use { stream ->
        generateSequence { stream.nextEntry }
            .filterNot { it.isDirectory }
            .map { entry ->
                val pair = Pair<String, ByteArray>(entry.name, stream.readBytes())
                pair
            }.toMap()
    }
}
