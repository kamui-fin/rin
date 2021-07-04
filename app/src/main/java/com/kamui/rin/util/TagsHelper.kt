package com.kamui.rin.util

import android.content.Context
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Serializable
data class Tag(
    val name: String,
    val category: String,
    val order: Int,
    val description: String,
    val score: Int,
    val color: String
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(category)
        parcel.writeInt(order)
        parcel.writeString(description)
        parcel.writeInt(score)
        parcel.writeString(color)
    }

    companion object CREATOR : Parcelable.Creator<Tag> {
        override fun createFromParcel(parcel: Parcel): Tag {
            return Tag(parcel)
        }

        override fun newArray(size: Int): Array<Tag?> {
            return arrayOfNulls(size)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
class TagsHelper(mContext: Context) {
    private var tags: List<Tag>

    init {
        val reader = BufferedReader(
            InputStreamReader(
                mContext.assets.open("tags.json"),
                StandardCharsets.UTF_8
            )
        )
        val text = reader.readText()
        tags = Json.decodeFromString(text)
    }

    fun getTagFromName(name: String): Tag {
        val defaultTag = Tag("", "", 0, "", 0, "#FFFFFF")
        return tags.find { t -> t.name == name } ?: defaultTag
    }
}