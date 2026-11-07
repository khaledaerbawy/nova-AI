package com.opendroid.ai.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.opendroid.ai.core.agent.AgentLoop
import com.opendroid.ai.core.agent.AgentState
import com.opendroid.ai.core.agent.PlanApprovalSummary
import com.opendroid.ai.core.voice.ApprovalListenGate
import com.opendroid.ai.core.voice.SpeechRecognitionEngine
import com.opendroid.ai.core.voice.TextToSpeechEngine
import com.opendroid.ai.core.voice.VoiceApprovalIntent
import com.opendroid.ai.core.voice.PendingApprovalListen
import com.opendroid.ai.core.voice.VoiceApprovalParser
import com.opendroid.ai.core.voice.WakeWordDetector
import com.opendroid.ai.data.models.Plan
import com.opendroid.ai.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class OpenDroidService : Service() {

    @Inject
    lateinit var agentLoop: AgentLoop

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var mcpServer: McpServer

    private lateinit var wakeWordDetector: WakeWordDetector
    private lateinit var speechRecognitionEngine: SpeechRecognitionEngine
    private lateinit var textToSpeechEngine: TextToSpeechEngine

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeWordEnabled = true
    private val pendingApprovalListen = PendingApprovalListen()
    private val approvalListenGate = ApprovalListenGate()
    private var foregroundStarted = false
    private var microphoneForegroundStarted = false
    private var voiceStatus = "جاري تجهيز محرك الاستيقاظ المحلي"

    companion object {
        const val ACTION_TRIGGER_RECORD = "com.opendroid.ai.action.TRIGGER_RECORD"
        private const val ACTION_USER_START = "com.opendroid.ai.action.USER_START"
        private const val CHANNEL_ID = "opendroid_channel"
        private const val NOTIFICATION_ID = 2024
        
        /** Called from a visible app interaction; may promote the service to microphone FGS. */
        fun start(context: Context) {
            val intent = Intent(context, OpenDroidService::class.java).setAction(ACTION_USER_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OpenDroidService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Start Foreground Notification. A refused start (boot on Android 15, or a
        // sticky restart from the background) stops the service instead of crashing
        // the process; MainActivity and the Navigation ON_RESUME hook start it again.
        createNotificationChannel()
        foregroundStarted = startForegroundCompat()
        if (!foregroundStarted) {
            stopSelf()
            return
        }

        // Initialize engines
        wakeWordDetector = WakeWordDetector(this) { status ->
            voiceStatus = status
            if (foregroundStarted) updateVoiceNotification()
        }
        speechRecognitionEngine = SpeechRecognitionEngine(this)
        textToSpeechEngine = TextToSpeechEngine(this, settingsRepository)

        // Bind Agent Loop TTS
        agentLoop.onSpeakCallback = { text ->
            textToSpeechEngine.speak(text)
        }
        // Stop and Clear cancel the task; speech already playing or still being fetched
        // must stop with it.
        agentLoop.onStopSpeakingCallback = {
            approvalListenGate.disarm()
            textToSpeechEngine.stop()
        }

        // Set TTS completion listener to transition back to Idle
        textToSpeechEngine.onCompletionListener = { utteranceId ->
            // Only Speaking resets to Idle - speech that happens to play while a
            // plan sits in PlanProposed must not knock the approval gate over.
            if (agentLoop.agentState.value is AgentState.Speaking) {
                agentLoop.setAgentState(AgentState.Idle)
            }
            if (approvalListenGate.onUtteranceDone(utteranceId)) {
                pendingApprovalListen.take()?.let { planId -> startListeningForApproval(planId) }
            }
        }

        mcpServer.start()

        // Keep the always-on wake-word preference independent from the optional floating button.
        serviceScope.launch {
            settingsRepository.llmConfig.collectLatest { config ->
                wakeWordEnabled = config.wakeWordEnabled
                if (!wakeWordEnabled || !microphoneForegroundStarted) {
                    wakeWordDetector.stopListening()
                } else {
                    startWakeWordDetection()
                }
            }
        }

        // Hands-free plan approval (upstream issue 18 spec, voice section):
        // speak the prompt once per proposal; auto-listen for the reply only in
        // wake-word mode, where the user is known to be voice-driven. Keyed on the
        // whole plan: a re-evaluation can propose the same planId again mid-run,
        // and that second proposal must not wait in silence.
        serviceScope.launch {
            var promptedPlan: Plan? = null
            agentLoop.agentState.collectLatest { state ->
                pendingApprovalListen.onAgentState(state)
                if (state is AgentState.PlanProposed && state.plan != promptedPlan) {
                    promptedPlan = state.plan
                    // Armed with the prompt's own id before it is queued, so only the
                    // prompt finishing opens the mic, never an earlier utterance.
                    val promptId = UUID.randomUUID().toString()
                    if (wakeWordEnabled) {
                        pendingApprovalListen.arm(state.plan.planId)
                        approvalListenGate.armFor(promptId)
                    } else {
                        approvalListenGate.disarm()
                    }
                    textToSpeechEngine.speak(PlanApprovalSummary.spokenPrompt(state.plan), utteranceId = promptId)
                }
            }
        }
    }

    private fun startForegroundCompat(): Boolean {
        val notification = createNotification()
        val micGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val types = ForegroundStart.typesFor(android.os.Build.VERSION.SDK_INT, micGranted)
        return ForegroundStart.startFirstAllowed(types) { type ->
            if (type == null) {
                startForeground(NOTIFICATION_ID, notification)
                microphoneForegroundStarted = false
            } else {
                androidx.core.app.ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
                microphoneForegroundStarted =
                    type == android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
        }
    }

    /**
     * Promote a boot-recovered special-use service only after a visible user
     * interaction. Android 14+ applies while-in-use checks to microphone FGS.
     */
    private fun promoteToMicrophoneForeground(): Boolean {
        if (microphoneForegroundStarted) return true
        val micGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!micGranted) return false
        return try {
            val notification = createNotification()
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                androidx.core.app.ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            microphoneForegroundStarted = true
            updateVoiceNotification()
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    private fun updateVoiceNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun startWakeWordDetection() {
        if (!microphoneForegroundStarted || !wakeWordEnabled) return
        wakeWordDetector.startListening {
            // Wake word detected! Prompt user with a sound or greeting and start listing for query
            textToSpeechEngine.speak("أيوه، معاك.")
            startListeningForQuery()
        }
    }

    private fun startListeningForQuery() {
        if (!microphoneForegroundStarted) {
            agentLoop.setAgentState(AgentState.Idle)
            updateVoiceNotification()
            return
        }
        // Temporarily pause wake word to avoid hearing itself
        wakeWordDetector.stopListening()

        // Set agent state to Listening
        agentLoop.setAgentState(AgentState.Listening)

        // Start speech recognizer for query input
        speechRecognitionEngine.startListening(
            onResult = { query ->
                agentLoop.processQuery(query, this)
                // Resume wake word detection only if floating button is disabled
                if (wakeWordEnabled) {
                    wakeWordDetector.startListening {
                        textToSpeechEngine.speak("أيوه، معاك.")
                        startListeningForQuery()
                    }
                } else {
                    agentLoop.setAgentState(AgentState.Idle)
                }
            },
            onError = { _ ->
                agentLoop.setAgentState(AgentState.Idle)
                // Resume wake word detection only if floating button is disabled
                if (!showFloatingButton) {
                    wakeWordDetector.startListening {
                        textToSpeechEngine.speak("أيوه، معاك.")
                        startListeningForQuery()
                    }
                }
            }
        )
    }

    /**
     * One-shot reply capture after the spoken approval prompt. Unrecognized
     * speech (or an error) is a deliberate no-op: the plan stays in
     * PlanProposed with the visual modal still on screen. Never a grant path -
     * nothing spoken may widen the allowlist.
     */
    private fun startListeningForApproval(planId: String) {
        if (!microphoneForegroundStarted) return
        wakeWordDetector.stopListening()
        speechRecognitionEngine.startListening(
            onResult = { utterance ->
                when (VoiceApprovalParser.parse(utterance)) {
                    VoiceApprovalIntent.APPROVE -> agentLoop.approveProposedPlan(this, planId)
                    VoiceApprovalIntent.REJECT -> agentLoop.rejectProposedPlan()
                    VoiceApprovalIntent.NONE -> Unit
                }
                if (wakeWordEnabled) startWakeWordDetection()
            },
            onError = { _ ->
                if (!showFloatingButton) startWakeWordDetection()
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The service is already stopping; a sticky restart would only be refused again.
        if (!foregroundStarted) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_USER_START -> {
                if (promoteToMicrophoneForeground()) {
                    if (!showFloatingButton) startWakeWordDetection()
                } else {
                    updateVoiceNotification()
                }
            }
            ACTION_TRIGGER_RECORD -> {
                if (promoteToMicrophoneForeground()) startListeningForQuery()
                else updateVoiceNotification()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (!foregroundStarted) return
        mcpServer.stop()
        wakeWordDetector.destroy()
        speechRecognitionEngine.destroy()
        textToSpeechEngine.destroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "NOVA Assistant Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps NOVA voice assistant available"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NOVA Active")
            .setContentText(
                if (microphoneForegroundStarted && wakeWordEnabled) {
                    voiceStatus
                } else if (microphoneForegroundStarted) {
                    "NOVA نشط — اضغط زر الميكروفون للتحدث"
                } else {
                    "افتح NOVA لتفعيل الميكروفون بعد إعادة تشغيل الهاتف"
                }
            )
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }
}
