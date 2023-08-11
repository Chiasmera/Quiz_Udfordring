package com.chiasmera.quizudfordring

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Question (
    val category : String,
    val type : String,
    val difficulty : String,
    val question : String,
    val correctAnswer : String,
    val wrongAnswers : List<String>
) : Parcelable {

    override fun toString(): String {
        return question
    }
}