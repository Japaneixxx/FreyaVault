package app.meufin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App(vm: AppViewModel, onUnlock: () -> Unit, onInstallUpdate: (android.net.Uri) -> Unit) {
    if (!vm.configured) {
        SetupScreen(vm)
        return
    }
    if (vm.locked) {
        LockScreen(onUnlock)
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var tabHistory by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val snack = remember { SnackbarHostState() }
    fun navigateTo(newTab: Int) {
        if (newTab == tab) return
        tabHistory = tabHistory + tab
        tab = newTab
    }
    BackHandler(enabled = tabHistory.isNotEmpty() || tab != 0) {
        if (tabHistory.isNotEmpty()) {
            tab = tabHistory.last()
            tabHistory = tabHistory.dropLast(1)
        } else {
            tab = 0
        }
    }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snack.showSnackbar(it)
            vm.message = null
        }
    }
    vm.updateRelease?.let { release ->
        AlertDialog(
            onDismissRequest = { vm.updateRelease = null },
            title = { Text("Atualização disponível") },
            text = { Text("A versão ${release.version} está disponível. O APK será baixado e o Android pedirá confirmação para instalar.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.downloadUpdate(onInstallUpdate)
                    vm.updateRelease = null
                }) { Text(if (vm.downloadingUpdate) "Baixando…" else "Atualizar") }
            },
            dismissButton = { TextButton(onClick = { vm.updateRelease = null }) { Text("Agora não") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    val colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                    NavigationBarItem(tab == 0, { navigateTo(0) }, { Icon(Icons.Default.Home, null) }, label = { Text("Início") }, colors = colors)
                    NavigationBarItem(tab == 1, { navigateTo(1) }, { Icon(Icons.Default.List, null) }, label = { Text("Extrato") }, colors = colors)
                    NavigationBarItem(tab == 2, { navigateTo(2) }, { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Planejar") }, colors = colors)
                    NavigationBarItem(tab == 3, { navigateTo(3) }, { Icon(Icons.Default.Menu, null) }, label = { Text("Mais") }, colors = colors)
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (vm.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (tab) {
                0 -> HomeScreen(vm, goPlan = { navigateTo(2) })
                1 -> ExtratoScreen(vm)
                2 -> PlanScreen(vm)
                else -> MoreScreen(vm)
            }
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) { onUnlock() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Freya", style = MaterialTheme.typography.headlineMedium)
            Text("Bloqueado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("Desbloquear") }
        }
    }
}
