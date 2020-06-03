package com.github.medavox.pytokot

import com.github.medavox.pytokot.Pytokot.stringRules
import com.github.medavox.transcribers.BaseRule
import com.github.medavox.transcribers.RuleBasedTranscriber.*


fun String.processDalaiWithRules(rules:List<BaseRule>, onNoRuleMatch:(unmatched:String) -> UnmatchedOutput) : String =
    this.processDalaiWithDynamicRules({rules}, onNoRuleMatch)
fun String.processDalaiWithDynamicRules(rules:()->List<BaseRule>, onNoRuleMatch:(unmatched:String) -> UnmatchedOutput) : String {
    var out:String = ""
    var processingWord:String = this
    var consumed = ""
    loop@ while(processingWord.isNotEmpty()) {
        //uses the first rule which matches -- so rule order matters
        for (rule in rules()) {
            val unconsumedMatch:MatchResult? = rule.unconsumedMatcher.find(processingWord)

            val consumedMatches:Boolean = rule.consumedMatcher == null ||// if it's null, that counts as matching:
                    //rules that don't specify a consumedMatcher aren't checked against it

                    //if it has been specified by this rule, it has to match at the end of the already-consumed string
                    (rule.consumedMatcher as Regex).findAll(consumed).lastOrNull()?.range?.endInclusive == consumed.length-1

            //if the rule matches the start of the remaining string, and the end of the consumed string
            if(consumedMatches && unconsumedMatch?.range?.start == 0) {
                if(Pytokot.wasInsideString || rule in stringRules) {
                    println(if(Pytokot.wasInsideString){"END  "}else{"START"}+ ": |${processingWord.subSequence(
                            0,
                            kotlin.math.min(16, processingWord.length)
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
        val unmatchedOutput = onNoRuleMatch(processingWord)
        processingWord = processingWord.substring(unmatchedOutput.indexAdvance)
        consumed += processingWord.substring(0, unmatchedOutput.indexAdvance)
        out = unmatchedOutput.output(out)
    }
    //System.out.println("consumed: $consumed")
    return out
}

fun String.processFasterWithRules(rules:List<BaseRule>, onNoRuleMatch:(remainingInput:String, unmatchedChars:Int) -> UnmatchedOutput
) : String = this.processFasterWithDynamicRules({rules}, onNoRuleMatch)
fun String.processFasterWithDynamicRules(rules:()->List<BaseRule>,
                                         onNoRuleMatch:(remainingInput:String, unmatchedChars:Int) -> UnmatchedOutput
) : String {
    var out:String = ""
    var i = 0
    val alreadyRunRules = mutableSetOf<BaseRule>()
    loop@ while(i < this.length) {
        val processingWord = this.substring(i, this.length)
        //uses the first rule which matches -- so rule order matters
        //use some fancy collections lambda function to only call .find() and create a MatchResult once
        val (earliestMatchingRule, earliestMatchingResult) = (rules() - alreadyRunRules).
            map { rule: BaseRule -> Pair<BaseRule, MatchResult?>(rule, rule.unconsumedMatcher.find(processingWord)) }.
            filter { (rule, result) ->
                val doesMatch = result != null && (
                        rule.consumedMatcher == null ||
                        rule.consumedMatcher?.findAll(this.substring(0, result.range.start))?.lastOrNull()?.range?.endInclusive == result.range.start-1 )
                if(rule.consumedMatcher.toString() == "\\n") {
                    println("consumed matcher \"${rule.consumedMatcher.toString()}\" matches: $doesMatch")
                    println("\tits unconsumedMatcher: "+rule.unconsumedMatcher.toString())
                    println("\tfor input beginning: ${processingWord.subSequence(0, kotlin.math.min(16, processingWord.length))}\n")
                }
                doesMatch
                // if the rule's consumedMatcher is null, that counts as matching:
                //rules that don't specify a consumedMatcher aren't checked against it
                    //if it has been specified by this rule, it has to match at the end of the already-consumed string
            }.//filter out rules that don't match
            sortedBy { (rule, result) -> result!!.range.start }.//sort by earliest match
                //filter out rules whose consumedMatcher don't 'match':
                //need to get the list of rules after each rule!
            firstOrNull() ?: //else, if it's null:
            //no rules matched anywhere, at all
            //(no rules match before the end of the remaining input)
            //this means the rest of the input "doesn't match"

            //so call the lambda on the remaining string
            return onNoRuleMatch(processingWord, processingWord.length).output(out)
        println("\nUSED RULE \"$earliestMatchingRule\"")/* to consume: |${processingWord.subSequence(
            0, kotlin.math.min(16, processingWord.length)
        )}|\n")*/

        //there are actually 2 separate input-consumption steps here, one after the other:
        //1. process any non-matching chars which precede the rule match by passing it to the provided function,
        //2. consume the input that the matching rule actually matched

        //println("rule: $earliestMatchingRule ; result: ${earliestMatchingResult?.groupValues}")
        //call the lambda on any unmatched characters before the earliest match

        val ruleConsumptionLog = StringBuilder("(unmatched{|}consumed): (")
        if(earliestMatchingResult!!.range.start > 0) {
            val unmatchedOutput = onNoRuleMatch(processingWord, earliestMatchingResult.range.start)
            i += unmatchedOutput.indexAdvance
            out = unmatchedOutput.output(out)
            //println("already run rules before emptying: ${alreadyRunRules.size}: $alreadyRunRules")
            alreadyRunRules.removeAll { true }
/*            println("matched |${earliestMatchingResult.groupValues}| near: |${processingWord.subSequence(
                    0,
                    kotlin.math.min(16, processingWord.length)
            )}|\ninput: ${processingWord.length}; consumed: ${consumed.length}; output: ${out.length}")*/
            println("\tunmatched chars skipped (${earliestMatchingResult!!.range.start - 1}):\n\"\"\"\n"+
                    processingWord.substring(0, earliestMatchingResult!!.range.start)+"\n\"\"\"")
        }

        out = earliestMatchingRule.outputString(out, earliestMatchingResult.groups)
        //number of letters consumed is the match length, unless explicitly specified
        val actualLettersConsumed = earliestMatchingRule.lettersConsumed?.invoke(earliestMatchingResult.groups) ?: earliestMatchingResult.value.length
        if(actualLettersConsumed > 0) {
            i += actualLettersConsumed
            println("\tchars consumed ($actualLettersConsumed): \n\"\"\"\n"+processingWord.substring(0, actualLettersConsumed)+"\n\"\"\"")
            println("\t replacement: $earliestMatchingRule.")
            alreadyRunRules.removeAll { true }
        } else {//no characters were consumed
            //add the just-processed rule to the list of rules that have already been run, so we know not to run it again on the same input
            alreadyRunRules.add(earliestMatchingRule)
        }
    }
    //System.out.println("consumed: $consumed")
    return out
}