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
        stringSize
    }
    private val shimsToInclude = mapOf<Keys, String> (
        Keys.stringSize to "private val String.size:Int get() = this.length"
    )
    fun getNeededShims():String {
        return enabledShims.fold("\n\n//-----------SHIMS-----------") {acc, key ->
            acc+shimsToInclude[key]+"\n\n"
        }
    }
}