package com.github.medavox.ipa_transcribers

object Python2Kotlin:RuleBasedTranscriber() {
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
        CapturingRule(Regex("^(\\s*)if (.+):([^\\n]*)$"), {
                soFar:String, theMatches:MatchGroupCollection ->
            "\\1if (\\2) {\\3"
        }),

        CapturingRule(Regex("^(\\s*)elif (.+):([^\\n]*)$"), {
                soFar:String, theMatches:MatchGroupCollection ->
            "\\1} else if (\\2) {\\3"
        }),

        CapturingRule(Regex("^(\\s*)else ?:([^\\n]*)$"), {
                soFar:String, theMatches:MatchGroupCollection ->
            "\\1} else {\\3" }),

        CapturingRule(Regex("u'([^']+)'"), {
                soFar:String, theMatches:MatchGroupCollection ->
            "\"\\1\"" }),

        Rule(Regex("\\band\\b"), "&&"),

        Rule(Regex("\\bor\\b"), "||")
    )
    private var lastSeenIndentationSpaces = 0
    private fun indentationSpaces(spaces:Int) {
        //if line is non-empty, AND
        //indentation spaces are less than last time,
        //add a line before it with a "}" on it
    }
    override val completionStatus: CompletionStatus
        get() = TODO("Not yet implemented")


    override fun transcribe(nativeText: String): String {
        TODO("Not yet implemented")
    }
}