package com.imrat.focuslock.data

/** A launchable app: display name + package (FMT-3 uploads only these two). */
data class AppEntry(val name: String, val pkg: String)

/** A named list of apps (FMT-1). */
data class Format(val id: String, val name: String, val packages: List<String>, val createdBy: String)

object AppListCodec {
    /** Dedupe by package, drop [excludePkg] (this app), sort by name (case-insensitive). */
    fun normalize(apps: List<AppEntry>, excludePkg: String? = null): List<AppEntry> =
        apps.filter { it.pkg.isNotBlank() && it.pkg != excludePkg }
            .distinctBy { it.pkg }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.pkg }))

    fun toItems(apps: List<AppEntry>): List<Map<String, String>> = apps.map { mapOf("n" to it.name, "p" to it.pkg) }

    fun fromItems(raw: Any?): List<AppEntry> =
        (raw as? List<*>).orEmpty().mapNotNull { m ->
            val map = m as? Map<*, *> ?: return@mapNotNull null
            val p = map["p"] as? String ?: return@mapNotNull null
            AppEntry((map["n"] as? String).orEmpty().ifBlank { p }, p)
        }

    /** Stable fingerprint so an unchanged list is not re-uploaded. */
    fun fingerprint(apps: List<AppEntry>): String = apps.joinToString("|") { it.pkg + ":" + it.name }.hashCode().toString()
}

object FormatCodec {
    const val MAX_NAME = 60
    const val MAX_PACKAGES = 500

    fun toMap(name: String, packages: Collection<String>, createdBy: String): Map<String, Any> =
        mapOf("name" to name.trim().take(MAX_NAME), "packages" to packages.distinct().take(MAX_PACKAGES), "createdBy" to createdBy)

    fun fromMap(id: String, m: Map<String, Any?>): Format? {
        val name = m["name"] as? String ?: return null
        val by = m["createdBy"] as? String ?: return null
        val pk = (m["packages"] as? List<*>).orEmpty().filterIsInstance<String>()
        return Format(id, name, pk, by)
    }

    fun isValid(name: String, packages: Collection<String>) =
        name.trim().isNotEmpty() && name.trim().length <= MAX_NAME && packages.isNotEmpty() && packages.toSet().size <= MAX_PACKAGES
}
