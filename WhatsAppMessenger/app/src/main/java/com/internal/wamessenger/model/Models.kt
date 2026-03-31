package com.internal.wamessenger.model

data class Contact(
    val phone: String,
    val fields: Map<String, String>  // name, company, role, and any extra CSV columns
) {
    val name: String get() = fields["name"] ?: ""
    val displayPhone: String get() = phone
}

enum class SendStatus {
    PENDING,
    SENT,
    FAILED_BUTTON,
    FAILED_INVALID_NUMBER,
    SKIPPED_DUPLICATE,
    SIMULATED
}

data class CampaignStatus(
    val total: Int = 0,
    val currentIndex: Int = 0,
    val sent: Int = 0,
    val failed: Int = 0,
    val skipped: Int = 0,
    val isPaused: Boolean = false,
    val isStopped: Boolean = false,
    val isRunning: Boolean = false,
    val currentContact: Contact? = null,
    val currentMessage: String = ""
) {
    val progress: Float get() = if (total == 0) 0f else currentIndex.toFloat() / total
    val isComplete: Boolean get() = !isRunning && currentIndex >= total && total > 0
}

data class LogEntry(
    val id: Long = 0,
    val phone: String,
    val name: String,
    val message: String,
    val status: SendStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val errorDetail: String = ""
)
