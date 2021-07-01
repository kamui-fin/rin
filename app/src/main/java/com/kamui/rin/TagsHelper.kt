package com.kamui.rin

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Tag(
    val name: String,
    val category: String,
    val order: Int,
    val description: String,
    val score: Int,
    val color: String
)

@RequiresApi(Build.VERSION_CODES.KITKAT)
class TagsHelper(mContext: Context) {
    private var tags: List<Tag>

    init {
        val reader = BufferedReader(InputStreamReader(mContext.assets.open("tag_bank.json"), StandardCharsets.UTF_8))
        var mLine: String
        val readText = StringBuilder()
        while (reader.readLine().also { mLine = it } != null) {
            readText.append(mLine)
        }
        tags = Json.decodeFromString(readText.toString())
    }

    fun getTagFromName(name: String): Tag {
        val defaultTag = Tag("", "", 0, "", 0, "#FFFFFF")
        return tags.find { t -> t.name == name } ?: defaultTag
    }
}