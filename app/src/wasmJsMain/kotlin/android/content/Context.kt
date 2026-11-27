package android.content

import android.net.Uri
import com.japaneixxx.freya.web.WebBridge

open class Context {
    val contentResolver: ContentResolver = ContentResolver()
    val applicationContext: Context get() = this

    open fun startActivity(intent: Intent) {
        when {
            intent.action == Intent.ACTION_VIEW && intent.data != null ->
                WebBridge.openUrl(intent.data!!.toString())
            intent.action == Intent.ACTION_SEND ->
                WebBridge.share(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
        }
    }

    fun getSharedPreferences(name: String, mode: Int): SharedPreferences = WebPreferences.get(name)

    fun deleteSharedPreferences(name: String): Boolean {
        WebPreferences.delete(name)
        return true
    }

    fun getString(resId: Int): String = com.japaneixxx.freya.R.string.of(resId)
}
