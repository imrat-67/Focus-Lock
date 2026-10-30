package com.imrat.focuslock.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.filter
import com.imrat.focuslock.time.ScheduleSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/** `users/{uid}/schedules/{sid}` (SCH-1, SCH-2). Days are ISO ints 1=Mon..7=Sun. */
data class Schedule(
    val id: String,
    val createdBy: String,
    val mode: LockMode,
    val packages: List<String>,
    val days: Set<Int>,
    val oneTimeDate: String?, // yyyy-MM-dd
    val startMin: Int,
    val endMin: Int,
    val tz: String,
    val enabled: Boolean,
    /** UNL-6, Observer-made schedules only: occurrence start (ms, text) -> "hash:salt". */
    val pwPool: Map<String, String> = emptyMap(),
) {
    fun toSpec(): ScheduleSpec = ScheduleSpec(
        days.mapNotNull { runCatching { DayOfWeek.of(it) }.getOrNull() }.toSet(), startMin, endMin,
        oneTimeDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    )

    /** Valid and (for Apps mode) has at least one app. */
    fun isUsable(): Boolean = toSpec().isValid() && (mode == LockMode.FULL || packages.isNotEmpty()) &&
        (oneTimeDate == null || runCatching { LocalDate.parse(oneTimeDate) }.isSuccess)
}

object ScheduleCodec {
    fun toMap(s: Schedule): Map<String, Any?> = buildMap {
        put("createdBy", s.createdBy); put("mode", s.mode.wire)
        put("startMin", s.startMin); put("endMin", s.endMin); put("tz", s.tz); put("enabled", s.enabled)
        if (s.packages.isNotEmpty()) put("packages", s.packages)
        if (s.pwPool.isNotEmpty()) put("pwPool", s.pwPool)
        if (s.oneTimeDate != null) put("oneTimeDate", s.oneTimeDate) else put("days", s.days.sorted())
    }

    fun fromMap(id: String, m: Map<String, Any?>): Schedule? {
        val mode = LockMode.parse(m["mode"] as? String) ?: return null
        val by = m["createdBy"] as? String ?: return null
        val start = (m["startMin"] as? Number)?.toInt() ?: return null
        val end = (m["endMin"] as? Number)?.toInt() ?: return null
        val tz = m["tz"] as? String ?: return null
        return Schedule(
            id, by, mode, (m["packages"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
            (m["days"] as? List<*>)?.filterIsInstance<Number>()?.map { it.toInt() }?.toSet().orEmpty(),
            m["oneTimeDate"] as? String, start, end, tz, m["enabled"] as? Boolean ?: true,
            (m["pwPool"] as? Map<*, *>)?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }?.toMap().orEmpty(),
        )
    }

    fun toJson(list: List<Schedule>): String = JSONArray().also { arr ->
        list.forEach { s ->
            arr.put(
                JSONObject().put("id", s.id).put("by", s.createdBy).put("mode", s.mode.wire)
                    .put("pkgs", JSONArray(s.packages)).put("days", JSONArray(s.days.toList()))
                    .put("date", s.oneTimeDate ?: JSONObject.NULL).put("st", s.startMin).put("en", s.endMin)
                    .put("tz", s.tz).put("on", s.enabled).put("pool", JSONObject(s.pwPool))
            )
        }
    }.toString()

    fun fromJson(text: String): List<Schedule> = try {
        val arr = JSONArray(text)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val pk = o.getJSONArray("pkgs"); val dy = o.getJSONArray("days")
            Schedule(
                o.getString("id"), o.getString("by"), LockMode.parse(o.getString("mode")) ?: return@mapNotNull null,
                (0 until pk.length()).map { pk.getString(it) }, (0 until dy.length()).map { dy.getInt(it) }.toSet(),
                if (o.isNull("date")) null else o.getString("date"), o.getInt("st"), o.getInt("en"), o.getString("tz"), o.getBoolean("on"),
                o.optJSONObject("pool")?.let { po -> po.keys().asSequence().associateWith { k -> po.getString(k) } }.orEmpty(),
            )
        }
    } catch (_: org.json.JSONException) {
        emptyList()
    }
}

class ScheduleRepository(private val db: FirebaseFirestore) {
    private fun col(uid: String) = db.collection("users").document(uid).collection("schedules")

    fun observe(uid: String): Flow<List<Schedule>> = col(uid).snapshots().map { snap ->
        snap.documents.mapNotNull { d -> d.data?.let { ScheduleCodec.fromMap(d.id, it) } }
    }

    /** Like [observe] but only server-confirmed lists, never the possibly empty first cache snapshot. */
    fun observeConfirmed(uid: String): Flow<List<Schedule>> = col(uid).snapshots(MetadataChanges.INCLUDE)
        .filter { !it.metadata.isFromCache }
        .map { snap -> snap.documents.mapNotNull { d -> d.data?.let { ScheduleCodec.fromMap(d.id, it) } } }

    suspend fun create(uid: String, s: Schedule): String {
        val ref = col(uid).document()
        ref.set(ScheduleCodec.toMap(s)).await()
        return ref.id
    }

    suspend fun update(uid: String, s: Schedule) { col(uid).document(s.id).set(ScheduleCodec.toMap(s)).await() }

    suspend fun delete(uid: String, id: String) { col(uid).document(id).delete().await() }
}
