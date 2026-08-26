package id.kopikontrol.app.data

import android.content.Context
import org.json.JSONObject

class SessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("kopikontrol_native_session", Context.MODE_PRIVATE)

    fun cookieHeader(): String {
        val stored = preferences.getString("cookies", "{}") ?: "{}"
        val json = runCatching { JSONObject(stored) }.getOrElse { JSONObject() }
        return json.keys().asSequence().joinToString("; ") { key -> "$key=${json.optString(key)}" }
    }

    fun mergeSetCookies(headers: List<String>) {
        if (headers.isEmpty()) return
        val current = runCatching { JSONObject(preferences.getString("cookies", "{}") ?: "{}") }
            .getOrElse { JSONObject() }
        headers.forEach { header ->
            val pair = header.substringBefore(';')
            val separator = pair.indexOf('=')
            if (separator <= 0) return@forEach
            val name = pair.substring(0, separator).trim()
            val value = pair.substring(separator + 1).trim()
            if (value.isBlank()) current.remove(name) else current.put(name, value)
        }
        preferences.edit().putString("cookies", current.toString()).apply()
    }

    fun clear() = preferences.edit().remove("cookies").apply()
}
