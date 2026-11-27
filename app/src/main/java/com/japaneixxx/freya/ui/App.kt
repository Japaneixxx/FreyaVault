package com.japaneixxx.freya.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun App(
    vm: AppViewModel,
    onUnlock: () -> Unit,
    onInstallUpdate: (android.net.Uri) -> Unit,
    onEnableAlerts: () -> Unit
) {
    if (!vm.configured) {
        SetupScreen(vm)
        return
    }
    if (vm.locked) {
        LockScreen(vm.pinEnabled, onUnlock, { vm.settings.pinLockRemainingMs() }) { vm.unlockWithPin(it) }
        return
    }

    val allTxs by vm.txs.collectAsState()
    val recents = remember(allTxs) {
        allTxs.groupingBy { it.category }.eachCount().entries
            .filter { it.key !in com.japaneixxx.freya.data.IGNORED_CATEGORIES }
            .sortedByDescending { it.value }.map { it.key }.take(6)
    }
    var planSection by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(vm.startTab) }
    var tabHistory by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val snack = remember { SnackbarHostState() }
    var showAlertPrompt by remember { mutableStateOf(!vm.settings.alertPromptShown) }
    fun navigateTo(newTab: Int) {
        if (newTab == tab) return
        tabHistory = tabHistory + tab
        tab = newTab
    }
    BackHandler(enabled = tabHistory.isNotEmpty() || tab != vm.startTab) {
        if (tabHistory.isNotEmpty()) {
            tab = tabHistory.last()
            tabHistory = tabHistory.dropLast(1)
        } else {
            tab = vm.startTab
        }
    }
    // Atalho do ícone, widget: abre a aba, a seção do Planejar e a ação pedidas (só depois de desbloquear).
    LaunchedEffect(vm.pendingLink) {
        val link = vm.pendingLink ?: return@LaunchedEffect
        vm.pendingLink = null
        link.planSection?.let { planSection = it }
        navigateTo(link.tab)
        when (link.action) {
            "add" -> vm.addRequest = true
            "sync" -> vm.sync()
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
            title = { Text(if (release.beta) "Versão beta disponível" else "Atualização disponível") },
            text = {
                Text(
                    "A versão ${release.version}" + (if (release.beta) " (beta)" else "") +
                        " está disponível. " + (if (release.beta) "Versões beta podem ter instabilidades. " else "") +
                        "O APK será baixado e o Android pedirá confirmação para instalar."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.downloadUpdate(onInstallUpdate)
                    vm.updateRelease = null
                }) { Text(if (vm.downloadingUpdate) "Baixando…" else "Atualizar") }
            },
            dismissButton = { TextButton(onClick = { vm.updateRelease = null }) { Text("Agora não") } }
        )
    }

    vm.whatsNewFrom?.let { from ->
        ChangelogSheet(notesSince(from, vm.currentVersion), "Novidades do Freya", vm.currentVersion) { vm.dismissWhatsNew() }
    }
    if (showAlertPrompt) {
        AlertDialog(
            onDismissRequest = {
                vm.settings.alertPromptShown = true
                showAlertPrompt = false
            },
            title = { Text("Ativar alertas?") },
            text = { Text("O Freya pode avisar sobre contas, faturas e orçamentos próximos do limite.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.settings.alertPromptShown = true
                    showAlertPrompt = false
                    onEnableAlerts()
                }) { Text("Ativar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.settings.alertPromptShown = true
                    showAlertPrompt = false
                }) { Text("Agora não") }
            }
        )
    }

    // Tutorial: só roda depois dos avisos iniciais, para um diálogo não ficar por cima do outro.
    val tourState = remember { TourState() }
    val tourRunning = vm.tourActive && !showAlertPrompt && vm.whatsNewFrom == null && vm.updateRelease == null
    LaunchedEffect(tab, vm.tourStep, tourRunning) {
        if (!tourRunning) return@LaunchedEffect
        val step = TOUR_STEPS.getOrNull(vm.tourStep) ?: return@LaunchedEffect
        if (step.tapTab != null) {
            if (tab == step.tapTab) vm.tourStep += 1
        } else if (step.tab != null && tab != step.tab) {
            tab = step.tab
        }
    }
    BackHandler(enabled = tourRunning) { vm.finishTour(); tab = 0 }

    CompositionLocalProvider(
        LocalCategoryCreator provides { vm.addCategory(it) },
        LocalRecentCategories provides recents,
        LocalTour provides (if (tourRunning) tourState else null)
    ) {
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    val colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                    val icons = listOf(Icons.Default.Home, Icons.Default.List, Icons.Default.AccountBox, Icons.Default.CheckCircle, Icons.Default.Menu)
                    vm.tabOrder.forEach { id ->
                        NavigationBarItem(
                            tab == id, { navigateTo(id) }, { NavIcon(icons[id], tab == id) },
                            label = { Text(TAB_LABELS[id]) }, colors = colors, modifier = Modifier.tourTarget("nav_$id")
                        )
                    }
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            AnimatedVisibility(
                visible = vm.syncing,
                enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = vm.tabOrder.indexOf(targetState) > vm.tabOrder.indexOf(initialState)
                    val enter = fadeIn(tween(260)) +
                        slideInHorizontally(tween(320, easing = Motion.EaseOut)) { w ->
                            if (forward) w / 7 else -w / 7
                        }
                    val exit = fadeOut(tween(150)) +
                        slideOutHorizontally(tween(240, easing = Motion.EaseOut)) { w ->
                            if (forward) -w / 12 else w / 12
                        }
                    enter togetherWith exit
                },
                label = "tabContent",
                modifier = Modifier.fillMaxSize()
            ) { current ->
                when (current) {
                    0 -> HomeScreen(
                        vm,
                        goPlan = { navigateTo(3) },
                        goAlerts = { planSection = PlanSection.CHARGES; navigateTo(3) },
                        goAgenda = { planSection = PlanSection.AGENDA; navigateTo(3) },
                        goCards = { planSection = PlanSection.CARDS; navigateTo(3) }
                    )
                    1 -> ExtratoScreen(vm)
                    2 -> NetWorthScreen(vm)
                    3 -> PlanScreen(vm, planSection) { planSection = it }
                    else -> MoreScreen(vm)
                }
            }
        }
    }
    if (tourRunning) {
        val step = TOUR_STEPS.getOrNull(vm.tourStep)
        if (step == null) {
            vm.finishTour()
        } else {
            TourOverlay(
                step = step, index = vm.tourStep, total = TOUR_STEPS.size, state = tourState,
                onNext = { if (vm.tourStep >= TOUR_STEPS.lastIndex) { vm.finishTour(); tab = 0 } else vm.tourStep += 1 },
                onSkip = { vm.finishTour(); tab = 0 }
            )
        }
    }
    }
    }
}

