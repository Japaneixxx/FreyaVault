package com.japaneixxx.freya

import android.content.Context
import androidx.fragment.app.FragmentActivity

object Biometric {
    fun available(ctx: Context): Boolean = false

    fun prompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        onError("Biometria não está disponível na versão web.")
    }
}
