package com.heartyculture.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageData: ByteArray,
    val disease: String = "",
    val plant: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageEntity

        if (id != other.id) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (disease != other.disease) return false
        if (plant != other.plant) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + disease.hashCode()
        result = 31 * result + plant.hashCode()
        return result
    }
}