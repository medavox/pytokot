package com.github.medavox.pytokot

import com.github.medavox.pytokot.Shims.Keys.stringSize
import com.github.medavox.transcribers.*

/*
Python is kind to the programmer if there are fewer items than you ask for.
For example, if you ask for a[:-2] and a only contains one element,
you get an empty list instead of an error.
Sometimes you would prefer the error, so you have to be aware that this may happen.
* */
/*TODO: 'var' insertion algorithm idea:
Have a MutableList<MutableSet<String>>
The set contains all the variable assignments we've seen before at that indentation level
The last item in the list is the current indentation
Append a new MutableSet to the list upon every indentation increase
Remove the last MutableSet on every indendation decrease
Every time we encounter a new variable assignment (detected with <name> = <whatever> or similar),
check if that variable has been seen in this current indentation set, or any of the ones above/before it
If it's nowhere in the List<Set>, then prepend 'var' to the front, before the variable name
*/
var tabSize:Int = 4
object Pytokot: RuleBasedTranscriber() {
    val man: (String.(rules:()->List<BaseRule>, onNoRuleMatch:NoMatchHandler) -> String) = String::processFasterWithDynamicRules
    fun String.ruleProcess(riwls:()->List<BaseRule>, onNoRuleMatch:NoMatchHandler): String = man(riwls, onNoRuleMatch)
    fun String.ruleProcess(riwlz:List<BaseRule>, onNoRuleMatch:NoMatchHandler): String = man({riwlz}, onNoRuleMatch)
    var wasInsideString = false
    private val shims = Shims()
    val stringRules = listOf(

        //'' -> ""
        Rule("\'\'", "\"\""),

        //keep single-character python strings as Kotlin Char literals; this is to prevent the next rule consuming them
        CapturingRule(Regex("\'([^\'])\'"), {s, m -> s + "\'${m[1]!!.value}\'"}),


/*        CapturingRule(Regex("\'([^\']{2,})"), {s, m ->
            currentRuleset = ignoreUntil("[^\\\\]", "\'")
            s + "\'${m[1]!!.value}\'"
        }),*/

        //this has to occur before the single double-quote rule
        CapturingRule(Regex("\"\"\""), {s:String, m:MatchGroupCollection ->
            wasInsideString = true
            currentRuleset = ignoreUntil("\"\"\"")
            s+m[0]!!.value
        }),

        CapturingRule(Regex("\""), { soFar:String, _ ->
        wasInsideString = true
        currentRuleset = ignoreUntil(Regex("[^\\\\]"), Regex("\""))
        soFar+"\""})
    )
    private val normalRules:List<BaseRule> = listOf(
        //list comprehensions
        //class defs
        //strings with single-quotes to double quotes (ignore in comments)
        //type tagging function args (with Any?)
        //(somehow) make first use of a variable into its declaration (probably var)
        //main function
        //print calls
        //lambdas
        //dictionary declarations
        //list literals
        //tuple literals
        //lots of string functions, eg string.lower() -> String.toLower()
        //built-in functions
        //comment out Python import statements and add a 'TODO' to find Kotlin replacements
        //with

        //switch to string mode when we encounter a single double-quote char (")
        //fixme: consider replacing \n with ^
        BaseRule(Regex("\\n"), Regex("(\\h*)(\\S+)"), { soFar:String, m:MatchGroupCollection ->
            soFar+bracesFromIndents(m[1]!!.value.indentation, m[2]!!.value)
        }, {it[1]!!.value.length}),

        //slice: one arg
        //a[-1]    # last item in the array
        CapturingRule(Regex("\\[(-?\\d+)]"), {s, m ->
            shims.enable(Shims.Keys.slices)
            s+"!!.s(${m[1]!!.value})"
        }),

        //slice: two args
        //a[-2:]   # last two items in the array
        //a[:-2]   # everything except the last two items
        //a[-5:-2]
        CapturingRule(Regex("\\[((?:-?\\d+)?):((?:-?\\d+)?)]"), {s, m ->
            shims.enable(Shims.Keys.slices)
            s+"!!.s(${if(m[1]!!.value.isEmpty()) "null" else m[1]!!.value}, " +
                   "${if(m[2]!!.value.isEmpty()) "null" else m[2]!!.value})"
        }),

        //slice: three args
        //Similarly, step may be a negative number:
        //a[::-1]    # all items in the array, reversed
        //a[1::-1]   # the first two items, reversed
        //a[:-3:-1]  # the last two items, reversed
        //a[-3::-1]  # everything except the last two items, reversed
        CapturingRule(Regex("\\[((?:-?\\d+)?):((?:-?\\d+)?):((?:-?\\d+)?)]"), {s, m ->
            shims.enable(Shims.Keys.slices)
            s+"!!.s(${if(m[1]!!.value.isEmpty()) "null" else m[1]!!.value}, " +
                    (if(m[2]!!.value.isEmpty()) "null" else m[2]!!.value) +
                    "${if(m[3]!!.value.isEmpty()) "" else m[3]!!.value})"
        }),

        WordBoundaryRule("True\\b", "true"),
        WordBoundaryRule("False\\b", "false"),
        //comment out but don't delete 'pass'es.
        //they are needed for indentation-to-closing-brace conversion
        WordBoundaryRule("pass\\b", "//pass"),
        WordBoundaryRule("and\\b", "&&"),
        WordBoundaryRule("or\\b", "||"),

        //len(obj) -> obj.size or obj.length
        WordBoundaryRule(Regex("len\\(([^)]+)\\)"), {s, m ->
            shims.enable(stringSize)
            s+m[1]!!.value+".size"
        }),

        RuleBuilder(Regex("def (\\w+)\\((?:(?:([a-zA-Z]\\w+) ?,? ?)+)*\\h*\\):"))
            .afterWordBoundary()
            .outputString { s:String, m:MatchGroupCollection ->
                val args = StringBuilder(s+"fun "+m[1]!!.value+"(")
                for(i in 2 until m.size) {
                    if(m[i] != null) {
                        args.append(m[i]!!.value + ":Any" + if (i < m.size - 1) "," else "")
                    }
                }
                args.toString()+") {"
            }.build(),

        WordBoundaryRule(Regex("for\\h*([^:]+):"), {s, m ->
            s+"for ("+m[1]!!.value+") {"
        }),

        //if and else-if statements
        RuleBuilder(Regex("(el)?if ([^\\n]+):")).afterWordBoundary()//\h*(#[^\n]*)?\n
            .outputString { s, m ->
                //println("matches: "+m)
                s+(if(m[1] != null) "else " else "")+//add optional else
                        "if (${m[2]!!.value.ruleProcess(currentRuleset, copy)}) {"
            }.build(),
        WordBoundaryRule("else ?\\:", "else {" ),

        CapturingRule(Regex("u'([^']+)'"), { soFar:String, matches:MatchGroupCollection ->
            "${soFar}\"${matches[1]!!.value}\""
        }),

        //fallback rule: end-of-line colons -> curly braces
        //Rule("\\h*:\\h*\\n", " {\n"),

        //switch to comment mode when we encounter "#" (and convert it to a kotlin // comment)
        RevisingRule("#", { currentRuleset = ignoreUntil("\n"); "$it//"})
    )+ stringRules
    fun ignoreUntil(closingRegexToDetect:Regex):List<BaseRule> = ignoreUntil(null, closingRegexToDetect)
    fun ignoreUntil(closingRegexToDetect:String):List<BaseRule> = ignoreUntil(Regex(closingRegexToDetect))
    fun ignoreUntil(consumedMatcher:Regex?=null, closingRegexToDetect:Regex, replacement:String?=null):List<BaseRule> = listOf(
        BaseRule(consumedMatcher, closingRegexToDetect, { s:String, m:MatchGroupCollection ->
            currentRuleset = normalRules
            wasInsideString = false
            s + (replacement ?: m[0]!!.value)
        })
    )

