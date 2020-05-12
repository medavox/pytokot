package com.github.medavox.pytokot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class ArgParser:CliktCommand(name="pytokot") {
    val forceOverwrite:Boolean? by option("-f", "--force-overwrite").flag()
    val sources by argument().file(mustExist = true, mustBeReadable = true).multiple().unique()
    //val dest by argument().file(canBeFile = false)//todo: require a single file path/name if input is a single file,
    //                                                 or a directory if input is multiple files/directory
    override fun run() {
        /*Thread.setDefaultUncaughtExceptionHandler { thread:Thread, exception:Throwable ->
            err.println(exception.localizedMessage)
        }*/
        for (file in sources) {
            val converted = Python2Kotlin.transcribe(file.readText())
            val outputFile = File(file.absolutePath+".kt")
            echo("new file:"+file.absolutePath+".kt")
            if(!outputFile.exists() || forceOverwrite == true) {
                outputFile.writeText(converted)
            }
        }
    }
}

fun main(args:Array<String>) {
    ArgParser().main(args)
}