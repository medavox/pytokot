package com.github.medavox.pytokot

import org.w3c.dom.HTMLTextAreaElement
import kotlin.browser.document

actual object err {
    val errorsTextArea = document.getElementById("errors_text") as HTMLTextAreaElement
    actual fun print(err: String) {
        errorsTextArea.textContent += err
    }

    actual fun println(err: String) {
        return print(err+"\n")
    }
}

