package com.github.medavox.pytokot

sealed class UiStrings {
    abstract val errorsHint: String
    abstract val outputHint: String
    abstract val inputHint: String

    object English : UiStrings() {
        override val errorsHint: String = "Errors"
        override val outputHint: String = "Converted Kotlin code"
        override val inputHint: String = "Enter python code here"
    }
}