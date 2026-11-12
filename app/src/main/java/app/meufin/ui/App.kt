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

    val allTxs by vm.txs.collectAsState()
    val recents = remember(allTxs) {
        allTxs.groupingBy { it.category }.eachCount().entries
            .filter { it.key !in app.meufin.data.IGNORED_CATEGORIES }
            .sortedByDescending { it.value }.map { it.key }.take(6)
    }
    var planSection by rememberSaveable { mutableIntStateOf(0) }
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

    CompositionLocalProvider(
        LocalCategoryCreator provides { vm.addCategory(it) },
        LocalRecentCategories provides recents
    ) {
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    val colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                    NavigationBarItem(tab == 0, { navigateTo(0) }, { Icon(Icons.Default.Home, null) }, label = { Text("Início") }, colors = colors)
                    NavigationBarItem(tab == 1, { navigateTo(1) }, { Icon(Icons.Default.List, null) }, label = { Text("Extrato") }, colors = colors)
<<<<<<< HEAD
                    NavigationBarItem(tab == 2, { navigateTo(2) }, { Icon(Icons.Default.AccountBox, null) }, label = { Text("Patrimônio") }, colors = colors)
                    NavigationBarItem(tab == 3, { navigateTo(3) }, { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Planejar") }, colors = colors)
                    NavigationBarItem(tab == 4, { navigateTo(4) }, { Icon(Icons.Default.Menu, null) }, label = { Text("Mais") }, colors = colors)
=======
                    NavigationBarItem(tab == 2, { navigateTo(2) }, { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Planejar") }, colors = colors)
                    NavigationBarItem(tab == 3, { navigateTo(3) }, { Icon(Icons.Default.Menu, null) }, label = { Text("Mais") }, colors = colors)
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (vm.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (tab) {
<<<<<<< HEAD
                0 -> HomeScreen(
                    vm,
                    goPlan = { navigateTo(3) },
                    goAlerts = { planSection = 6; navigateTo(3) }
                )
=======
                0 -> HomeScreen(vm, goPlan = { navigateTo(2) })
>>>>>>> ca3862f5ab875d29627bcebfbf222fb3e17f0ba8
                1 -> ExtratoScreen(vm)
                2 -> NetWorthScreen(vm)
                3 -> PlanScreen(vm, planSection) { planSection = it }
                else -> MoreScreen(vm)
            }
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
