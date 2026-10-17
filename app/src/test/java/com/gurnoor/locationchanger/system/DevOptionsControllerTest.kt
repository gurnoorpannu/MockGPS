package com.gurnoor.locationchanger.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fakes the settings layer: an in-memory map of Global settings. */
private class FakeGlobalSettings(initial: Map<String, Int> = emptyMap()) : GlobalSettingsGateway {
    val values = HashMap(initial)
    var writes = 0
    override fun getInt(name: String, default: Int): Int = values[name] ?: default
    override fun putInt(name: String, value: Int) {
        values[name] = value
        writes++
    }
}

class DevOptionsControllerTest {

    private val key = DevOptionsController.KEY

    @Test
    fun isEnabled_readsGlobalSetting() {
        assertFalse(DevOptionsController(FakeGlobalSettings()).isEnabled())
        assertTrue(DevOptionsController(FakeGlobalSettings(mapOf(key to 1))).isEnabled())
        assertFalse(DevOptionsController(FakeGlobalSettings(mapOf(key to 0))).isEnabled())
    }

    @Test
    fun enable_whenOff_turnsOnAndReportsWeChangedIt() {
        val fake = FakeGlobalSettings()
        val controller = DevOptionsController(fake)

        val weEnabled = controller.enable()

        assertTrue(weEnabled)
        assertEquals(1, fake.values[key])
    }

    @Test
    fun enable_whenAlreadyOn_doesNothingAndReportsWeDidNotChangeIt() {
        val fake = FakeGlobalSettings(mapOf(key to 1))
        val controller = DevOptionsController(fake)

        val weEnabled = controller.enable()

        assertFalse(weEnabled)
        assertEquals(0, fake.writes) // never wrote, so we won't claim to have enabled it
    }

    @Test
    fun disableIfWeEnabled_turnsOffOnlyWhenWeEnabledIt() {
        val fake = FakeGlobalSettings(mapOf(key to 1))
        val controller = DevOptionsController(fake)

        controller.disableIfWeEnabled(weEnabled = false) // user had it on themselves
        assertEquals(1, fake.values[key])

        controller.disableIfWeEnabled(weEnabled = true)  // we had turned it on
        assertEquals(0, fake.values[key])
    }

    @Test
    fun fullCycle_leavesDevOptionsAsWeFoundIt() {
        // Start off. We enable it, then on stop we must put it back off.
        val fake = FakeGlobalSettings()
        val controller = DevOptionsController(fake)

        val weEnabled = controller.enable()
        assertTrue(weEnabled)
        assertTrue(controller.isEnabled())

        controller.disableIfWeEnabled(weEnabled)
        assertFalse(controller.isEnabled())
    }
}
