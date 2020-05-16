package com.github.medavox.pytokot

/**These are added to converted code, to smooth some trickier differences between Python and Kotlin syntax.*/
class Shims {
    private val enabledShims = mutableSetOf<Keys>()
    fun enable(key:Keys) {
        enabledShims.add(key)
    }
    fun disable(key:Keys) {
        enabledShims.remove(key)
    }
    enum class Keys {
        stringSize,
        slices
    }
    private val shimsToInclude = mapOf<Keys, String> (
        Keys.stringSize to "private val String.size:Int get() = this.length",
        Keys.slices to
"""/**Allows the use of Python-like negative indices, which go backwards from the end of a [String].
 * This function is for selecting substrings by `string.get(0, -1)` or `string[0, -1]`,
 *  rather than the bulkier Kotlin `string[0, string.length-1]`.
 *  Also casts the resultant [Char] to a [String].
 *  The s stands for slice.*/
private fun String.s(start:Int?, end:Int?):String {
    val actualStart = when {
        start == null -> 0
        start >= 0 -> start
        else -> length-start
    }
    val actualEnd = when {
        end == null -> length
        end >= 0 -> end
        else -> length-end
    }
    return this.substring(actualStart, actualEnd)
}
//slice to ranges (the until syntax)
/**Allows the use of Python-like negative indices, which go backwards from the end of a [String].
 * This function is for getting a character from a [String] by `string.ket(-1)`,
 *  rather than the bulkier Kotlin `string[string.length-1]`.
 *  Also casts the resultant [Char] to a [String], for convenience.
 *  The s stands for slice.*/
private fun String.s(index:Int):String {
    val actualIndex = if(index >= 0) index else this.length-index
    return this[actualIndex].toString()
}
"""
    )
    fun getNeededShims():String {
        return enabledShims.fold("\n\n//-----------SHIMS-----------") {acc, key ->
            acc+shimsToInclude[key]+"\n\n"
        }
    }
}

/**Allows the use of Python-like negative indices, which go backwards from the end of a [String].
 * This function is for selecting substrings by `string.get(0, -1)` or `string[0, -1]`,
 *  rather than the bulkier Kotlin `string[0, string.length-1]`.
 *  Also casts the resultant [Char] to a [String].
 *  The s stands for slice.*/
private fun String.s(start:Int?, end:Int?, step:Int?=null):String {
    var reversed = false
    val actualStep = when {
        step == null -> 1
        step < 0 -> {
            reversed = true
            step * -1
        }
        else -> step
    }
    val actualStart = when {
        start == null -> 0
        start >= 0 -> start
        else -> length-start
    }
    val actualEnd = when {
        end == null -> length
        end >= 0 -> end
        else -> length-end
    }
    return this.slice(actualStart until actualEnd step actualStep).run{ if(reversed) reversed() else this }
}
//slice to ranges (the until syntax)
/**Allows the use of Python-like negative indices, which go backwards from the end of a [String].
 * This function is for getting a character from a [String] by `string.ket(-1)`,
 *  rather than the bulkier Kotlin `string[string.length-1]`.
 *  Also casts the resultant [Char] to a [String], for convenience.
 *  The s stands for slice.*/
private fun String.s(index:Int):String {
    val actualIndex = if(index >= 0) index else this.length-index
    return this[actualIndex].toString()
}