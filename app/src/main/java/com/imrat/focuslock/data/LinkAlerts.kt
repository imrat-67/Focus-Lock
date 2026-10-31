package com.imrat.focuslock.data

/** Pure: which link events deserve a notification (NOT-3)? Each event notifies once. */
object LinkAlerts {
    enum class Kind { NEW_REQUEST, REMOVAL_ASKED, REQUEST_ACCEPTED, REQUEST_DECLINED }
    data class Alert(val key: String, val kind: Kind)
    data class Result(val fresh: List<Alert>, val currentKeys: Set<String>, val pendingNow: Set<String> = emptySet())

    /**
     * [myLink]: my Observer link; [people]: Users I observe (incl. pending requests I sent); [seen]: keys already notified;
     * [prevPending]: Users whose request from me was pending last time, so a vanished one reads as declined, an active one as accepted.
     */
    fun plan(myLink: Link?, people: List<Link>, seen: Set<String>, prevPending: Set<String> = emptySet()): Result {
        val all = buildList {
            if (myLink != null && myLink.status == LinkStatus.PENDING) {
                add(Alert("req:${myLink.observerUid}", Kind.NEW_REQUEST))
            }
            people.filter { it.status == LinkStatus.REMOVAL_REQUESTED }
                .forEach { add(Alert("rm:${it.userUid}", Kind.REMOVAL_ASKED)) }
        }
        val byUser = people.associateBy { it.userUid }
        val outcome = prevPending.mapNotNull { u ->
            val l = byUser[u]
            when {
                l == null -> Alert("dec:$u", Kind.REQUEST_DECLINED)
                l.status == LinkStatus.ACTIVE -> Alert("acc:$u", Kind.REQUEST_ACCEPTED)
                else -> null
            }
        }
        val pendingNow = people.filter { it.status == LinkStatus.PENDING }.map { it.userUid }.toSet()
        // Keys that vanished are forgotten, so a later repeat of the same event notifies again.
        return Result(all.filter { it.key !in seen } + outcome, all.map { it.key }.toSet(), pendingNow)
    }
}
