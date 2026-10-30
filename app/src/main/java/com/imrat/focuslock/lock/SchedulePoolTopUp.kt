package com.imrat.focuslock.lock

import com.imrat.focuslock.data.Schedule
import com.imrat.focuslock.data.ScheduleRepository
import com.imrat.focuslock.security.PasswordCrypto
import com.imrat.focuslock.security.PasswordVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Observer side of UNL-6: keeps 14 upcoming occurrences of an Observer-made schedule supplied with a fresh password.
 * Plaintext goes to the encrypted [vault] first, then only the salted hashes go to the schedule doc.
 * One top-up at a time, so a stale snapshot can never leave vault and server disagreeing.
 */
class SchedulePoolTopUp(private val vault: PasswordVault, private val repo: ScheduleRepository) {

    /** Returns the schedule as it stands afterwards. Throws if the server write fails (vault entries are then simply re-made next time). */
    suspend fun topUp(targetUid: String, me: String, s: Schedule, nowMs: Long = System.currentTimeMillis()): Schedule = lock.withLock {
        if (s.createdBy != me || targetUid == me) return s // only the Observer's own schedules on someone else's phone
        val keep = SchedulePool.pruned(s.pwPool, nowMs)
        val miss = SchedulePool.missing(keep, SchedulePool.upcoming(s, nowMs))
        if (miss.isEmpty() && keep.size == s.pwPool.size) return s
        val pool = keep.toMutableMap()
        for (start in miss) {
            val f = withContext(Dispatchers.Default) { PasswordCrypto.fresh() }
            vault.put(SchedulePool.vaultKey(s.id, start), f.plain)
            pool[start.toString()] = SchedulePool.encode(f.hash, f.salt)
        }
        val updated = s.copy(pwPool = pool)
        repo.update(targetUid, updated)
        updated
    }

    private companion object { val lock = Mutex() }
}
