package com.chiasmera.quizudfordring

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a category of quiz questions
 */
data class Category (
    val id : Int,
    val name : String,
) {
    var totalCount : Int = 0
    var easyCount : Int = 0
    var mediumCount : Int = 0
    var hardCount : Int = 0

    /**
     * Returns a JSON String representation of this Category
     */
    fun toJSONObject() : String {
            return "{\"id\":\"${id}\",\"name\":\"${name}\"}"
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        return other is Category && this.id == other.id
    }
}