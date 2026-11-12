package com.japaneixxx.freya

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.japaneixxx.freya.ui.App
import com.japaneixxx.freya.ui.AppTheme
import com.japaneixxx.freya.ui.AppViewModel

class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()
    private var stoppedAt = 0L
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && Alerts.notificationsEnabled(this)) {
            vm.setAlerts(true)
        } else {
            Alerts.openNotificationSettings(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme(vm.themeMode) {
                App(
                    vm,
                    onUnlock = { unlock() },
                    onInstallUpdate = { installUpdate(it) },
                    onEnableAlerts = { requestAlertsPermission() }
                )
            }
        }
        if (vm.settings.alertsEnabled) Alerts.schedule(this, true)
    }

    private fun unlock() {
        Biometric.prompt(this, onSuccess = { vm.locked = false })
    }

    override fun onStart() {
        super.onStart()
        if (vm.settings.lockEnabled && stoppedAt > 0 && System.currentTimeMillis() - stoppedAt > 20_000) {
            vm.locked = true
        }

        vm.syncIfStale()
        if (vm.checkUpdatesOnOpen) vm.checkForUpdate(showNoUpdate = false)
    }

    private fun requestAlertsPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            vm.setAlerts(true)
            return
        }
        if (Alerts.canNotify(this)) {
            if (Alerts.notificationsEnabled(this)) vm.setAlerts(true)
            else Alerts.openNotificationSettings(this)
            return
        }
        window.decorView.post {
            runCatching {
                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }.onFailure {
                vm.message = "Não foi possível solicitar a permissão de notificações"
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
    }

    private fun installUpdate(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !packageManager.canRequestPackageInstalls()
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }
}
