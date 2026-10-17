package com.gurnoor.locationchanger.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSetupEnvironment(
    private val write: Boolean,
    private val mock: Boolean,
    private val location: Boolean,
) : SetupEnvironment {
    override fun hasWriteSecureSettings() = write
    override fun isMockLocationAllowed() = mock
    override fun hasLocationPermission() = location
}

class SetupCheckerTest {

    @Test
    fun allGranted_isReadyToMock() {
        val status = SetupChecker(FakeSetupEnvironment(write = true, mock = true, location = true)).check()
        assertTrue(status.adbSetupComplete)
        assertTrue(status.readyToMock)
    }

    @Test
    fun missingWriteSecureSettings_adbSetupIncomplete() {
        val status = SetupChecker(FakeSetupEnvironment(write = false, mock = true, location = true)).check()
        assertFalse(status.adbSetupComplete)
        assertFalse(status.readyToMock)
    }

    @Test
    fun missingMockAppOp_adbSetupIncomplete() {
        val status = SetupChecker(FakeSetupEnvironment(write = true, mock = false, location = true)).check()
        assertFalse(status.adbSetupComplete)
        assertFalse(status.readyToMock)
    }

    @Test
    fun adbDoneButNoLocationPermission_setupCompleteButNotReady() {
        // The setup screen (for the son) should pass; the father still gets a normal permission prompt.
        val status = SetupChecker(FakeSetupEnvironment(write = true, mock = true, location = false)).check()
        assertTrue(status.adbSetupComplete)
        assertFalse(status.readyToMock)
    }
}
