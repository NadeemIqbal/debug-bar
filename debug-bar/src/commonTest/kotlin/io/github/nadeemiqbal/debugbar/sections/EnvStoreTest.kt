package io.github.nadeemiqbal.debugbar.sections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvStoreTest {

    private val envs = listOf(
        EnvEntry("dev", "https://dev-api.example.com"),
        EnvEntry("staging", "https://staging-api.example.com"),
        EnvEntry("prod", "https://api.example.com"),
    )

    @Test
    fun initialName_resolvesToMatchingEntry() {
        val store = EnvStore(envs = envs, initialName = "staging")
        assertEquals("staging", store.selected.value.name)
    }

    @Test
    fun initialName_unknownFallsBackToFirst() {
        val store = EnvStore(envs = envs, initialName = "nonexistent")
        assertEquals("dev", store.selected.value.name)
    }

    @Test
    fun select_changesSelected() {
        val store = EnvStore(envs = envs)
        store.select("prod")
        assertEquals("prod", store.selected.value.name)
        assertEquals("https://api.example.com", store.selected.value.baseUrl)
    }

    @Test
    fun select_unknownNameIsNoOp() {
        val store = EnvStore(envs = envs, initialName = "dev")
        store.select("doesnotexist")
        assertEquals("dev", store.selected.value.name)
    }

    @Test
    fun select_firesOnSelectedCallback() {
        var lastCallback: EnvEntry? = null
        val store = EnvStore(envs = envs, onSelected = { lastCallback = it })
        store.select("staging")
        assertEquals("staging", lastCallback?.name)
    }

    @Test
    fun emptyEnvList_throws() {
        assertFailsWith<IllegalArgumentException> { EnvStore(envs = emptyList()) }
    }
}
