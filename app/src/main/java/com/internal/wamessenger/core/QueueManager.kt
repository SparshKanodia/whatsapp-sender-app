package com.internal.wamessenger.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.internal.wamessenger.model.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "campaign_queue")

class QueueManager(private val context: Context) {

    companion object {
        private val KEY_QUEUE_JSON = stringPreferencesKey("queue_json")
        private val KEY_INDEX = intPreferencesKey("current_index")
        private val KEY_PAUSED = booleanPreferencesKey("is_paused")
        private val KEY_STOPPED = booleanPreferencesKey("is_stopped")
        private val KEY_TEMPLATES = stringPreferencesKey("templates_json")
        private val KEY_START_TIME = longPreferencesKey("start_time")
        private val KEY_TEST_MODE = booleanPreferencesKey("test_mode")
        private val KEY_MANUAL_MODE = booleanPreferencesKey("manual_mode")

        const val MAX_RECIPIENTS = 50
    }

    // ── Persist ──────────────────────────────────────────────────────────────

    suspend fun saveQueue(contacts: List<Contact>, templates: List<String>,
                          testMode: Boolean, manualMode: Boolean) {
        val arr = JSONArray()
        contacts.forEach { c ->
            val obj = JSONObject()
            obj.put("phone", c.phone)
            val fields = JSONObject()
            c.fields.forEach { (k, v) -> fields.put(k, v) }
            obj.put("fields", fields)
            arr.put(obj)
        }
        val templatesArr = JSONArray(templates)
        context.dataStore.edit { prefs ->
            prefs[KEY_QUEUE_JSON] = arr.toString()
            prefs[KEY_TEMPLATES] = templatesArr.toString()
            prefs[KEY_INDEX] = 0
            prefs[KEY_PAUSED] = false
            prefs[KEY_STOPPED] = false
            prefs[KEY_START_TIME] = System.currentTimeMillis()
            prefs[KEY_TEST_MODE] = testMode
            prefs[KEY_MANUAL_MODE] = manualMode
        }
    }

    suspend fun saveIndex(index: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_INDEX] = index }
    }

    suspend fun setPaused(paused: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_PAUSED] = paused }
    }

    suspend fun setStopped(stopped: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_STOPPED] = stopped }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    fun getStateFlow(): Flow<QueueState> = context.dataStore.data.map { prefs ->
        val queueJson = prefs[KEY_QUEUE_JSON] ?: "[]"
        val templatesJson = prefs[KEY_TEMPLATES] ?: "[]"
        val index = prefs[KEY_INDEX] ?: 0
        val paused = prefs[KEY_PAUSED] ?: false
        val stopped = prefs[KEY_STOPPED] ?: false
        val startTime = prefs[KEY_START_TIME] ?: 0L
        val testMode = prefs[KEY_TEST_MODE] ?: false
        val manualMode = prefs[KEY_MANUAL_MODE] ?: false

        val contacts = parseContacts(queueJson)
        val templates = parseTemplates(templatesJson)

        QueueState(
            contacts = contacts,
            templates = templates,
            currentIndex = index,
            isPaused = paused,
            isStopped = stopped,
            startTime = startTime,
            testMode = testMode,
            manualMode = manualMode
        )
    }

    private fun parseContacts(json: String): List<Contact> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val phone = obj.getString("phone")
                val fieldsObj = obj.getJSONObject("fields")
                val fields = mutableMapOf<String, String>()
                fieldsObj.keys().forEach { k -> fields[k] = fieldsObj.getString(k) }
                Contact(phone, fields)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseTemplates(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class QueueState(
    val contacts: List<Contact> = emptyList(),
    val templates: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val isPaused: Boolean = false,
    val isStopped: Boolean = false,
    val startTime: Long = 0L,
    val testMode: Boolean = false,
    val manualMode: Boolean = false
) {
    val hasActiveCampaign: Boolean get() = contacts.isNotEmpty() && currentIndex < contacts.size
    val currentContact: Contact? get() = if (currentIndex < contacts.size) contacts[currentIndex] else null
}