@Composable
private fun NavIcon(icon: ImageVector, selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 650f),
        label = "navIcon"
    )
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    )
}

@Composable
private fun LockScreen(hasPin: Boolean, onUnlock: () -> Unit, pinWaitMs: () -> Long, onPinUnlock: (String) -> Boolean) {
    var pin by rememberSaveable { mutableStateOf("") }
    var pinError by rememberSaveable { mutableStateOf(false) }
    // Depois de várias tentativas erradas o PIN trava por um tempo; contagem regressiva na tela.
    var waitSecs by remember { mutableIntStateOf(((pinWaitMs() + 999) / 1000).toInt()) }
    LaunchedEffect(waitSecs) {
        if (waitSecs > 0) {
            kotlinx.coroutines.delay(1000)
            waitSecs = ((pinWaitMs() + 999) / 1000).toInt()
        }
    }
    LaunchedEffect(Unit) { onUnlock() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lock,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp).pulse()
            )
            Spacer(Modifier.height(16.dp))
            Text("Freya", style = MaterialTheme.typography.headlineMedium)
            Text("Bloqueado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onUnlock) { Text("Desbloquear") }
            if (hasPin) {
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(4)
                        pinError = false
                    },
                    label = { Text("PIN de 4 dígitos") },
                    singleLine = true,
                    isError = pinError,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                if (waitSecs > 0) {
                    Text("Muitas tentativas erradas. Tente de novo em ${waitSecs}s.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                } else if (pinError) {
                    Text("PIN incorreto", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        pinError = !onPinUnlock(pin)
                        waitSecs = ((pinWaitMs() + 999) / 1000).toInt()
                        if (pinError) pin = ""
                    },
                    enabled = pin.length == 4 && waitSecs == 0
                ) { Text("Entrar com PIN") }
            }
        }
    }
}
