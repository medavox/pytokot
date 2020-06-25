package com.github.medavox.pytokot

import com.github.medavox.pytokot.Pytokot.stringRules
import com.github.medavox.transcribers.BaseRule
import com.github.medavox.transcribers.NoMatchHandler
import com.github.medavox.transcribers.RuleBasedTranscriber.*

fun String.processDalaiWithRules(rules:List<BaseRule>, onNoRuleMatch:NoMatchHandler) : String =
    this.processDalaiWithDynamicRules({rules}, onNoRuleMatch)
fun String.processDalaiWithDynamicRules(rules:()->List<BaseRule>, onNoRuleMatch:NoMatchHandler) : String {
    var out:String = ""
    var processingWord:String = this
    var consumed = ""
    loop@ while(processingWord.isNotEmpty()) {
        //uses the first rule which matches -- so rule order matters
        for (rule in rules()) {
            val unconsumedMatch:MatchResult? = rule.unconsumedMatcher.find(processingWord)
            val gek = rule.consumedMatcher?.let { (rule.consumedMatcher as Regex).findAll(consumed).lastOrNull()}
            val consumedMatches:Boolean = rule.consumedMatcher == null ||// if it's null, that counts as matching:
                    //rules that don't specify a consumedMatcher aren't checked against it

                    //if it has been specified by this rule, it has to match at the end of the already-consumed string

                    gek?.range?.endInclusive == consumed.length-1

            //if the rule matches the start of the remaining string, and the end of the consumed string
            if(consumedMatches && unconsumedMatch?.range?.start == 0) {
                if(rule.consumedMatcher != null) {
                    println(gek?.range?.endInclusive)
                }
                if(Pytokot.wasInsideString || rule in stringRules) {
                    println(if(Pytokot.wasInsideString){"END  "}else{"START"}+ ": |${processingWord.subSequence(
                            0, kotlin.math.min(16, processingWord.length)
                        )}|\n")
                }
                out = rule.outputString(out, unconsumedMatch.groups)
                //number of letters consumed is the match length, unless explicitly specified
                val actualLettersConsumed = rule.lettersConsumed?.invoke(unconsumedMatch.groups) ?: unconsumedMatch.value.length
                if(actualLettersConsumed > 0) {
                    consumed += processingWord.substring(0, actualLettersConsumed)
                    processingWord = processingWord.substring(actualLettersConsumed)
                    continue@loop
                }//else keep going through the rule list
            }
        }
        //no rule matched; call the lambda!
        val unmatchedOutput = onNoRuleMatch(processingWord, 1)
        processingWord = processingWord.substring(unmatchedOutput.indexAdvance)
        consumed += processingWord.substring(0, unmatchedOutput.indexAdvance)
        out = unmatchedOutput.output(out)
    }
    //System.out.println("consumed: $consumed")
    return out
}

fun BaseRule.asString(): String {
    //this.outputString.
    return "BaseRule("+(if(consumedMatcher==null) "" else "consumedMatcher=\"$consumedMatcher\", ")+
            "unconsumedMatcher=$unconsumedMatcher, " +
            "outputString=${outputString}, lettersConsumed=$lettersConsumed)"
}
fun MatchResult.asString():String = "${range.first}:${range.last} = \"${groupValues[0].esc()}\""
//fixme: choosing the next earliest rule to match is not the same as choosing the first matching rule in the list
fun String.processFasterWithRules(rules:List<BaseRule>, onNoRuleMatch:(remainingInput:String, unmatchedChars:Int) -> UnmatchedOutput
) : String = this.processFasterWithDynamicRules({rules}, onNoRuleMatch)

