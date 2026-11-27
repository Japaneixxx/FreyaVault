package androidx.activity.result.contract

import android.net.Uri
import com.japaneixxx.freya.web.WebBridge
import com.japaneixxx.freya.web.WebFiles

abstract class ActivityResultContract<I, O> {
    abstract fun execute(input: I, callback: (O) -> Unit)
}

class ActivityResultContracts {
    class CreateDocument(private val mime: String) : ActivityResultContract<String, Uri?>() {
        override fun execute(input: String, callback: (Uri?) -> Unit) {
            callback(WebFiles.createOutput(input, mime))
        }
    }

    class OpenDocument : ActivityResultContract<Array<String>, Uri?>() {
        override fun execute(input: Array<String>, callback: (Uri?) -> Unit) {
            WebBridge.openFile(input.joinToString(",")) { text ->
                callback(if (text == null) null else WebFiles.createInput(text.encodeToByteArray()))
            }
        }
    }

    class RequestPermission : ActivityResultContract<String, Boolean>() {
        override fun execute(input: String, callback: (Boolean) -> Unit) {
            callback(true)
        }
    }

    class RequestMultiplePermissions : ActivityResultContract<Array<String>, Map<String, Boolean>>() {
        override fun execute(input: Array<String>, callback: (Map<String, Boolean>) -> Unit) {
            callback(input.associateWith { true })
        }
    }
}
