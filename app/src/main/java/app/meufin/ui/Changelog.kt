package app.meufin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Uma versão do app e o que mudou nela, em linguagem de quem usa. */
data class ReleaseNote(val version: String, val title: String, val items: List<String>)

/**
 * Histórico de versões, da mais nova para a mais antiga.
 * Ao publicar uma versão nova, adicione a entrada dela no TOPO desta lista (a mesma do `versionName`).
 */
val releaseNotes: List<ReleaseNote> = listOf(
    ReleaseNote(
        "1.6.0", "Compromissos e previsão de salário",
        listOf(
            "Compromissos: configure contas fixas (internet), variáveis (luz, água) e financiamentos com parcelas definidas.",
            "Contas variáveis usam a média dos últimos pagamentos como estimativa e pedem o valor real ao marcar como paga.",
            "Financiamentos mostram a parcela atual, quantas faltam, quanto ainda resta pagar e o mês em que terminam.",
            "Baixa automática: informe uma palavra do extrato (ex.: enel) e o app marca a conta como paga quando o débito aparecer.",
            "Receitas previstas: cadastre seu salário por dia fixo ou N-ésimo dia útil (ex.: 5º dia útil).",
            "Agenda: linha do tempo dos próximos 45 dias com entradas, contas e faturas, e o saldo previsto depois de cada uma. O app avisa se o saldo vai ficar negativo.",
            "Alerta no dia do salário previsto e previsão de saldo no fim do mês no Início.",
            "Novidades: este histórico de versões no menu Mais e um resumo automático na primeira abertura depois de atualizar."
        )
    ),
    ReleaseNote(
        "1.5.0", "Nomes limpos, patrimônio e envelopes",
        listOf(
            "Nomes limpos no extrato: o texto repetido do banco sai e entram sinalizações (💳 Débito, ⚡ Pix, 🧾 Boleto, 🧩 parcela).",
            "Cobranças para revisar: detecta cobranças duplicadas e assinaturas que ficaram mais caras.",
            "Nova aba Patrimônio: patrimônio líquido, evolução, investimentos do Pluggy e bens e dívidas cadastrados à mão.",
            "Orçamentos em modo envelope: a sobra de cada mês passa para o seguinte.",
            "Seletor de categorias em grade, com busca, mais usadas e criação na hora.",
            "Tutorial de conexão com o Pluggy refeito, com teste da conexão antes de salvar."
        )
    ),
    ReleaseNote(
        "1.4.0", "Atualização pelo próprio app",
        listOf(
            "O app verifica novas versões e instala a atualização por conta própria.",
            "Ajustes nas notificações e no botão voltar entre as abas."
        )
    ),
    ReleaseNote(
        "1.3.0", "Redesign nórdico",
        listOf(
            "Novo visual em todo o app, claro e escuro, com montanhas no topo do Início.",
            "Extrato reformulado: cartões por dia, filtros em chips, seleção em lote e detalhe em painel.",
            "Calendário de gastos, carrossel de insights e categorias personalizadas."
        )
    ),
    ReleaseNote(
        "1.2.0", "Segurança, alertas e widget",
        listOf(
            "Bloqueio por biometria ou PIN e modo para ocultar valores.",
            "Alertas de orçamento, contas a vencer e faturas de cartão.",
            "Widget de saldo, contas a pagar, cartões, parcelas detectadas e backup em JSON."
        )
    ),
    ReleaseNote(
        "1.1.0", "Planejamento",
        listOf(
            "Orçamentos por categoria, metas de poupança e cobranças recorrentes.",
            "Lançamentos manuais, insights automáticos e exportação em CSV."
        )
    ),
    ReleaseNote(
        "1.0.0", "Primeira versão",
        listOf(
            "Sincronização direta com o Pluggy (Meu Pluggy), sem servidor.",
            "Resumo do mês, extrato e categorias automáticas."
        )
    )
)

private fun versionParts(v: String): List<Int> = v.trim().removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }

/** true se [a] é uma versão mais nova que [b]. */
fun isNewerVersion(a: String, b: String): Boolean {
    val x = versionParts(a); val y = versionParts(b)
    for (i in 0 until maxOf(x.size, y.size)) {
        val p = x.getOrElse(i) { 0 }; val q = y.getOrElse(i) { 0 }
        if (p != q) return p > q
    }
    return false
}

/**
 * Notas das versões que o usuário ainda não viu: mais novas que [seen] e até a versão instalada.
 * Sem versão anterior conhecida, mostra só a da versão instalada.
 */
fun notesSince(seen: String, current: String): List<ReleaseNote> {
    val upToCurrent = releaseNotes.filter { !isNewerVersion(it.version, current) }
    if (seen.isBlank()) return upToCurrent.take(1)
    return upToCurrent.filter { isNewerVersion(it.version, seen) }
}

/** Painel com as notas de versão (histórico completo no menu, ou só o que é novo após atualizar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogSheet(notes: List<ReleaseNote>, title: String, currentVersion: String, onClose: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Você está na versão $currentVersion",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notes, key = { it.version }) { n ->
                    NCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("v${n.version}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            if (n.version == currentVersion) {
                                Spacer(Modifier.width(8.dp))
                                Pill("instalada")
                            }
                        }
                        Text(n.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
                        n.items.forEach {
                            Text("•  $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Entendi") }
        }
    }
}
