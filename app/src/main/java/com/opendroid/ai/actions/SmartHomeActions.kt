package com.opendroid.ai.actions

import android.content.Context
import com.opendroid.ai.actions.base.Action
import com.opendroid.ai.actions.base.ActionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart-home action boundary.
 *
 * These actions intentionally fail closed until a real, authenticated controller adapter
 * (for example Home Assistant or MQTT) is configured. Opening Google Home or a web search
 * does not execute or verify a device command, so it must never be reported as success.
 */
@Singleton
class SmartHomeActions @Inject constructor() {

    fun getActions(): List<Action> = listOf(
        GeneralSmartHomeAction(),
        ToggleLightAction(),
        SetThermostatAction(),
        LockDoorAction()
    )

    private abstract class UnconfiguredSmartHomeAction : Action {
        protected fun notConnected(): ActionResult = ActionResult.Failure(
            errorMsg = "Smart-home control is not connected yet. Connect and authorize a supported Home Assistant or MQTT controller before asking NOVA to operate devices."
        )

        override suspend fun execute(
            params: Map<String, String>,
            context: Context
        ): ActionResult = notConnected()
    }

    private class GeneralSmartHomeAction : UnconfiguredSmartHomeAction() {
        override val name: String = "SMART_HOME"
    }

    private class ToggleLightAction : UnconfiguredSmartHomeAction() {
        override val name: String = "TOGGLE_LIGHT"
    }

    private class SetThermostatAction : UnconfiguredSmartHomeAction() {
        override val name: String = "SET_THERMOSTAT"

        override suspend fun execute(
            params: Map<String, String>,
            context: Context
        ): ActionResult {
            if (params["temperature"].isNullOrBlank()) {
                return ActionResult.Failure("temperature parameter is missing")
            }
            return notConnected()
        }
    }

    private class LockDoorAction : UnconfiguredSmartHomeAction() {
        override val name: String = "LOCK_DOOR"
    }
}
