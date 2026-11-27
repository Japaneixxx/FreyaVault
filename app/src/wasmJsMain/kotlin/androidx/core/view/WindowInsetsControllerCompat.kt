package androidx.core.view

import android.app.Window
import android.view.View

/** No-op: o navegador controla a aparência da barra pelo manifest/index.html. */
class WindowInsetsControllerCompat(window: Window, view: View) {
    var isAppearanceLightStatusBars: Boolean = true
    var isAppearanceLightNavigationBars: Boolean = true
}
