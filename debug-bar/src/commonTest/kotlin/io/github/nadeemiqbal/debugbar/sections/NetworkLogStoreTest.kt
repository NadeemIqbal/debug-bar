package io.github.nadeemiqbal.debugbar.sections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkLogStoreTest {

    @Test
    fun freshStore_isEmpty() {
        val store = NetworkLogStore()
        assertTrue(store.entries.value.isEmpty())
    }

    @Test
    fun record_addsEntryAtTail() {
        val store = NetworkLogStore()
        store.record("GET", "https://x.com/api", 200, 120L)
        val entries = store.entries.value
        assertEquals(1, entries.size)
        assertEquals("GET", entries[0].method)
        assertEquals(200, entries[0].statusCode)
        assertEquals(120L, entries[0].durationMs)
    }

    @Test
    fun record_preservesOrder() {
        val store = NetworkLogStore()
        store.record("GET", "https://x.com/1", 200, 10L)
        store.record("POST", "https://x.com/2", 201, 20L)
        store.record("DELETE", "https://x.com/3", 204, 5L)
        val entries = store.entries.value
        assertEquals(3, entries.size)
        assertEquals("https://x.com/1", entries[0].url)
        assertEquals("https://x.com/2", entries[1].url)
        assertEquals("https://x.com/3", entries[2].url)
    }

    @Test
    fun record_respectsMaxEntries() {
        val store = NetworkLogStore(maxEntries = 3)
        repeat(5) { i -> store.record("GET", "https://x.com/$i", 200, i.toLong()) }
        val entries = store.entries.value
        assertEquals(3, entries.size)
        // Oldest 2 dropped, newest 3 retained.
        assertEquals("https://x.com/2", entries[0].url)
        assertEquals("https://x.com/4", entries[2].url)
    }

    @Test
    fun clear_emptiesEntries() {
        val store = NetworkLogStore()
        store.record("GET", "https://x.com/1", 200, 10L)
        store.record("GET", "https://x.com/2", 200, 20L)
        store.clear()
        assertTrue(store.entries.value.isEmpty())
    }

    @Test
    fun record_idsAreMonotonic() {
        val store = NetworkLogStore()
        store.record("GET", "url1", 200, 1L)
        store.record("GET", "url2", 200, 1L)
        store.record("GET", "url3", 200, 1L)
        val ids = store.entries.value.map { it.id }
        assertEquals(ids.sorted(), ids)
        assertEquals(ids.toSet().size, ids.size, "ids must be unique")
    }
}