    private var currentRuleset = normalRules
    private var lineNumber = 0
    private val openIndents = mutableListOf<Int>()
    /**if indentation spaces are less than last time,
    add a line before it with a "}" on it
    spits out "\n(INDENTATION)}" or "",
    depending on if the non-blank line has less indentation than the last one*/
    private fun bracesFromIndents(newIndentation:Int, debugLineHint:String=""):String {
        //EUREKA: the line that triggers this algo IS ALSO A LINE WITH ITS OWN INDENT TO BE ADDED
        lineNumber++
        //println("called braces function on line $lineNumber : \"$debugLineHint\"")
        val debug = false
        //val debug = (newIndentation !in openIndents)
        if (debug) println( "FOR LINE STARTING \"$debugLineHint\": ")
        val ret = if(openIndents.isNotEmpty() && newIndentation < openIndents.last()) {
            if (debug) println(openIndents.fold("CLOSING BRACES; stack:"){ acc, el -> "$acc $el,"})
            if (debug) println("input: $newIndentation")
            val outputBraces = openIndents.subList(0, openIndents.size-1).filter { it >= newIndentation}
            val bracesToRemove = outputBraces+openIndents.last()
            if (debug) println(outputBraces.fold("output: "){ acc, el -> "$acc $el,"})
            if (debug) println(bracesToRemove.fold("removed: "){ acc, el -> "$acc $el,"})
            openIndents.removeAll(bracesToRemove)

            outputBraces.reversed().fold("") {acc, el -> acc+" ".repeat(el)+"}\n" }+" ".repeat(newIndentation)
        }else {//indentations are empty, or the new indentation is not less than the last
            if (debug) println(openIndents.fold("NOT BRACING; stack: "){ acc, el -> "$acc $el,"})
            " ".repeat(newIndentation)
        }
        if(openIndents.isEmpty() || newIndentation > openIndents.last()) {
            //indentation has increased, add it to the stack
            if (debug) println("added $newIndentation")
            openIndents.add(newIndentation)
            //println(" ".repeat(openIndents.last())+">")
        }
        if (debug) println()
        return ret
    }

    /**Add up the total value, in spaces, of all the indentation in the argument string.*/
    private val String.indentation:Int get()  {
        return count { it == ' ' } + (count { it == '\t' } * tabSize)
    }


    override fun transcribe(nativeText: String): String {
        return nativeText.ruleProcess({ currentRuleset}, copy)+shims.getNeededShims()
        //return nativeText.processDalaiWithRules({ currentRuleset}, copy)+shims.getNeededShims()
    }
}