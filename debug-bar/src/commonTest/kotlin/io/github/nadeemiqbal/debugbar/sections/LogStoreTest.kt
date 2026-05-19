package io.github.nadeemiqbal.debugbar.sections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LogStoreTest {

    @Test
    fun freshStore_isEmpty() {
        val store = LogStore()
        assertTrue(store.entries.value.isEmpty())
    }

    @Test
    fun record_storesFields() {
        val store = LogStore()
        store.record(LogLevel.Warn, "something happened", tag = "Auth", throwable = "java.lang.X: ...")
        val e = store.entries.value.single()
        assertEquals(LogLevel.Warn, e.level)
        assertEquals("something happened", e.message)
        assertEquals("Auth", e.tag)
        assertEquals("java.lang.X: ...", e.throwable)
    }

    @Test
    fun record_respectsMaxEntries() {
        val store = LogStore(maxEntries = 4)
        repeat(10) { i -> store.record(LogLevel.Info, "msg $i") }
        val entries = store.entries.value
        assertEquals(4, entries.size)
        assertEquals("msg 6", entries[0].message) // oldest 6 dropped
        assertEquals("msg 9", entries[3].message)
    }

    @Test
    fun clear_emptiesEntries() {
        val store = LogStore()
        store.record(LogLevel.Error, "err")
        store.clear()
        assertTrue(store.entries.value.isEmpty())
    }
}
