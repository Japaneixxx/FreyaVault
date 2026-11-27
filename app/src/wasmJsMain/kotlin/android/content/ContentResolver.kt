package android.content

import android.net.Uri
import com.japaneixxx.freya.web.WebFiles
import java.io.InputStream
import java.io.OutputStream

class ContentResolver {
    fun openOutputStream(uri: Uri?): OutputStream? = uri?.let { WebFiles.output(it) }
    fun openInputStream(uri: Uri?): InputStream? = uri?.let { WebFiles.input(it) }
}
