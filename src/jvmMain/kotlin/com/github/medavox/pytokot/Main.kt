package com.github.medavox.pytokot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.medavox.transcribers.BaseRule
import com.github.medavox.transcribers.WordBoundaryRule
import java.io.File

class ArgParser:CliktCommand(name="pytokot") {
    val forceOverwrite:Boolean? by option("-f", "--force-overwrite").flag()
    val sources by argument().file(mustExist = true, mustBeReadable = true).multiple().unique()
    //val tabSize by option("--tab-size", "-t").int().default(4)
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

fun BaseRule.matchesAt(string:String):Boolean {
    for(position in string.indices) {
        val processingWord = string.substring(position, string.length)
        val consumed = string.substring(0, position)
        //println("UNCONSUMED STRING: $processingWord")
        //println("CONSUMED STRING: $consumed")
        val unconsumedMatch: MatchResult? = unconsumedMatcher.find(processingWord)
        //println("unconsumed matches: ${unconsumedMatch != null}")

        val consumedMatches: Boolean = consumedMatcher == null ||// if it's null, that counts as matching:
                //rules that don't specify a consumedMatcher aren't checked against it

                //if it has been specified by this rule, it has to match at the end of the already-consumed string
                consumedMatcher!!.findAll(consumed).lastOrNull()?.range?.endInclusive == consumed.length - 1
        //println("consumed matches: $consumedMatches")
        //println("unconsumed matches at start: ${unconsumedMatch?.range?.start == 0}")
        //if the rule matches the start of the remaining string, and the end of the consumed string
        if(consumedMatches && unconsumedMatch?.range?.start == 0){
            println("rule MATCHES at:\n$processingWord")
            return true
        }
    }
    return false
}

fun main(args:Array<String>) {
    val debug = false
    if(debug) {
        println("matches: ${
        WordBoundaryRule(Regex("if ([^:]+):([^\\n]*)"), { soFar: String, matches: MatchGroupCollection ->
            "${soFar}if (${matches[1]!!.value}) {${matches[2]!!.value}"
        })
                //LookbackRule(Regex("(^|[^a-zA-Z_0-9])"), Regex("or\\b"), "||")
                //.matchesAt(File("/home/scc/src/mine/Ysgrifen/rendering/render.py").readText())}")
                .matchesAt(File("/home/scc/src/others/vPhon/vPhon.py").readText())}")
    }else { ArgParser().main(args) }
}