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
    //todo: use a StringBuilder for output instead of replacing the immutable string instance with a newly concatted one
    var out:String = ""
    var processingWord:String = this
    var consumed = ""
    loop@ while(processingWord.isNotEmpty()) {
        //uses the first rule which matches -- so rule order matters
        val matchingRuleResultPairs:List<Pair<BaseRule, MatchResult>> = rules().
            map{rule:BaseRule -> Pair<BaseRule, MatchResult?>(rule, rule.unconsumedMatcher.find(processingWord))}.
            filter {  (rule, result) -> result != null }.//filter out rules that don't match
            sortedBy { (rule, result) -> result!!.range.start }.//sort by earliest match
                    //filter out rules whose consumedMatcher don't 'match':
            filter { (rule, result) -> rule.consumedMatcher == null ||// if the rule's consumedMatcher is null,
                    // that counts as matching:
                    //rules that don't specify a consumedMatcher aren't checked against it
                    //if it has been specified by this rule, it has to match at the end of the already-consumed string
                    (rule.consumedMatcher as Regex).findAll(consumed).lastOrNull()?.range?.endInclusive == consumed.length-1
            }.map { Pair(it.first, it.second!!) }//de-nulltype the match result


        //else, if it's null:
                //no rules matched anywhere, at all
                //(no rules match before the end of the remaining input)
                //this means the rest of the input "doesn't match"

                //so call the lambda on the remaining string
        if(matchingRuleResultPairs.isEmpty()) {
            return onNoRuleMatch(processingWord, processingWord.length).output(out)
        }
/*        if(Pytokot.wasInsideString || earliestMatchingRule in stringRules) {
            println(if(Pytokot.wasInsideString){"END  "}else{"START"}+ ": |${processingWord.subSequence(
                    0,
                    kotlin.math.min(16, processingWord.length)
            )}|\n")
        }*/
        var i = 0
        //gotta update the stored matched of all the other rules too, once we've executed a rule
        var newMatchResult = matchingRuleResultPairs[i].second
        while(true) {
            println("i:$i")
            val (rule, _) = matchingRuleResultPairs[i]
            //call the lambda on any unmatched characters before the earliest match
            if(newMatchResult.range.first > 0) {
                val unmatchedOutput = onNoRuleMatch(processingWord, newMatchResult.range.start)
                processingWord = unmatchedOutput.newWorkingInput
                consumed += unmatchedOutput.newConsumed
                out = unmatchedOutput.output(out)
            }
            //add the rule's replacement
            out = rule.outputString(out, newMatchResult.groups)
            //number of letters consumed is the match length, unless explicitly specified
            val actualLettersConsumed = rule.lettersConsumed?.invoke(newMatchResult.groups) ?: newMatchResult.value.length
            println("actual letters consumed: $actualLettersConsumed")
            consumed += processingWord.substring(0, actualLettersConsumed)
            processingWord = processingWord.substring(actualLettersConsumed)
            //keep processing rules until we get one whose actualLettersConsumed is > 0
            //otherwise we get stuck in a loop reprocessing this same zero-consuming rule forever
           if(rule.lettersConsumed?.invoke(newMatchResult.groups) ?: newMatchResult.value.length == 0 && i < matchingRuleResultPairs.size) {
               break
           }else {
               i++
               //recalculate the match result for the next rule,
               //as this one may have changed processingWord
               newMatchResult = matchingRuleResultPairs[i].first.
           }
        }

        println("output length: ${out.length}")
        //println("processingWord length: ${out.length}")
    }
    //System.out.println("consumed: $consumed")
    return out
}
fun <T> fur(initializer: () -> T,
            loopCheck:(T) -> Boolean,
            update:(T) -> T,
            loopBody:(T) -> Unit
) {
    var index:T = initializer()
    while(loopCheck(index)) {
        loopBody(index)
        index = update(index)
    }
}