package androidx.activity.compose

import androidx.compose.runtime.Composable

/** No navegador o botão voltar é gerenciado pelo histórico da página. */
@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    // Intencionalmente vazio: a navegação por abas não usa o histórico do browser.
}
