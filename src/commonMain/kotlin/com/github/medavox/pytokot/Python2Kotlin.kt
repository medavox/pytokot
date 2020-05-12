package com.github.medavox.pytokot

import com.github.medavox.ipa_transcribers.*

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
    private val normalRules:List<IRule> = listOf(
        //slices
        //indentation to curly braces
        //list comprehensions
        //class defs
        //strings with single-quotes to double quotes (ignore in comments)

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

        Rule(Regex("\\bor\\b"), "||"),

        //switch to string mode when we encounter a single double-quote char (")
        RevisingRule(Regex("([^\"]|^)\"([^\"]|$)"), { currentRuleset = doubleQuoteStringMode; it}, 0),

        Rule("#", "//", 0),//don't consume this, so the mode-switch rule below can match
        //switch to comment mode when we encounter "//" (python # comments will always get converted first)
        RevisingRule(Regex("//"), { currentRuleset = commentToEndOfLineMode; it}, 0)

    )

    /**ignore inside comments & string-literals mode:
    mode-switch upon a match*/

    //strings-mode, whose only rule is to switch back to normal mode upon matching the corresponding string-end regex
    private val doubleQuoteStringMode:List<IRule> = listOf(
        //switch back to normal-mode when we encounter a non-escaped double-quote character
        RevisingRule(Regex("[^\\\\]\""), { currentRuleset = normalRules; it}, 0)
    )

    //comment-mode, whose only rule is to switch back to normal mode upon matching a newline
    private val commentToEndOfLineMode:List<IRule> = listOf(
        RevisingRule(Regex("\n"), { currentRuleset = normalRules; it}, 0)
    )

    private var currentRuleset = normalRules

    private var lastSeenIndentationSpaces = 0
    private fun indentationSpaces(spaces:Int) {
        //if line is non-empty, AND
        //indentation spaces are less than last time,
        //add a line before it with a "}" on it
    }

    override fun transcribe(nativeText: String): String {
        return nativeText.processWithRules({ currentRuleset}, copy)
    }
}