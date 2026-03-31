package com.internal.wamessenger.core

import android.content.Context
import com.internal.wamessenger.database.AppDatabase
import com.internal.wamessenger.database.MessageLogEntity
import com.internal.wamessenger.model.LogEntry
import com.internal.wamessenger.model.SendStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MessageLogger(context: Context) {

    private val dao = AppDatabase.getInstance(context).messageLogDao()

    suspend fun log(entry: LogEntry) {
        dao.insert(MessageLogEntity(
            phone = entry.phone,
            name = entry.name,
            message = entry.message,
            status = entry.status.name,
            timestamp = entry.timestamp,
            errorDetail = entry.errorDetail
        ))
    }

    fun getAllFlow() = dao.getAllFlow()

    suspend fun getFailedEntries(): List<MessageLogEntity> = dao.getFailedEntries()

    suspend fun exportFailedAsCsv(context: Context): File {
        val failed = getFailedEntries()
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val file = File(dir, "failed_${sdf.format(Date())}.csv")

        file.bufferedWriter().use { writer ->
            writer.write("phone,name,status,message,timestamp,error\n")
            failed.forEach { e ->
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(e.timestamp))
                val msgSafe = e.message.replace(",", ";").replace("\n", " ")
                writer.write("${e.phone},${e.name},${e.status},\"$msgSafe\",$ts,${e.errorDetail}\n")
            }
        }
        return file
    }

    suspend fun clear() = dao.clearAll()

    suspend fun getSummary(): LogSummary {
        return LogSummary(
            sent = dao.getSentCount(),
            failed = dao.getFailedCount(),
            skipped = dao.getSkippedCount()
        )
    }
}

data class LogSummary(val sent: Int, val failed: Int, val skipped: Int) {
    val total: Int get() = sent + failed + skipped
}
