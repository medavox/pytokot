package com.github.medavox.pytokot

import com.github.medavox.transcribers.*

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
    private val normalRules:List<BaseRule> = listOf(
        //slices
        //indentation to curly braces
        //list comprehensions
        //class defs
        //strings with single-quotes to double quotes (ignore in comments)
        //type tagging function args (with Any?)
        //make first use of a variable into its declaration (probably var)
        //main function
        //print calls
        //lambdas
        //dictionary declarations
        //lots of string functions, eg
        //string.lower() -> String.toLower()
        //len(obj) -> obj.size or obj.length
        // pass -> {} or something else?
        //comment out Python import statements and add a 'TODO' to find Kotlin replacements

            //TODO: for curly braces:
            //lookback consumed match on \n, then feed non-blank lines into a function which spits out "\n(INDENTATION)}" or "",
            //if the non-blank line has less indentation than the last one
            //switch to string mode when we encounter a single double-quote char (")
        BaseRule(Regex("\n"), Regex("(\\h*)(\\S+)"), { soFar:String, m:MatchGroupCollection ->
            soFar+m[1]!!.value+bracesFromIndents(m[1]!!.value)
        }, {it[1]!!.value.length}),
            //todo: need to only consume the number of chars in the line-initial whitespace,
            //which means we need access the the matchgroup in the context of lettersConsumed

        //False -> false
        WordBoundaryRule("True\\b", "true"),
        //True -> true
        WordBoundaryRule("False\\b", "false"),

        //functions 'def' to 'fun'
        WordBoundaryRule("def\\b", "fun"),

        WordBoundaryRule(Regex("if ([^:]+):"), { soFar:String, matches:MatchGroupCollection ->
            "${soFar}if (${matches[1]!!.value}) {"
        }),

        WordBoundaryRule(Regex("elif ([^:]+):"), { soFar:String, matches:MatchGroupCollection ->
            "${soFar}else if (${matches[1]!!.value}) {"
        }),

        WordBoundaryRule(Regex("else ?:"), { soFar:String, matches:MatchGroupCollection ->
            "${soFar}else {"
        }),

        CapturingRule(Regex("u'([^']+)'"), { soFar:String, matches:MatchGroupCollection ->
            "${soFar}\"${matches[1]!!.value}\""
        }),

        WordBoundaryRule("and\\b", "&&"),

        WordBoundaryRule("or\\b", "||"),

        //this has to occur before the single double-quote rule
        CapturingRule(Regex("\"\"\""), {s:String, m:MatchGroupCollection ->
            currentRuleset = multiLineStringMode
            s+m[0]!!.value
        }),

        CapturingRule(Regex("\""), { soFar:String, m:MatchGroupCollection ->
            //println("entering string mode")
            currentRuleset = doubleQuoteStringMode
            soFar+"\""}),

        //switch to comment mode when we encounter "#" (and convert it to a kotlin // comment)
        RevisingRule("#", { currentRuleset = commentToEndOfLineMode; "$it//"})//don't consume this, so the mode-switch rule below can match

    )

    /**ignore inside comments & string-literals mode:
    mode-switch upon a match*/
//todo: embed each of these Rule-lists-of-one-rule into the starting rule that switches to it
    //strings-mode, whose only rule is to switch back to normal mode upon matching the corresponding string-end regex
    private val doubleQuoteStringMode:List<BaseRule> = listOf(
        //switch back to normal-mode when we encounter a non-escaped double-quote character
        BaseRule(Regex("[^\\\\]"),Regex("\""), { soFar, matches ->
            //println("leaving string mode")
            currentRuleset = normalRules
            soFar+"\""
        })
    )

    //multiline-strings-mode, whose only rule is to switch back to normal mode upon matching the corresponding string-end regex
    private val multiLineStringMode:List<BaseRule> = listOf(
        //switch back to normal-mode when we encounter a non-escaped double-quote character
        CapturingRule(Regex("\"\"\""), { s, m ->
            //println("leaving string mode")
            currentRuleset = normalRules
            s+m[0]!!.value
        })
    )

    //comment-mode, whose only rule is to switch back to normal mode upon matching a newline
    private val commentToEndOfLineMode:List<BaseRule> = listOf(
        CapturingRule(Regex("\n"), {soFar:String, matches:MatchGroupCollection ->
            currentRuleset = normalRules
            soFar+matches[0]!!.value
        })
    )

    private var currentRuleset = normalRules

    private var lastIndentation = ""
    private fun bracesFromIndents(spaces:String):String {
        //if indentation spaces are less than last time,
        //add a line before it with a "}" on it
        val ret = if(spaces.length < lastIndentation.length) {
            "}\n$spaces"
        }else { "" }//else just return an empty string
        lastIndentation = spaces

        return ret
    }

    override fun transcribe(nativeText: String): String {
        return nativeText.processWithRules({ currentRuleset}, copy)
    }
}