package android.content

import com.japaneixxx.freya.web.WebBridge
import org.json.JSONObject

internal class WebPreferences(private val name: String) : SharedPreferences {
    private val data = LinkedHashMap<String, Any?>()
    private val storageKey = "freya.prefs.$name"

    init {
        runCatching {
            val raw = WebBridge.storageGet(storageKey)
            if (!raw.isNullOrBlank()) {
                val o = JSONObject(raw)
                o.keys().forEach { key -> data[key] = o.opt(key) }
            }
        }
    }

    override val all: Map<String, Any?> get() = data.toMap()

    override fun contains(key: String): Boolean = data.containsKey(key)

    override fun getString(key: String, defValue: String?): String? {
        val v = data[key] ?: return defValue
        return when (v) {
            is String -> v
            is Number -> v.toString()
            is Boolean -> v.toString()
            else -> defValue
        }
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        when (val v = data[key]) {
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true)
            else -> defValue
        }

    override fun getLong(key: String, defValue: Long): Long =
        when (val v = data[key]) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: defValue
            else -> defValue
        }

    override fun getInt(key: String, defValue: Int): Int =
        when (val v = data[key]) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: defValue
            else -> defValue
        }

    override fun edit(): SharedPreferences.Editor = EditorImpl()

    private fun persist() {
        val o = JSONObject()
        data.forEach { (k, v) -> o.put(k, v) }
        WebBridge.storageSet(storageKey, o.toString())
    }

    internal fun clearAll() {
        data.clear()
        WebBridge.storageRemove(storageKey)
    }

    private inner class EditorImpl : SharedPreferences.Editor {
        private val changes = LinkedHashMap<String, Any?>()
        private val removals = HashSet<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            changes[key] = value; removals.remove(key); return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            changes[key] = value; removals.remove(key); return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            changes[key] = value; removals.remove(key); return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            changes[key] = value; removals.remove(key); return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            removals += key; changes.remove(key); return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true; changes.clear(); removals.clear(); return this
        }

        override fun apply() {
            if (clearAll) data.clear()
            removals.forEach { data.remove(it) }
            changes.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v }
            persist()
        }

        override fun commit(): Boolean {
            apply(); return true
        }
    }

    companion object {
        private val cache = HashMap<String, WebPreferences>()
        fun get(name: String): WebPreferences = cache.getOrPut(name) { WebPreferences(name) }
        fun delete(name: String) {
            cache.remove(name)?.clearAll()
            WebBridge.storageRemove("freya.prefs.$name")
        }
    }
}
