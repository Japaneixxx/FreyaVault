package app.meufin.ui

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
fun App(vm: AppViewModel, onUnlock: () -> Unit) {
    if (!vm.configured) {
        SetupScreen(vm)
        return
    }
    if (vm.locked) {
        LockScreen(onUnlock)
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snack.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.Home, null) }, label = { Text("Início") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.List, null) }, label = { Text("Extrato") })
                NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Planejar") })
                NavigationBarItem(tab == 3, { tab = 3 }, { Icon(Icons.Default.Menu, null) }, label = { Text("Mais") })
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (vm.syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (tab) {
                0 -> HomeScreen(vm, goPlan = { tab = 2 })
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
