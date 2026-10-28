package com.muse.companion.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.muse.companion.data.db.MuseDatabase
import com.muse.companion.data.memory.MemoryLayer
import com.muse.companion.data.memory.MemoryRepository
import com.muse.companion.data.memory.Sensitivity
import com.muse.companion.security.crypto.AesGcmSecretBox
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MemoryRepositoryTest {
    private lateinit var db: MuseDatabase
    private lateinit var repo: MemoryRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MuseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        repo = MemoryRepository(db.memoryDao(), AesGcmSecretBox { key })
    }

    @After
    fun teardown() = db.close()

    @Test fun insertAndSearch() = runTest {
        repo.remember("owner loves strategy games", MemoryLayer.PERMANENT)
        val hits = repo.search("strategy")
        assertThat(hits).isNotEmpty()
        assertThat(hits.first().content).contains("strategy")
    }

    @Test fun sensitiveIsEncryptedAtRestButReadsBack() = runTest {
        repo.remember("bank pin is 1234", MemoryLayer.PERMANENT, sensitivity = Sensitivity.SENSITIVE)
        // Plaintext search must NOT surface encrypted content.
        assertThat(repo.search("1234")).isEmpty()
        // But it decrypts transparently when listed.
        assertThat(repo.recent().any { it.content.contains("1234") }).isTrue()
    }

    @Test fun deleteAndClear() = runTest {
        val id = repo.remember("temp", MemoryLayer.EPISODIC)
        repo.deleteById(id)
        assertThat(repo.count()).isEqualTo(0)
        repo.remember("a", MemoryLayer.EPISODIC)
        repo.remember("b", MemoryLayer.EPISODIC)
        repo.clearAll()
        assertThat(repo.count()).isEqualTo(0)
    }
}
