package com.github.medavox.pytokot

import com.github.medavox.pytokot.Pytokot.stringRules
import com.github.medavox.transcribers.BaseRule
import com.github.medavox.transcribers.RuleBasedTranscriber.*
import kotlin.math.max


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

fun BaseRule.asString(): String {
    //this.outputString.
    return "BaseRule("+(if(consumedMatcher==null) "" else "consumedMatcher=\"$consumedMatcher\", ")+
            "unconsumedMatcher=$unconsumedMatcher, " +
            "outputString=${outputString}, lettersConsumed=$lettersConsumed)"
}

//fixme: choosing the next earliest rule to match is not the same as choosing the first matching rule in the list
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
        //val braceCloseRule = rules().firstOrNull{ it.label == "closing braces"}
        //if(i > 0 && this[i-1] == '\n' && braceCloseRule != null) println("closing brace consumed matches: "+braceCloseRule.consumedMatcher)
        //println("already run rules: "+alreadyRunRules.size)
        //use some fancy collections lambda function to only call .find() and create a MatchResult once
        val hull:List<Pair<BaseRule, MatchResult?>> = (rules() - alreadyRunRules).
            //run the unconsumed regex on every rule
            map { rule: BaseRule -> Pair<BaseRule, MatchResult?>(rule, rule.unconsumedMatcher.find(this, i)) }.
            filter { (rule, result) -> result != null }.//filter out rules whose unconsumedMatcher don't match
            filter { (rule, result) ->
                val consumed = this.substring(0, i + result!!.value.length)
                val doesMatch = rule.consumedMatcher == null ||
                        rule.consumedMatcher?.findAll(consumed)?.lastOrNull()?.range?.endInclusive == result.range.start-1
                if(rule.label == "closing braces") {
                    rule.consumedMatcher?.findAll(this.substring(0, result.range.start))?.let {
                        println("\tmatches: "+it.toList().size)
                        println("\ti:$i")
                        println("\tresult.range.start:${result.range.start}")
                        println("\tresult.value.length:${result.value.length}")
                        println(it.fold("") { acc, elem:MatchResult ->
                            ""+elem.range.endInclusive
                        })
                    }
                }
                doesMatch
                // if the rule's consumedMatcher is null, that counts as matching:
                //rules that don't specify a consumedMatcher aren't checked against it
                    //if it has been specified by this rule, it has to match at the end of the already-consumed string
            }.//filter out rules that don't match
            sortedBy { (rule, result) -> result!!.range.start }//sort by earliest match
                //need to get the list of rules after each rule!
        //(earliestMatchingRules, earliestMatchingResults)
        //no rules matched anywhere, at all
        //(no rules match before the end of the remaining input)
        //this means the rest of the input "doesn't match"

        //so call the lambda on the remaining string
        if(hull.isEmpty()) return onNoRuleMatch(processingWord, processingWord.length).output(out) //else, if it's null: //else, if it's null:

        val earliestMatchingRule:BaseRule  = hull[0].first//hu.firstOrNull()?:return
        val earliestMatchingResult:MatchResult =  hull[0].second!!
        //.firstOrNull()?:return onNoRuleMatch(processingWord, processingWord.length).output(out) //else, if it's null:
        //last 8 consumed chars

        val possibleRules:String = hull.fold("", { acc:String, elem:Pair<BaseRule, MatchResult?> ->
            acc+"\n\t\t\"${elem.first.label}\" after ${elem.second!!.range.start} skipped chars"
        })
        //the brace closer doesn't match before the next normal rule does (because its \n match is in its consumed), so it might always be 2nd or whatever
        println("\nUSED RULE \"${earliestMatchingRule.label}\" from ${rules().size} rules, ${hull.size} matching")/* to consume: |${processingWord.subSequence(
            0, kotlin.math.min(16, processingWord.length)
        )}|\n")*/
        println("\tlast 8 consumed chars:"+this.substring(max(i-8, 0), i).esc())

        //there are actually 2 separate input-consumption steps here, one after the other:
        //1. process any non-matching chars which precede the rule match by passing it to the provided function,
        //2. consume the input that the matching rule actually matched

        //println("rule: $earliestMatchingRule ; result: ${earliestMatchingResult?.groupValues}")
        //call the lambda on any unmatched characters before the earliest match

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
            println("\t${earliestMatchingResult.range.start - 1} unmatched chars skipped before match:"+processingWord.substring(0, earliestMatchingResult.range.start).esc())
        }

        out = earliestMatchingRule.outputString(out, earliestMatchingResult.groups)
        //number of letters consumed is the match length, unless explicitly specified
        val actualLettersConsumed = earliestMatchingRule.lettersConsumed?.invoke(earliestMatchingResult.groups) ?: earliestMatchingResult.value.length
        if(actualLettersConsumed > 0) {
            i += actualLettersConsumed
            println("\t$actualLettersConsumed chars consumed: "+processingWord.substring(0, actualLettersConsumed).esc())
//            println("\t replacement: $earliestMatchingRule.")
            alreadyRunRules.removeAll { true }
        } else {//no characters were consumed
            //add the just-processed rule to the list of rules that have already been run, so we know not to run it again on the same input
            alreadyRunRules.add(earliestMatchingRule)
        }
    }
    //System.out.println("consumed: $consumed")
    return out
}

fun String.esc():String = this.replace("\n", "\\n")