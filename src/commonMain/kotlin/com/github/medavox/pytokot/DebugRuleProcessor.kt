package com.github.medavox.pytokot

import com.github.medavox.pytokot.Pytokot.jeff
import com.github.medavox.transcribers.BaseRule
import com.github.medavox.transcribers.RuleBasedTranscriber


fun String.processDalaiWithRules(rules:List<BaseRule>, onNoRuleMatch:(unmatched:String) -> RuleBasedTranscriber.UnmatchedOutput) : String =
    this.processDalaiWithRules({rules}, onNoRuleMatch)
fun String.processDalaiWithRules(rules:()->List<BaseRule>, onNoRuleMatch:(unmatched:String) -> RuleBasedTranscriber.UnmatchedOutput) : String {
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
                if(Pytokot.insideString || rule == jeff) {
                    println(
                        "|${unconsumedMatch.groupValues[0]}| near |${processingWord.subSequence(
                            0,
                            kotlin.math.min(16, processingWord.length)
                        )}| "+if(Pytokot.insideString) "STARTS STRING" else "ENDS STRING")
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
        processingWord = unmatchedOutput.newWorkingInput
        consumed += unmatchedOutput.newConsumed
        out = unmatchedOutput.output(out)
    }
    //System.out.println("consumed: $consumed")
    return out
}