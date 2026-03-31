package com.internal.wamessenger.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.internal.wamessenger.core.CampaignController
import kotlinx.coroutines.*

class WhatsAppAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "WAAccessibility"
        private const val WA_PACKAGE = "com.whatsapp"
        private const val INPUT_ID = "com.whatsapp:id/entry"
        private const val SEND_ID = "com.whatsapp:id/send"

        @Volatile
        var instance: WhatsAppAccessibilityService? = null
            private set

        @Volatile
        var isServiceEnabled: Boolean = false
            private set
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected() {
        instance = this
        isServiceEnabled = true
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events handled on-demand via sendMessage()
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        isServiceEnabled = false
        scope.cancel()
        super.onDestroy()
    }

    // ── Public API called by CampaignController ───────────────────────────────

    /**
     * Attempts to tap the send button in the currently open WhatsApp chat.
     * Returns true if button was found and clicked.
     */
    suspend fun tapSendButton(): Boolean {
        return withContext(Dispatchers.Main) {
            // Ensure input field is focused first
            ensureInputFocused()
            delay(300)

            // Try to find and click send button
            val result = tryClickSend()
            if (!result) {
                // Retry once after delay
                delay(1000)
                ensureInputFocused()
                delay(300)
                tryClickSend()
            } else {
                result
            }
        }
    }

    /**
     * Check that the currently active window is WhatsApp
     */
    fun isWhatsAppForeground(): Boolean {
        return try {
            rootInActiveWindow?.packageName?.toString() == WA_PACKAGE
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if a login/QR screen is visible
     */
    fun isLoginScreenVisible(): Boolean {
        val root = rootInActiveWindow ?: return false
        val loginIndicators = listOf("Log in", "QR code", "Phone number", "Agree and continue")
        return loginIndicators.any { indicator ->
            root.findAccessibilityNodeInfosByText(indicator).isNotEmpty()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun ensureInputFocused() {
        try {
            val root = rootInActiveWindow ?: return

            // Find the message input field
            val inputNodes = root.findAccessibilityNodeInfosByViewId(INPUT_ID)
            val inputNode = inputNodes.firstOrNull()
                ?: findNodeByClass(root, "android.widget.EditText")
                ?: return

            if (!inputNode.isFocused) {
                inputNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error focusing input: ${e.message}")
        }
    }

    private fun tryClickSend(): Boolean {
        return try {
            val root = rootInActiveWindow ?: return false

            // Strategy 1: content description "Send"
            val byDesc = root.findAccessibilityNodeInfosByText("Send")
            val sendByDesc = byDesc.firstOrNull { node ->
                node.contentDescription?.contains("Send", ignoreCase = true) == true
                        || node.isClickable
            }
            if (sendByDesc != null) {
                sendByDesc.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Send clicked via description")
                return true
            }

            // Strategy 2: Resource ID
            val byId = root.findAccessibilityNodeInfosByViewId(SEND_ID)
            val sendById = byId.firstOrNull()
            if (sendById != null) {
                sendById.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Send clicked via resource ID")
                return true
            }

            // Strategy 3: Button class traversal
            val button = findSendButton(root)
            if (button != null) {
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(TAG, "Send clicked via class traversal")
                return true
            }

            // Strategy 4: Coordinate fallback
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            val x = (width * 0.92f)
            val y = (height * 0.78f)
            performCoordinateClick(x, y)
            Log.d(TAG, "Send clicked via coordinates ($x, $y)")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error clicking send: ${e.message}")
            false
        }
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.className?.contains("Button") == true && root.isClickable) {
            val desc = root.contentDescription?.toString() ?: ""
            val text = root.text?.toString() ?: ""
            if (desc.contains("send", ignoreCase = true) || text.contains("send", ignoreCase = true)) {
                return root
            }
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findSendButton(child)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByClass(root: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (root.className?.toString() == className) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByClass(child, className)
            if (found != null) return found
        }
        return null
    }

    private fun performCoordinateClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }
}
