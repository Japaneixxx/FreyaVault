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
        if (vm.settings.alertsEnabled) Alerts.schedule(th