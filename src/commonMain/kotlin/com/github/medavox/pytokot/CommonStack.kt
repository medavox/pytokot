package com.github.medavox.pytokot

/**Syntactic sugar around a [List], so it can only be used as a Stack.
 * Useful in Kotlin Multiplatform, because common code can't use the JVM Stack implementation.*/
class CommonStack<E> : MutableCollection<E> {
    private val storage:MutableList<E> = mutableListOf()

    fun peek():E? = if(storage.isNotEmpty()) storage.get(storage.size-1) else null
    fun pop():E? {
        val ret = if(storage.isNotEmpty()) storage.get(storage.size-1) else null
        storage.removeAt(storage.size-1)
        return ret
    }
    fun push(element:E) {
        storage.add(element)
    }
    override val size: Int get() = storage.size
    override fun contains(element: E): Boolean  = storage.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = storage.containsAll(elements)
    override fun isEmpty(): Boolean = storage.isEmpty()
    override fun add(element: E): Boolean = storage.add(element)
    override fun addAll(elements: Collection<E>): Boolean = storage.addAll(elements)
    override fun clear() = storage.clear()
    override fun iterator(): MutableIterator<E> = storage.iterator()
    override fun remove(element: E): Boolean = storage.remove(element)
    override fun removeAll(elements: Collection<E>): Boolean = storage.removeAll(elements)
    override fun retainAll(elements: Collection<E>): Boolean = storage.retainAll(elements)
}
