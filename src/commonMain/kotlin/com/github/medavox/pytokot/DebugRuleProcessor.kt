package com.github.medavox.pytokot

import com.github.medavox.pytokot.Pytokot.stringRules
import com.github.medavox.transcribers.BaseRule
import com.github.medavox.transcribers.RuleBasedTranscriber
import com.github.medavox.transcribers.RuleBasedTranscriber.*


fun String.processDalaiWithRules(rules:List<BaseRule>, onNoRuleMatch:(unmatched:String) -> UnmatchedOutput) : String =
    this.processDalaiWithRules({rules}, onNoRuleMatch)
fun String.processDalaiWithRules(rules:()->List<BaseRule>, onNoRuleMatch:(unmatched:String) -> UnmatchedOutput) : String {
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
        processingWord = unmatchedOutput.newWorkingInput
        consumed += unmatchedOutput.newConsumed
        out = unmatchedOutput.output(out)
    }
    //System.out.println("consumed: $consumed")
    return out
}

fun newCopy(remainingInput:String, unmatchedChars:Int): UnmatchedOutput {
    val unmatched = remainingInput.substring(0, unmatchedChars)
    return RuleBasedTranscriber.UnmatchedOutput(newWorkingInput = remainingInput.substring(unmatchedChars - 1),
            newConsumed = unmatched,
            output = unmatched
    )
}

fun String.processFasterWithRules(rules:List<BaseRule>, onNoRuleMatch:(remainingInput:String, unmatchedChars:Int) -> UnmatchedOutput
) : String = this.processFasterWithRules({rules}, onNoRuleMatch)
fun String.processFasterWithRules(rules:()->List<BaseRule>,
                                  onNoRuleMatch:(remainingInput:String, unmatchedChars:Int) -> UnmatchedOutput
) : String {
    var out:String = ""
    var processingWord:String = this
    var consumed = ""
    loop@ while(processingWord.isNotEmpty()) {
        //uses the first rule which matches -- so rule order matters
        //todo: use some fancy collections lambda function to only call .find() and create a MatchResult once
        val (earliestMatchingRule, earliestMatchingResult) = rules().
            map{rule:BaseRule -> Pair<BaseRule, MatchResult?>(rule, rule.unconsumedMatcher.find(processingWord))}.
            filter {  (rule, result) -> result != null }.//filter out rules that don't match
            sortedBy { (rule, result) -> result!!.range.start }.//sort by earliest match
                    //filter out rules whose consumedMatcher don't 'match':
            firstOrNull { (rule, result) -> rule.consumedMatcher == null ||// if the rule's consumedMatcher is null,
                    // that counts as matching:
                    //rules that don't specify a consumedMatcher aren't checked against it
                    //if it has been specified by this rule, it has to match at the end of the already-consumed string
                    (rule.consumedMatcher as Regex).findAll(consumed).lastOrNull()?.range?.endInclusive == consumed.length-1
            }?: //else, if it's null:
                //no rules matched anywhere, at all
                //(no rules match before the end of the remaining input)
                //this means the rest of the input "doesn't match"

                //so call the lambda on the remaining string
                return onNoRuleMatch(processingWord, processingWord.length).output(out)

        if(Pytokot.wasInsideString || earliestMatchingRule in stringRules) {
            println(if(Pytokot.wasInsideString){"END  "}else{"START"}+ ": |${processingWord.subSequence(
                    0,
                    kotlin.math.min(16, processingWord.length)
            )}|\n")
        }
        out = earliestMatchingRule.outputString(out, earliestMatchingResult!!.groups)
        //number of letters consumed is the match length, unless explicitly specified
        val actualLettersConsumed = earliestMatchingRule.lettersConsumed?.invoke(earliestMatchingResult.groups) ?: earliestMatchingResult.value.length
        if(actualLettersConsumed > 0) {
            consumed += processingWord.substring(0, actualLettersConsumed)
            processingWord = processingWord.substring(actualLettersConsumed)
            //continue@loop//fixme:is this actually necessary after all?
        }

        //call the lambda on any unmatched characters before the earliest match
        if(earliestMatchingResult.range.start > 0) {
            val unmatchedOutput = onNoRuleMatch(processingWord, earliestMatchingResult.range.start)
            processingWord = unmatchedOutput.newWorkingInput
            consumed += unmatchedOutput.newConsumed
            out = unmatchedOutput.output(out)
        }
    }
    //System.out.println("consumed: $consumed")
    return out
}