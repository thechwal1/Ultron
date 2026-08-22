package com.talha.ultron.memory

import android.content.Context
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.talha.ultron.notification.NotificationDao
import com.talha.ultron.notification.NotificationEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

@Database(
    entities = [MemoryEntity::class, PriorityEntity::class, MacroEntity::class, NotificationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun priorityDao(): PriorityDao
    abstract fun macroDao(): MacroDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile private var INSTANCE: MemoryDatabase? = null

        fun get(context: Context): MemoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): MemoryDatabase {
            SQLiteDatabase.loadLibs(context)
            val factory = SupportFactory(getOrCreatePassphrase(context))
            return Room.databaseBuilder(
                context.applicationContext,
                MemoryDatabase::class.java,
                "ultron_memory.db"
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }

        private fun getOrCreatePassphrase(context: Context): ByteArray {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                "ultron_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("db_passphrase", null)?.let {
                return Base64.decode(it, Base64.NO_WRAP)
            }
            val newKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
            prefs.edit()
                .putString("db_passphrase", Base64.encodeToString(newKey, Base64.NO_WRAP))
                .apply()
            return newKey
        }
    }
}
