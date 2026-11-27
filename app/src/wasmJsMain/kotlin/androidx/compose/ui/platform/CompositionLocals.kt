package androidx.compose.ui.platform

import androidx.compose.runtime.compositionLocalOf
import android.content.Context
import android.view.View
import com.japaneixxx.freya.web.WebContext

val LocalContext = compositionLocalOf<Context> { WebContext.current }
val LocalView = compositionLocalOf<View> { View(WebContext.current) }
