package com.imrat.focuslock.data

/** Pure: which link events deserve a notification (NOT-3)? Each event notifies once. */
object LinkAlerts {
    enum class Kind { NEW_REQUEST, REMOVAL_ASKED }
    data class Alert(val key: String, val kind: Kind)
    data class Result(val fresh: List<Alert>, val currentKeys: Set<String>)

    /** [myLink]: my Observer link; [people]: Users I observe; [seen]: keys already notified. */
    fun plan(myLink: Link?, people: List<Link>, seen: Set<String>): Result {
        val all = buildList {
            if (myLink != null && myLink.status == LinkStatus.PENDING) {
                add(Alert("req:${myLink.observerUid}", Kind.NEW_REQUEST))
            }
            people.filter { it.status == LinkStatus.REMOVAL_REQUESTED }
                .forEach { add(Alert("rm:${it.userUid}", Kind.REMOVAL_ASKED)) }
        }
        // Keys that vanished are forgotten, so a later repeat of the same event notifies again.
        return Result(all.filter { it.key !in seen }, all.map { it.key }.toSet())
    }
}
