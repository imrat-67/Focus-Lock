package com.imrat.focuslock.data

/** Pure link rules (LINK-4, LINK-6, LINK-7). No Android/Firebase, so they are unit-testable. */
object LinkRules {

    /** LINK-7: removal can only be requested for an active link while no lock is running. */
    fun canRequestRemoval(link: Link?, lockActive: Boolean): Boolean =
        link != null && link.status == LinkStatus.ACTIVE && !lockActive

    /** LINK-6: days left before a silent removal request completes by itself. */
    fun daysUntilAutoRemoval(link: Link, nowMs: Long): Long {
        val asked = link.removalRequestedAtMs ?: return 0
        val left = asked + LinkRepository.AUTO_REMOVE_AFTER_MS - nowMs
        return if (left <= 0) 0 else (left + DAY_MS - 1) / DAY_MS
    }

    /** LINK-7: the automatic completion waits until the lock ends. */
    fun shouldAutoComplete(link: Link, nowMs: Long, lockActive: Boolean): Boolean =
        !lockActive && LinkRepository.autoRemovalDue(link, nowMs)

    private const val DAY_MS = 24L * 60 * 60 * 1000
}
