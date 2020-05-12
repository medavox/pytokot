package com.github.medavox.pytokot

import com.github.medavox.ipa_transcribers.CapturingRule
import com.github.medavox.ipa_transcribers.IRule
import com.github.medavox.ipa_transcribers.Rule
import com.github.medavox.ipa_transcribers.RuleBasedTranscriber

/*
It's pretty simple really:

a[start:stop]  # items start through stop-1
a[start:]      # items start through the rest of the array
a[:stop]       # items from the beginning through stop-1
a[:]           # a copy of the whole array

There is also the step value, which can be used with any of the above:

a[start:stop:step] # start through not past stop, by step

The key point to remember is that the :stop value represents the first value that is not in the selected slice. So, the difference between stop and start is the number of elements selected (if step is 1, the default).

The other feature is that start or stop may be a negative number, which means it counts from the end of the array instead of the beginning. So:

a[-1]    # last item in the array
a[-2:]   # last two items in the array
a[:-2]   # everything except the last two items

Similarly, step may be a negative number:

a[::-1]    # all items in the array, reversed
a[1::-1]   # the first two items, reversed
a[:-3:-1]  # the last two items, reversed
a[-3::-1]  # everything except the last two items, reversed

Python is kind to the programmer if there are fewer items than you ask for. For example, if you ask for a[:-2] and a only contains one element, you get an empty list instead of an error. Sometimes you would prefer the error, so you have to be aware that this may happen.
* */
object Python2Kotlin: RuleBasedTranscriber() {
    //todo: ignore comments & string-literals mode
    private val rules:List<IRule> = listOf(
        //slices
        //indentation to curly braces
        //list comprehensions
        //class defs
        //strings with single-quotes to double quotes (ignore in comments)
        //single-line comments # to //
        //functions 'def' to 'fun'
        //type tagging function args (with Any?)
        //make first use of a variable into its declaration (probably var)
        //main function
        //print calls
        //lambdas
        //dictionary declarations
        //lots of string functions, eg
        //string.lower() -> String.toLower()
        //len(obj) -> obj.size or obj.length
        //False -> false, True -> true
        // pass -> {} or something else?
        //comment out Python import statements and add a 'TODO' to find Kotlin replacements
        CapturingRule(Regex("^(\\s*)if (.+):([^\\n]*)$"), {
                soFar:String, matches:MatchGroupCollection ->
            "${matches[1]}if (${matches[2]}) {${matches[3]}"
        }),

        CapturingRule(Regex("^(\\s*)elif (.+):([^\\n]*)$"), {
                soFar:String, matches:MatchGroupCollection ->
            "${matches[1]}} else if (${matches[2]}) {${matches[3]}"
        }),

        CapturingRule(Regex("^(\\s*)else ?:([^\\n]*)$"), {
                soFar:String, matches:MatchGroupCollection ->
            "${matches[1]}} else {${matches[3]}" }),

        CapturingRule(Regex("u'([^']+)'"), {
                soFar:String, matches:MatchGroupCollection ->
            "\"${matches[1]}\"" }),

        Rule(Regex("\\band\\b"), "&&"),

        Rule(Regex("\\bor\\b"), "||")
    )
    private var lastSeenIndentationSpaces = 0
    private fun indentationSpaces(spaces:Int) {
        //if line is non-empty, AND
        //indentation spaces are less than last time,
        //add a line before it with a "}" on it
    }

    override fun transcribe(nativeText: String): String {
        TODO("Not yet implemented")
    }
}