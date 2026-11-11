package app.meufin

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import app.meufin.ui.App
import app.meufin.ui.AppTheme
import app.meufin.ui.AppViewModel

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme(vm.themeMode) { App(vm, onUnlock = { unlock() }) }
        }
        if (vm.settings.alertsEnabled) Alerts.schedule(this, true)
    }

    private fun unlock() {
        Biometric.prompt(this, onSuccess = { vm.locked = false })
    }

    override fun onStart() {
        super.onStart()
        // Com bloqueio ativo: esconde a tela no "recentes" e trava após 20s fora do app.
        if (vm.settings.lockEnabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (stoppedAt > 0 && System.currentTimeMillis() - stoppedAt > 20_000) vm.locked = true
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        vm.syncIfStale()
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
    }
}
