package com.imrat.focuslock.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A remote device the user added: the name they chose, and the account it points to (null when the account was not found). */
data class RemoteEntry(val id: String, val name: String, val targetUid: String?)

/** Device list kept on this phone for one signed-in account: "This device" flag + named remote devices. */
class DeviceStore(ctx: Context, private val owner: String) {
    private val prefs = ctx.getSharedPreferences("focuslock_devices", Context.MODE_PRIVATE)
    private val _mine = MutableStateFlow(prefs.getBoolean("mine:$owner", false))
    private val _remotes = MutableStateFlow(load())

    val myDevice: StateFlow<Boolean> get() = _mine
    val remotes: StateFlow<List<RemoteEntry>> get() = _remotes

    fun setMyDevice(added: Boolean) {
        prefs.edit().putBoolean("mine:$owner", added).apply()
        _mine.value = added
    }

    fun addRemote(name: String, targetUid: String?) {
        val id = targetUid ?: "pending-" + System.currentTimeMillis()
        save(_remotes.value.filterNot { it.id == id } + RemoteEntry(id, name.trim(), targetUid))
    }

    fun removeRemote(id: String) = save(_remotes.value.filterNot { it.id == id })

    private fun save(list: List<RemoteEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("uid", it.targetUid ?: JSONObject.NULL)) }
        prefs.edit().putString("remotes:$owner", arr.toString()).apply()
        _remotes.value = list
    }

    private fun load(): List<RemoteEntry> = runCatching {
        val arr = JSONArray(prefs.getString("remotes:$owner", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            RemoteEntry(o.getString("id"), o.getString("name"), if (o.isNull("uid")) null else o.getString("uid"))
        }
    }.getOrDefault(emptyList())
}
