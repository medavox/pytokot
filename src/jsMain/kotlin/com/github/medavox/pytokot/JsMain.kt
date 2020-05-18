package com.github.medavox.pytokot

import org.w3c.dom.*
import org.w3c.dom.events.Event
import kotlin.browser.document

fun main() {
    val uiStrings = UiStrings.English//todo: a system to choose UI language
    val slekt = document.getElementById("lang_select") as HTMLSelectElement
    /*Language.values().forEach {
        slekt.add(Option(
            text= it.neim,
            value = it.name,
            defaultSelected = false,
            selected = false
        ))
    }*/

    val inputTextArea = document.getElementById("input_text") as HTMLTextAreaElement
    val outputTextArea = document.getElementById("output_text") as HTMLTextAreaElement
    val errorsTextArea = document.getElementById("errors_text") as HTMLTextAreaElement

    inputTextArea.setAttribute("placeholder", uiStrings.inputHint)
    outputTextArea.setAttribute("placeholder", uiStrings.outputHint)
    errorsTextArea.setAttribute("placeholder", uiStrings.errorsHint)


    val button = document.getElementById("transliterate_button") as HTMLButtonElement
    button.addEventListener("click", { event:Event ->
        val transcribr = Pytokot
        outputTextArea.textContent = transcribr.transcribe(inputTextArea.value)
        //errorsTextArea.textContent = "transcriber: $transcribr"
    })

    //remove js warning
    val jsWarning = document.getElementById("js_warning") as HTMLDivElement
    jsWarning.remove()
}