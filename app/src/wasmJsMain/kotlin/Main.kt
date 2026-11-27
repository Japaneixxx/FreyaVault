import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.japaneixxx.freya.ui.App
import com.japaneixxx.freya.ui.AppTheme
import com.japaneixxx.freya.ui.AppViewModel

/** O texto de carregamento fica no HTML; o ComposeViewport apenas acrescenta o canvas. */
@JsFun("() => { const b = document.querySelector('.boot'); if (b) b.remove(); const r = document.getElementById('root'); if (r) r.innerHTML = ''; }")
private external fun clearBootOverlay()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    clearBootOverlay()
    val vm = AppViewModel()
    ComposeViewport(viewportContainerId = "root") {
        AppTheme(mode = 2) {
            Box(Modifier.fillMaxSize()) {
                App(
                    vm = vm,
                    onUnlock = {},
                    onInstallUpdate = {},
                    onEnableAlerts = {}
                )
                Text(
                    "BETA WEB",
                    modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
