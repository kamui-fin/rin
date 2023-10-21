package com.kamui.rin.dict

import com.kamui.rin.db.model.Frequency
import com.kamui.rin.db.model.Tag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

val format = Json { ignoreUnknownKeys = true }

@kotlinx.serialization.Serializable
data class YomichanMeta(
    val title: String
)

@kotlinx.serialization.Serializable
data class YomichanDictionaryEntry(
    val expression: String,
    val reading: String,
    val definitionTags: String,
    val ruleIdentifiers: String,
    val popularity: Int,
    val meanings: List<String>,
    val sequence: Int,
    val termTags: List<String>,
)

fun decodeDictionaryEntries(stringData: String): List<YomichanDictionaryEntry> {
    val root: JsonArray = format.parseToJsonElement(stringData).jsonArray
    return root.map {
        YomichanDictionaryEntry(
            it.jsonArray[0].jsonPrimitive.content,
            it.jsonArray[1].jsonPrimitive.content,
            it.jsonArray[2].jsonPrimitive.content,
            it.jsonArray[3].jsonPrimitive.content,
            it.jsonArray[4].jsonPrimitive.intOrNull!!,
            it.jsonArray[5].jsonArray.toList().map { meaning -> meaning.jsonPrimitive.content },
            it.jsonArray[6].jsonPrimitive.intOrNull!!,
            it.jsonArray[7].jsonPrimitive.content.split(" "),
        )
    }
}

fun decodeFrequencyEntries(stringData: String): List<Frequency> {
    val root: JsonArray = format.parseToJsonElement(stringData).jsonArray
    return root.map {
        Frequency(
            kanji = it.jsonArray[0].jsonPrimitive.content,
            frequency = it.jsonArray[2].jsonPrimitive.long,
        )
    }
}


fun decodeTags(stringData: String, dictId: Long): List<Tag> {
    val root: JsonArray = format.parseToJsonElement(stringData).jsonArray
    return root.map {
        Tag(
            dictionaryId = dictId,
            name = it.jsonArray[0].jsonPrimitive.content,
            notes = it.jsonArray[3].jsonPrimitive.content,
        )
    }
}