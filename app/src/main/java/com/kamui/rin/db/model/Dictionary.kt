package com.kamui.rin.db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
)
data class Dictionary(
    @PrimaryKey(autoGenerate = true) var dictId: Long = 0,
    @ColumnInfo(name = "name", typeAffinity = ColumnInfo.TEXT) var name: String,
    @ColumnInfo(name = "order", typeAffinity = ColumnInfo.INTEGER) val order: Int = 0,
) : Comparable<Dictionary> {

    override operator fun compareTo(other: Dictionary): Int {
        return order.compareTo(other.order)
    }
}
