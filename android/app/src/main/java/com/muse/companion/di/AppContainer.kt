package com.muse.companion.di

import android.content.Context
import androidx.room.Room
import com.muse.companion.agent.Orchestrator
import com.muse.companion.ai.ConnectivityMonitor
import com.muse.companion.ai.ContextBuilder
import com.muse.companion.ai.OfflineRuleEngine
import com.muse.companion.ai.device.DeviceCapabilities
import com.muse.companion.data.audit.RoomAuditLog
import com.muse.companion.data.db.MuseDatabase
import com.muse.companion.data.memory.MemoryRepository
import com.muse.companion.data.settings.SettingsRepository
import com.muse.companion.security.PermissionBroker
import com.muse.companion.security.SecurityPolicy
import com.muse.companion.security.crypto.AesGcmSecretBox
import com.muse.companion.security.crypto.KeystoreKeyProvider
import com.muse.companion.security.crypto.SecretBox
import com.muse.companion.security.guards.PathGuard
import com.muse.companion.security.guards.UrlGuard
import com.muse.companion.tools.ToolRegistry
import com.muse.companion.tools.impl.LaunchAppTool
import com.muse.companion.tools.impl.OpenUrlTool
import com.muse.companion.tools.impl.ShareTextTool
import com.muse.companion.tools.impl.WebFetchTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hand-wired dependency graph (no Hilt in M1 — see docs/02 §3). Single source of app singletons. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val db = Room.databaseBuilder(appContext, MuseDatabase::class.java, "muse.db")
        // M1 ships schema v1; real migrations are added as the schema evolves (see docs/02 §7).
        .fallbackToDestructiveMigration()
        .build()

    val secretBox: SecretBox = AesGcmSecretBox(KeystoreKeyProvider())
    val settings = SettingsRepository(appContext)
    val memory = MemoryRepository(db.memoryDao(), secretBox)
    val auditDao = db.auditDao()
    private val audit = RoomAuditLog(auditDao, appScope)

    val connectivity = ConnectivityMonitor(appContext)
    val device = DeviceCapabilities(appContext)
    private val contextBuilder = ContextBuilder()

    val toolRegistry = ToolRegistry(
        listOf(
            OpenUrlTool(appContext),
            ShareTextTool(appContext),
            LaunchAppTool(appContext),
            WebFetchTool(),
        ),
    )

    /** A broker bound to the CURRENT policy snapshot. Cheap to build per request. */
    fun broker(policy: SecurityPolicy) = PermissionBroker(
        specs = toolRegistry.specs,
        policy = policy,
        urlGuard = UrlGuard(),
        pathGuard = PathGuard(),
        audit = audit,
    )

    /** Orchestrator for one turn. Uses the offline engine in M1 (swap in Settings once a model exists). */
    fun orchestrator(policy: SecurityPolicy) = Orchestrator(
        engine = OfflineRuleEngine(),
        broker = broker(policy),
        tools = toolRegistry,
        contextBuilder = contextBuilder,
        audit = audit,
    )
}
