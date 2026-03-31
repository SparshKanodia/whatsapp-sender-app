package com.internal.wamessenger.database

import androidx.room.*
import com.internal.wamessenger.model.SendStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "message_log")
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String,
    val name: String,
    val message: String,
    val status: String,  // SendStatus.name
    val timestamp: Long = System.currentTimeMillis(),
    val errorDetail: String = ""
)

@Dao
interface MessageLogDao {
    @Insert
    suspend fun insert(entry: MessageLogEntity): Long

    @Query("SELECT * FROM message_log ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<MessageLogEntity>>

    @Query("SELECT * FROM message_log WHERE status != 'SENT' AND status != 'SIMULATED' ORDER BY timestamp DESC")
    suspend fun getFailedEntries(): List<MessageLogEntity>

    @Query("SELECT COUNT(*) FROM message_log WHERE status = 'SENT'")
    suspend fun getSentCount(): Int

    @Query("SELECT COUNT(*) FROM message_log WHERE status = 'FAILED_BUTTON' OR status = 'FAILED_INVALID_NUMBER'")
    suspend fun getFailedCount(): Int

    @Query("SELECT COUNT(*) FROM message_log WHERE status = 'SKIPPED_DUPLICATE'")
    suspend fun getSkippedCount(): Int

    @Query("DELETE FROM message_log")
    suspend fun clearAll()
}

@Database(entities = [MessageLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageLogDao(): MessageLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "wamessenger.db")
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
