package com.opendroid.ai.actions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.opendroid.ai.actions.base.ActionResult
import com.opendroid.ai.core.agent.ActionRisk
import com.opendroid.ai.core.agent.ActionSchema
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SmartHomeActionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val actions = SmartHomeActions().getActions().associateBy { it.name }

    @Test
    fun `smart home actions are registered`() {
        assertEquals(
            setOf("SMART_HOME", "TOGGLE_LIGHT", "SET_THERMOSTAT", "LOCK_DOOR"),
            actions.keys
        )
    }

    @Test
    fun `unconfigured smart home actions fail honestly instead of pretending to control devices`() = runBlocking {
        val cases = listOf(
            "SMART_HOME" to mapOf("device" to "living room fan", "action" to "toggle"),
            "TOGGLE_LIGHT" to mapOf("room" to "bedroom", "on" to "true"),
            "SET_THERMOSTAT" to mapOf("temperature" to "22"),
            "LOCK_DOOR" to mapOf("location" to "front door")
        )

        cases.forEach { (name, params) ->
            val result = actions.getValue(name).execute(params, context)

            assertTrue("$name should return an explicit failure", result is ActionResult.Failure)
            assertFalse("$name must not report success without a connected controller", result.success)
            assertTrue(
                "$name should explain the missing integration",
                result.error.orEmpty().contains("Home Assistant or MQTT")
            )
        }
    }

    @Test
    fun `set thermostat reports missing temperature before checking integration`() = runBlocking {
        val result = actions.getValue("SET_THERMOSTAT").execute(emptyMap(), context)

        assertTrue(result is ActionResult.Failure)
        assertFalse(result.success)
        assertEquals("temperature parameter is missing", result.error)
    }

    @Test
    fun `lock door remains behind the irreversible approval boundary`() {
        val definition = ActionSchema.getAction("LOCK_DOOR")

        assertNotNull(definition)
        assertTrue(definition!!.neverAutoApprove)
        assertEquals(ActionRisk.IRREVERSIBLE, ActionSchema.riskForAction("LOCK_DOOR"))
    }
}
