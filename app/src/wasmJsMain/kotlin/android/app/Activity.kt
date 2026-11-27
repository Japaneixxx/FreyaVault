package android.app

import android.content.Context

open class Activity : Context() {
    open val window: Window = Window()

    fun finish() {}
}

class Window {
    var statusBarColor: Int = 0
    var navigationBarColor: Int = 0
    val decorView: android.view.View = android.view.View(Context())
}
