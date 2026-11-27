package android.content

import android.net.Uri

class Intent(val action: String? = null, val data: Uri? = null) {
    var type: String? = null
    private val extras = HashMap<String, Any?>()

    fun setType(value: String?): Intent { type = value; return this }
    fun putExtra(key: String, value: String?): Intent { extras[key] = value; return this }
    fun putExtra(key: String, value: Int): Intent { extras[key] = value; return this }
    fun getStringExtra(key: String): String? = extras[key] as? String
    fun getIntExtra(key: String, default: Int): Int = extras[key] as? Int ?: default
    fun setData(uri: Uri?): Intent { return this }
    fun setDataAndType(uri: Uri?, type: String?): Intent { this.type = type; return this }
    fun addFlags(flags: Int): Intent = this

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val FLAG_ACTIVITY_SINGLE_TOP = 0x20000000

        fun createChooser(target: Intent, title: CharSequence?): Intent = target
    }
}
