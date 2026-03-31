package com.internal.wamessenger.core

import android.content.Context
import android.util.Log
import com.internal.wamessenger.accessibility.WhatsAppAccessibilityService
import com.internal.wamessenger.model.Contact
import com.internal.wamessenger.model.LogEntry
import com.internal.wamessenger.model.SendStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CampaignState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isStopped: Boolean = false,
    val currentIndex: Int = 0,
    val total: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val currentPhone: String = "",
    val currentName: String = "",
    val currentMessage: String = "",
    val statusMessage: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L
) {
    val progress: Float get() = if (total == 0) 0f else currentIndex.toFloat() / total
    val isComplete: Boolean get() = !isRunning && currentIndex >= total && total > 0
    val elapsedSeconds: Long get() {
        val end = if (endTime > 0) endTime else System.currentTimeMillis()
        return (end - startTime) / 1000
    }
}

class CampaignController(private val context: Context) {

    companion object {
        private const val TAG = "CampaignController"
        const val COOLDOWN_EVERY = 12
        const val COOLDOWN_DURATION_MS = 30_000L
        const val MIN_DELAY_MS = 3000L
        const val MAX_DELAY_MS = 7000L
        const val MESSAGE_OPEN_WAIT_MS = 2500L
        const val LONG_MESSAGE_EXTRA_WAIT_MS = 3000L
        const val LONG_MESSAGE_THRESHOLD = 500
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logger = MessageLogger(context)
    private val queueManager = QueueManager(context)

    private val _state = MutableStateFlow(CampaignState())
    val state: StateFlow<CampaignState> = _state.asStateFlow()

    private var campaignJob: Job? = null
    private var contacts: List<Contact> = emptyList()
    private var templates: List<String> = emptyList()
    private var testMode = false
    private var manualMode = false

    // ── Campaign Lifecycle ────────────────────────────────────────────────────

    fun startCampaign(
        contactList: List<Contact>,
        templateList: List<String>,
        isTestMode: Boolean,
        isManualMode: Boolean,
        resumeFromIndex: Int = 0
    ) {
        contacts = contactList
        templates = templateList
        testMode = isTestMode
        manualMode = isManualMode

        _state.value = CampaignState(
            isRunning = true,
            total = contactList.size,
            currentIndex = resumeFromIndex,
            startTime = System.currentTimeMillis()
        )

        scope.launch { queueManager.saveQueue(contactList, templateList, testMode, manualMode) }

        campaignJob = scope.launch { runCampaign(resumeFromIndex) }
    }

    fun pause() {
        _state.value = _state.value.copy(isPaused = true, statusMessage = "Paused")
        scope.launch { queueManager.setPaused(true) }
    }

    fun resume() {
        _state.value = _state.value.copy(isPaused = false, statusMessage = "Resuming...")
        scope.launch { queueManager.setPaused(false) }
    }

    fun stop() {
        _state.value = _state.value.copy(
            isStopped = true, isRunning = false,
            statusMessage = "Stopped", endTime = System.currentTimeMillis()
        )
        scope.launch { queueManager.setStopped(true) }
        campaignJob?.cancel()
    }

    // ── Core Loop ─────────────────────────────────────────────────────────────

    private suspend fun runCampaign(startIndex: Int) {
        var messagesSinceBreak = 0

        for (i in startIndex until contacts.size) {

            // Check for stop
            if (_state.value.isStopped) break

            // Wait while paused
            while (_state.value.isPaused) {
                delay(500)
                if (_state.value.isStopped) return
            }

            val contact = contacts[i]
            val template = if (templates.size > 1) templates.random() else templates.first()
            val message = TemplateEngine.render(template, contact)

            updateState(i, contact, message, "Opening WhatsApp...")

            if (testMode) {
                // Simulate send
                delay(500)
                val entry = LogEntry(
                    phone = contact.phone, name = contact.name,
                    message = message, status = SendStatus.SIMULATED
                )
                logger.log(entry)
                incrementState(i, contacts.size, sent = true)
                Log.d(TAG, "TEST MODE: Simulated send to ${contact.phone}")
            } else {
                // Safety: Check WhatsApp is installed
                if (!WhatsAppLauncher.isWhatsAppInstalled(context)) {
                    updateStatusMessage("WhatsApp not installed — stopping campaign")
                    stop()
                    return
                }

                // Open WhatsApp chat with pre-filled message
                val opened = WhatsAppLauncher.openChat(context, contact.phone, message)
                if (!opened) {
                    logAndIncrement(i, contacts.size, contact, message, SendStatus.FAILED_BUTTON, "Failed to open WhatsApp")
                    continue
                }

                // Wait for message to load
                val waitTime = if (message.length > LONG_MESSAGE_THRESHOLD) {
                    MESSAGE_OPEN_WAIT_MS + LONG_MESSAGE_EXTRA_WAIT_MS
                } else {
                    MESSAGE_OPEN_WAIT_MS
                }
                delay(waitTime)

                if (manualMode) {
                    // Manual mode: user taps send themselves
                    updateStatusMessage("Manual mode — please tap Send in WhatsApp")
                    delay(8000) // Give user time to send
                    logAndIncrement(i, contacts.size, contact, message, SendStatus.SENT)
                } else {
                    // Auto mode via Accessibility Service
                    val service = WhatsAppAccessibilityService.instance
                    if (service == null || !WhatsAppAccessibilityService.isServiceEnabled) {
                        updateStatusMessage("Accessibility Service not active — switching to manual mode")
                        delay(8000)
                        logAndIncrement(i, contacts.size, contact, message, SendStatus.SENT)
                    } else {
                        // Check foreground
                        if (!service.isWhatsAppForeground()) {
                            pause()
                            updateStatusMessage("WhatsApp not in foreground — paused. Return to WhatsApp to resume.")
                            while (!service.isWhatsAppForeground() && !_state.value.isStopped) {
                                delay(1000)
                            }
                            resume()
                        }

                        // Check login screen
                        if (service.isLoginScreenVisible()) {
                            updateStatusMessage("WhatsApp login screen detected — stopping campaign")
                            stop()
                            return
                        }

                        // Tap send
                        val sent = service.tapSendButton()
                        delay(500)

                        if (sent) {
                            logAndIncrement(i, contacts.size, contact, message, SendStatus.SENT)
                        } else {
                            logAndIncrement(i, contacts.size, contact, message, SendStatus.FAILED_BUTTON, "Send button not found after retry")
                        }
                    }
                }
            }

            // Persist queue index
            queueManager.saveIndex(i + 1)
            messagesSinceBreak++

            // Cooldown every N messages
            if (messagesSinceBreak >= COOLDOWN_EVERY) {
                messagesSinceBreak = 0
                updateStatusMessage("Cooldown — waiting 30 seconds...")
                delay(COOLDOWN_DURATION_MS)
            } else {
                // Random delay between messages
                val delay = (MIN_DELAY_MS..MAX_DELAY_MS).random()
                updateStatusMessage("Waiting ${delay / 1000}s before next message...")
                delay(delay)
            }
        }

        // Campaign complete
        _state.value = _state.value.copy(
            isRunning = false,
            statusMessage = "Campaign complete!",
            endTime = System.currentTimeMillis(),
            currentIndex = contacts.size
        )
        queueManager.clear()
    }

    // ── State Helpers ─────────────────────────────────────────────────────────

    private fun updateState(index: Int, contact: Contact, message: String, status: String) {
        _state.value = _state.value.copy(
            currentIndex = index,
            currentPhone = contact.phone,
            currentName = contact.name,
            currentMessage = message,
            statusMessage = status
        )
    }

    private fun updateStatusMessage(msg: String) {
        _state.value = _state.value.copy(statusMessage = msg)
    }

    private fun incrementState(index: Int, total: Int, sent: Boolean = false, failed: Boolean = false) {
        val current = _state.value
        _state.value = current.copy(
            currentIndex = index + 1,
            sent = current.sent + if (sent) 1 else 0,
            failed = current.failed + if (failed) 1 else 0
        )
    }

    private suspend fun logAndIncrement(
        index: Int, total: Int,
        contact: Contact, message: String,
        status: SendStatus, error: String = ""
    ) {
        logger.log(LogEntry(
            phone = contact.phone, name = contact.name,
            message = message, status = status, errorDetail = error
        ))
        incrementState(index, total, sent = status == SendStatus.SENT, failed = status != SendStatus.SENT)
    }

    private fun LongRange.random(): Long = (first + (Math.random() * (last - first)).toLong())

    fun getSummaryFlow() = logger.getAllFlow()

    suspend fun exportFailed(context: Context): java.io.File = logger.exportFailedAsCsv(context)
}
