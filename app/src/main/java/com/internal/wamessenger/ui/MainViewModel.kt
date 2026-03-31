package com.internal.wamessenger.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.internal.wamessenger.core.*
import com.internal.wamessenger.model.Contact
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val contacts: List<Contact> = emptyList(),
    val csvFileName: String = "",
    val csvErrors: List<String> = emptyList(),
    val csvWarnings: List<String> = emptyList(),
    val csvHeaders: List<String> = emptyList(),
    val templates: List<String> = listOf(""),
    val testMode: Boolean = true,
    val manualMode: Boolean = false,
    val isLoading: Boolean = false,
    val validationErrors: List<String> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val controller = CampaignController(application)
    private val queueManager = QueueManager(application)

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    val campaignState: StateFlow<CampaignState> = controller.state

    val queueState: StateFlow<QueueState> = queueManager.getStateFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, QueueState())

    // ── CSV ────────────────────────────────────────────────────────────────────

    fun loadCsv(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _homeState.value = _homeState.value.copy(isLoading = true)
            val result = CsvParser.parse(getApplication(), uri)
            val limited = if (result.contacts.size > QueueManager.MAX_RECIPIENTS) {
                result.contacts.take(QueueManager.MAX_RECIPIENTS)
            } else result.contacts

            _homeState.value = _homeState.value.copy(
                contacts = limited,
                csvFileName = fileName,
                csvErrors = result.errors + if (result.contacts.size > QueueManager.MAX_RECIPIENTS)
                    listOf("Capped to ${QueueManager.MAX_RECIPIENTS} recipients") else emptyList(),
                csvWarnings = result.warnings,
                isLoading = false
            )
        }
    }

    // ── Templates ──────────────────────────────────────────────────────────────

    fun updateTemplate(index: Int, value: String) {
        val list = _homeState.value.templates.toMutableList()
        if (index < list.size) list[index] = value else list.add(value)
        _homeState.value = _homeState.value.copy(templates = list)
    }

    fun addTemplate() {
        val list = _homeState.value.templates.toMutableList()
        list.add("")
        _homeState.value = _homeState.value.copy(templates = list)
    }

    fun removeTemplate(index: Int) {
        val list = _homeState.value.templates.toMutableList()
        if (list.size > 1) {
            list.removeAt(index)
            _homeState.value = _homeState.value.copy(templates = list)
        }
    }

    fun setTestMode(v: Boolean) { _homeState.value = _homeState.value.copy(testMode = v) }
    fun setManualMode(v: Boolean) { _homeState.value = _homeState.value.copy(manualMode = v) }

    // ── Validation + Launch ────────────────────────────────────────────────────

    fun validate(): Boolean {
        val state = _homeState.value
        val errors = mutableListOf<String>()

        if (state.contacts.isEmpty()) errors.add("No contacts loaded from CSV")

        state.templates.forEachIndexed { i, t ->
            val errs = TemplateEngine.validate(t)
            errs.forEach { errors.add("Template ${i + 1}: $it") }
        }

        _homeState.value = state.copy(validationErrors = errors)
        return errors.isEmpty()
    }

    fun startCampaign() {
        if (!validate()) return
        val s = _homeState.value
        controller.startCampaign(
            contactList = s.contacts,
            templateList = s.templates.filter { it.isNotBlank() },
            isTestMode = s.testMode,
            isManualMode = s.manualMode
        )
    }

    fun pauseCampaign() = controller.pause()
    fun resumeCampaign() = controller.resume()
    fun stopCampaign() = controller.stop()

    suspend fun exportFailed(): java.io.File? {
        return try { controller.exportFailed(getApplication()) } catch (e: Exception) { null }
    }

    // Preview messages for PreviewScreen
    fun getPreviewMessages(): List<Pair<Contact, String>> {
        val s = _homeState.value
        return s.contacts.map { contact ->
            val template = if (s.templates.size > 1) s.templates.random() else s.templates.first()
            contact to TemplateEngine.render(template, contact)
        }
    }
}
