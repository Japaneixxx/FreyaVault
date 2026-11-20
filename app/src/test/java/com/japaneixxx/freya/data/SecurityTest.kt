package com.japaneixxx.freya.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SecurityTest {
    @Test fun pinHashDependsOnSaltAndPin() {
        val s1 = PinHasher.newSalt(); val s2 = PinHasher.newSalt()
        val h = PinHasher.pbkdf2("1234", s1, iterations = 1_000)
        assertEquals(h, PinHasher.pbkdf2("1234", s1, iterations = 1_000))
        assertNotEquals(h, PinHasher.pbkdf2("1235", s1, iterations = 1_000))
        assertNotEquals(h, PinHasher.pbkdf2("1234", s2, iterations = 1_000))
        assertTrue(PinHasher.same(h, h))
        assertFalse(PinHasher.same(h, PinHasher.legacySha256("1234", s1)))
    }

    @Test fun pinLockoutGrowsAndIsCapped() {
        assertEquals(0L, PinHasher.lockDelayMs(4))
        assertEquals(30_000L, PinHasher.lockDelayMs(5))
        assertEquals(60_000L, PinHasher.lockDelayMs(6))
        assertEquals(15 * 60_000L, PinHasher.lockDelayMs(50))
    }

    @Test fun backupEncryptionRoundTrip() {
        val plain = """{"version":2,"nota":"ação é útil"}"""
        val enc = BackupCrypto.encrypt(plain, "senha-forte".toCharArray())
        assertTrue(BackupCrypto.isEncrypted(enc))
        assertFalse(BackupCrypto.isEncrypted(plain))
        assertFalse(enc.contains("nota"))
        assertEquals(plain, BackupCrypto.decrypt(enc, "senha-forte".toCharArray()))
    }

    @Test fun wrongPasswordOrTamperedFileIsRejected() {
        val enc = BackupCrypto.encrypt("segredo", "certa123".toCharArray())
        try { BackupCrypto.decrypt(enc, "errada".toCharArray()); fail("deveria falhar") } catch (_: BadBackupPasswordException) {}
        val o = org.json.JSONObject(enc)
        val data = o.getString("data")
        o.put("data", (if (data[0] == 'A') "B" else "A") + data.drop(1))
        try { BackupCrypto.decrypt(o.toString(), "certa123".toCharArray()); fail("deveria falhar") } catch (_: BadBackupPasswordException) {}
    }
}
