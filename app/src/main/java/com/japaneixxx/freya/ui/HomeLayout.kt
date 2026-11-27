package com.japaneixxx.freya.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Um bloco que pode aparecer na tela inicial. O cartão de saldo no topo é fixo e não entra aqui. */
data class HomeBlockDef(val id: String, val title: String, val hint: String)

val HOME_BLOCKS = listOf(
    HomeBlockDef("agenda", "Agenda", "Próximos vencimentos e recebimentos"),
    HomeBlockDef("alerts", "Atenção", "Cobranças duplicadas ou mais caras (só aparece quando há avisos)"),
    HomeBlockDef("cards", "Faturas dos cartões", "Fatura aberta de cada cartão e quando fecha"),
    HomeBlockDef("month", "Resumo do mês", "Troca de mês, receitas, despesas e resultado"),
    HomeBlockDef("insights", "Dicas", "Observações sobre o seu mês"),
    HomeBlockDef("categories", "Para onde foi o dinheiro", "Gastos por categoria"),
    HomeBlockDef("heatmap", "Calendário de gastos", "Cor de cada dia: mais saídas ou mais entradas"),
    HomeBlockDef("pace", "Ritmo de gastos", "Este mês contra o anterior"),
    HomeBlockDef("trend", "Evolução do saldo", "Histórico do saldo em contas"),
    HomeBlockDef("budgets", "Orçamentos", "Categorias mais perto do limite"),
    HomeBlockDef("last6", "Últimos 6 meses", "Receitas e despesas por mês"),
    HomeBlockDef("top", "Maiores gastos do mês", "Os 5 maiores lançamentos")
)

data class HomeBlock(val id: String, val visible: Boolean)

/** Ordem padrão: tudo visível, exceto os blocos novos que começam desligados para não mudar o início de quem já usa. */
private val DEFAULT_HIDDEN = setOf("cards")

fun defaultHomeLayout(): List<HomeBlock> = HOME_BLOCKS.map { HomeBlock(it.id, it.id !in DEFAULT_HIDDEN) }

/** Lê "id:1,id:0". Ignora ids desconhecidos e repetidos; blocos que faltam entram no fim com o padrão. */
fun parseHomeLayout(raw: String): List<HomeBlock> {
    val known = HOME_BLOCKS.map { it.id }.toSet()
    val seen = mutableSetOf<String>()
    val out = mutableListOf<HomeBlock>()
    raw.split(",").forEach { part ->
        val bits = part.trim().split(":")
        val id = bits.firstOrNull().orEmpty()
        if (id in known && seen.add(id)) out += HomeBlock(id, bits.getOrNull(1) != "0")
    }
    defaultHomeLayout().filter { it.id !in seen }.forEach { out += it }
    return out
}

fun encodeHomeLayout(layout: List<HomeBlock>): String = layout.joinToString(",") { it.id + ":" + if (it.visible) "1" else "0" }

/** Troca o bloco em [index] com o vizinho ([delta] = -1 sobe, +1 desce). */
fun moveHomeBlock(layout: List<HomeBlock>, index: Int, delta: Int): List<HomeBlock> {
    val to = index + delta
    if (index !in layout.indices || to !in layout.indices) return layout
    return layout.toMutableList().also { val a = it[index]; it[index] = it[to]; it[to] = a }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCustomizeSheet(layout: List<HomeBlock>, onChange: (List<HomeBlock>) -> Unit, onClose: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Personalizar início", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Ligue ou desligue os blocos e use as setas para mudar a ordem. O cartão de saldo no topo sempre aparece.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f, fill = false)) {
                itemsIndexed(layout, key = { _, b -> b.id }) { i, b ->
                    val def = HOME_BLOCKS.first { it.id == b.id }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(def.title, style = MaterialTheme.typography.bodyLarge)
                            Text(def.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onChange(moveHomeBlock(layout, i, -1)) }, enabled = i > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, "Subir ${def.title}")
                        }
                        IconButton(onClick = { onChange(moveHomeBlock(layout, i, 1)) }, enabled = i < layout.lastIndex) {
                            Icon(Icons.Default.KeyboardArrowDown, "Descer ${def.title}")
                        }
                        Switch(checked = b.visible, onCheckedChange = { on ->
                            onChange(layout.map { if (it.id == b.id) it.copy(visible = on) else it })
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onChange(defaultHomeLayout()) }, modifier = Modifier.fillMaxWidth()) { Text("Restaurar o padrão") }
        }
    }
}
