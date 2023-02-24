package com.kamui.rin.util

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Serializable
@Parcelize
data class Tag(
    val name: String,
    val category: String,
    val order: Int,
    val description: String,
    val score: Int,
    val color: String
): Parcelable

class Tags(context: Context) {
    private var tags: Map<String, Tag>

    init {
        val reader = BufferedReader(
            InputStreamReader(
                context.assets.open("tags.json"),
                StandardCharsets.UTF_8
            )
        )
        val text = reader.readText()
        tags = Json.decodeFromString<List<Tag>>(text).associateBy { it.name }
    }

    fun getTagFromName(name: String): Tag? {
        return tags[name]
    }
}