fun String.processFasterWithDynamicRules(rules:()->List<BaseRule>,
                                         onNoRuleMatch:(remainingInput:String, unmatchedChars:Int) -> UnmatchedOutput
) : String {
    var out:String = ""
    var i = 0
    val alreadyRunRules = mutableSetOf<BaseRule>()
    val debug = true
    while(i < this.length) {
        //val unconsumedPreview = (if(this.substring(i).length > 32) this.substring(i, i+32) else this.substring(i)).esc()

        val processingWord = this.substring(i)
        val consumed = this.substring(0, i)

        println("\nAT POSITION $i:")
        //NOTE: MatchResult.range.first) is unchanged by whether we pass a startingIndex to Regex.find()
        //println("already run rules: "+alreadyRunRules.size)
        //use some fancy collections lambda function to only call .find() and create a MatchResult once
        val hull:List<Pair<BaseRule, MatchResult?>> = (rules() - alreadyRunRules).
            //run the unconsumed regex on every rule
            map { rule: BaseRule -> Pair<BaseRule, MatchResult?>(rule, rule.unconsumedMatcher.find(this, i)) }.
            filter { (rule, result) -> result != null }.//filter out rules whose unconsumedMatcher don't match
            map { Pair(it.first, it.second!!) }.//cast to List<Pair<BaseRule, MatchResult>>
            filter { (rule, result:MatchResult) ->
                //val consumedAndSkipped = this.substring(0, i + result!!.value.length)
                val consumedMatches = rule.consumedMatcher?.findAll(consumed)
                val numConsumedMatches = consumedMatches?.toList()

                val lastConsumedMatch = consumedMatches?.lastOrNull()//we want UNTIL i, not FROM
                // if the rule's consumedMatcher is null, that counts as matching:
                //rules that don't specify a consumedMatcher aren't checked against it
                //if it's not null, then it has to match at the end of the already-consumed string
                val consumedDoesMatch = rule.consumedMatcher == null ||
                        consumedResults?.lastOrNull()?.range?.endInclusive == result.range.first -1
                /*if(lastConsumedMatch != null && numConsumedMatches != null && numConsumedMatches.size > 0 && !doesMatch) {
                    println("-----------------------------------\nat position (consumed+1) $i: $unconsumedPreview")
                    println("\tlast 32 consumed chars:"+this.substring(max(i-32, 0), i).esc())
                    if(debug)println("\tFOR UNMATCHED CONSUMED-CHECKING RULE \"${rule.label}\":")
                    //if(debug)println("\t\tconsumed does match: $doesMatch, number of matches: $numConsumedMatches")
                    val lastMatchPreviewEnd = if(lastConsumedMatch.range.start+24 < lastConsumedMatch.range.endInclusive-lastConsumedMatch.range.start) {
                        lastConsumedMatch.range.start+24
                    } else lastConsumedMatch.range.endInclusive+1
                    if(debug)println("\t\tlast match: ${lastConsumedMatch.range}: "+
                            this.substring(lastConsumedMatch.range.start, lastMatchPreviewEnd).esc())
                    if(debug)println("\t\tunconsumed result.range.start:${result!!.range.start}")
                    if(debug)println("\t\tunconsumed result.value.length:${result!!.value.length}")
                }*/
                //if(rule.label == "closing braces" && processingWord.startsWith("\t\tsuche")) {
                //if(i >= 1413) {
                if(consumedDoesMatch) {
                    println("\tRule \"${rule.label}\" | ${consumedResults?.lastOrNull()?.asString() ?: "<null>"} ; ${result.asString()}")
                }
                //we want to examine the chosen rule (and only the chosen rule) in this context, with all these variables
                consumedDoesMatch
            }.
            sortedBy { (rule, result) -> result.range.first }//sort by earliest match
                //need to get the list of rules after each rule!
        //(earliestMatchingRules, earliestMatchingResults)
        //no rules matched anywhere, at all
        //(no rules match before the end of the remaining input)
        //this means the rest of the input doesn't match any rules,
        //so call the lambda on the remaining string
        if(hull.isEmpty()) return onNoRuleMatch(processingWord, processingWord.length).output(out) //else, if it's null:

        val earliestMatchingRule:BaseRule  = hull[0].first//hu.firstOrNull()?:return
        println("selected rule: "+earliestMatchingRule.label)
        val earliestMatchingResult:MatchResult =  hull[0].second!!
        //.firstOrNull()?:return onNoRuleMatch(processingWord, processingWord.length).output(out) //else, if it's null:
        //last 8 consumed chars

/*        val possibleRules:String = hull.fold("", { acc:String, elem:Pair<BaseRule, MatchResult?> ->
            acc+"\n\t\t\"${elem.first.label}\" after ${elem.second!!.range.start} skipped chars"
        })*/
        //the brace closer doesn't match before the next normal rule does (because its \n match is in its consumed), so it might always be 2nd or whatever
        //println("\nUSED RULE \"${earliestMatchingRule.label}\" from ${rules().size} rules, ${hull.size} matching")

        //there are actually 2 separate input-consumption steps here, one after the other:
        //1. process any non-matching chars which precede the rule match by passing it to the provided function,
        //2. consume the input that the matching rule actually matched

        //println("rule: $earliestMatchingRule ; result: ${earliestMatchingResult?.groupValues}")
        //call the lambda on any unmatched characters before the earliest match
        if(earliestMatchingResult!!.range.first > i) {
            val unmatchedOutput = onNoRuleMatch(processingWord, earliestMatchingResult.range.first - i )//number of chars between i and the start of the match result
            //println("\t${earliestMatchingResult.range.first - i} unmatched chars skipped before match:"+processingWord.substring(i, earliestMatchingResult.range.first).esc())
            i += unmatchedOutput.indexAdvance
            out = unmatchedOutput.output(out)
            //println("already run rules before emptying: ${alreadyRunRules.size}: $alreadyRunRules")
            //whenever i increments, clear out the list of already-run rules
            alreadyRunRules.removeAll { true }
        }

        out = earliestMatchingRule.outputString(out, earliestMatchingResult.groups)
        //number of letters consumed is the match length, unless explicitly specified
        val actualLettersConsumed = earliestMatchingRule.lettersConsumed?.invoke(earliestMatchingResult.groups) ?: earliestMatchingResult.value.length
        if(actualLettersConsumed > 0) {
            i += actualLettersConsumed
            //println("\t$actualLettersConsumed chars consumed: "+earliestMatchingResult.value)
//            println("\t replacement: $earliestMatchingRule.")
            alreadyRunRules.removeAll { true }
        } else {//no characters were consumed
            //add the just-processed rule to the list of rules that have already been run, so we know not to run it again on the same input
            alreadyRunRules.add(earliestMatchingRule)
        }
    }
    return out
}

fun String.esc():String = this.replace("\n", "\\n")
