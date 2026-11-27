package java.util

import com.japaneixxx.freya.web.WebBridge

object UUID {
    fun randomUUID(): String = WebBridge.randomUuid()
}
