package com.github.medavox.pytokot

actual object err {
    actual fun print(err: String) = System.err.print(err)
    actual fun println(err: String)  = System.err.println(err)
}
