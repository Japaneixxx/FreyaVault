package com.japaneixxx.freya.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Uma versão do app e o que mudou nela, em linguagem de quem usa. */
data class ReleaseNote(
    val version: String,
    val title: String,
    val items: List<String>,
    val codename: String? = null
)

/**
 * Histórico de versões, da mais nova para a mais antiga.
 * Ao publicar uma versão nova, adicione a entrada dela no TOPO desta lista (a mesma do `versionName`).
 */
val releaseNotes: List<ReleaseNote> = listOf(
    ReleaseNote(
        "1.11.0", "Freya na web",
        listOf("Versão web (PWA) em beta: abra o Freya no navegador, com os mesmos dados e interface do app. No celular, use o aplicativo para notificações, agenda, widget e atualizações.")
    ),
    ReleaseNote(
        "1.10.5", "Retrospectiva mensal e anual",
        listOf("Veja, quando quiser em Mais, um resumo discreto das receitas, despesas e categorias do mês ou do ano. Os dados ficam no aparelho e não geram notificações.")
    ),
    ReleaseNote(
        "1.10.4", "Gráficos interativos",
        listOf("Toque ou arraste nos gráficos para consultar os valores de cada ponto, mês ou categoria.")
    ),
    ReleaseNote(
        "1.10.3", "Recebimentos avulsos",
        listOf(
            "Corrigido um erro que repetia o mesmo recebimento avulso na agenda.",
            "Agora é possível escolher várias datas ao cadastrar um recebimento avulso; o valor informado vale para cada dia."
        )
    ),
    ReleaseNote(
        "1.10.2", "Segurança configurável",
        listOf("A proteção contra capturas e gravações de tela agora tem um controle separado do bloqueio por biometria ou PIN.")
    ),
    ReleaseNote(
        "1.10.1", "Correção na compilação",
        listOf("Corrigido um erro que impedia a compilação do aplicativo.")
    ),
    ReleaseNote(
        "1.10.0", "Agenda, faturas por ciclo e o app do seu jeito",
        listOf(
            "Mais > Agenda do celular: o Freya cria lembretes com o vencimento das faturas dos cartões (e, se quiser, das contas a pagar) num calendário local \"Freya\", com aviso antes e no dia. Os eventos são atualizados sozinhos quando a fatura muda ou é paga. O valor só vai para o evento se você ligar a opção; com valores ocultos ou bloqueio ativo ele nunca é escrito.",
            "O Calendário de gastos ficou maior: cada dia mostra o saldo (entradas menos saídas), vermelho quando saiu mais e verde-azulado quando entrou mais, com a cor mais forte nos dias mais extremos. Toque num dia para ver quanto entrou, quanto saiu e os lançamentos dele.",
            "Planejar > Cartões: cada cartão ganhou a fatura por ciclo. Informe o dia de fechamento (ou use o que o banco enviar) e veja o ciclo atual e os 5 anteriores, com total, categorias e lançamentos de cada um. O total vem dos lançamentos do extrato e pode diferir do valor oficial da fatura; compras no dia do fechamento entram na fatura seguinte.",
            "Início personalizável: toque em Personalizar início, no fim da tela, para ligar, desligar e reordenar os blocos. Há também o novo bloco Faturas dos cartões, que começa desligado.",
            "Tutorial interativo no primeiro acesso: o app destaca cada área e pede que você toque nas abas para conhecer. Dá para pular, e rever em Mais > Ajuda.",
            "Mais > Aparência: escolha a cor de destaque entre 8 paletas ou monte a sua com os controles de cor livre; o app ajusta a claridade para o texto continuar legível nos temas claro e escuro. Também dá para mudar o tamanho do texto (80% a 140%) e a densidade (compacta, padrão ou confortável).",
            "Mais > Abas, atalhos e widget: escolha a aba que abre com o app e a ordem da barra de baixo. Segurando o ícone do Freya você vai direto a Novo lançamento, Agenda, Faturas dos cartões, Orçamentos, Extrato, Patrimônio ou Sincronizar, até 4 por vez.",
            "O widget de saldo agora é configurável: mostra contas, faturas ou os dois, tem estilo da cor do app, escuro, claro ou transparente, e abre o destino que você escolher. A cor do app no widget só vale no Android 12 ou mais novo."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.9.0", "Dinheiro físico e dados mais seguros",
        listOf(
            "Nova conta de dinheiro físico em Mais: crie uma carteira, registre entradas e saídas pelo + do Extrato escolhendo a conta, e ajuste o saldo quando quiser. O saldo entra no total e no patrimônio.",
            "Corrigido: o calendário dos recebimentos avulsos não abria ao tocar na data (e a data escolhida podia ficar um dia antes). O Extrato também ganhou o calendário.",
            "Sincronização mais confiável: se um banco falha, os outros continuam e o aviso diz qual foi; cada banco é gravado por inteiro ou nada; bancos removidos não somam mais no patrimônio; novas tentativas automáticas quando o Pluggy está instável.",
            "Backup melhorado: inclui histórico dos gráficos, categorias próprias e contas de dinheiro; pode ser protegido com senha (AES-256); restaura com Mesclar ou Substituir e, se algo falhar, nada é alterado. O app lembra a cada 30 dias de fazer um backup.",
            "Segurança: o PIN agora usa hash lento e trava por um tempo após 5 erros; valores e bloqueio ligados escondem o app de capturas de tela e dos apps recentes; o app não trava mais se o armazenamento seguro do aparelho corromper.",
            "Atualizações: o APK baixado é conferido pelo SHA-256 publicado na Release, e a verificação funciona mesmo antes de configurar o Pluggy.",
            "Categorias: palavras-chave só casam no início de palavras (acabou \"uber\" dentro de \"tuberculose\"), Mercado Livre vira Compras e categorias do Pluggy em inglês não aparecem mais.",
            "Transferências entre suas próprias contas (Pix/TED de mesmo valor, até 1 dia de diferença) são reconhecidas e não contam como despesa e receita. Se errar, escolha a categoria à mão no lançamento.",
            "Somas em moeda estrangeira não viram reais, e os totais são arredondados em centavos.",
            "Corrigido: a tela de tutorial inicial mostrava os textos e cartões uns em cima dos outros."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.8.5", "Versões beta",
        listOf(
            "Versões beta (final 1, 3 ou 5 no número, como 1.8.41, 1.8.43 e 1.8.45) só aparecem ao tocar em Verificar atualizações.",
            "A verificação automática ao abrir o app continua oferecendo apenas versões estáveis."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.8.46", "Bancos mais frescos",
        listOf(
            "Em Mais, o Freya agora mostra quando o Meu Pluggy leu os bancos pela última vez e avisa quando passa de um dia.",
            "Novo botão para abrir o Meu Pluggy e atualizar os bancos na hora; ao voltar, o Freya sincroniza sem esperar o intervalo mínimo."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.8.45", "Movimento e luz",
        listOf(
            "Transições suaves entre abas e seções, com entrada em cascata dos cartões.",
            "Saldos, totais e resultados contam até o valor final, e os gráficos crescem ao abrir.",
            "O cartão de saldo ganhou gradiente com brilho suave e montanhas ao fundo.",
            "Barras de progresso e a divisão receita × despesa agora se animam, e os cartões respondem ao toque com elasticidade.",
            "Seleção de lançamentos, agenda, metas e simulador de investimento também receberam animações."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.8.4", "Alertas com permissão segura",
        listOf(
            "Na primeira abertura, escolha se deseja ativar alertas de contas, faturas e orçamentos.",
            "Ao ativar os alertas, o Freya solicita a permissão de notificações ou abre as configurações quando ela está bloqueada.",
            "Recusas ou falhas de permissão não causam mais crash no aplicativo."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.8.3", "Recebimentos avulsos e calendário",
        listOf(
            "Cadastre entradas que acontecerão uma única vez, como o pagamento de um freelance.",
            "O recebimento aparece na Agenda e é salvo no backup.",
            "Escolha a data usando o calendário do Android; ela aparece no padrão dd/mm/aaaa."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.8.2", "Novo identificador do app",
        listOf(
            "O Freya agora usa o identificador técnico com.japaneixxx.freya.",
            "Para instalações antigas, exporte o backup no app anterior e restaure-o nesta versão."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.8.1", "Contas com o seu nome",
        listOf(
            "Renomeie contas e cartões em Mais > Contas ou em Planejar > Cartões. O nome do banco não muda e o seu fica valendo depois de cada sincronização.",
            "Os nomes que você escolheu entram no backup e voltam ao restaurar.",
            "Os arquivos exportados agora se chamam freya.csv e freya-backup.json."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.8.0", "Simulador de investimentos",
        listOf(
            "Novo simulador na aba Patrimônio: informe valor inicial, aporte mensal, prazo e rentabilidade e veja quanto vai render.",
            "A rentabilidade pode ser % ao ano, % ao mês, % do CDI ou IPCA + taxa. CDI e IPCA podem ser digitados ou buscados no Banco Central.",
            "Imposto de renda regressivo calculado aporte por aporte, com opção isenta para LCI, LCA e poupança.",
            "Preencha o simulador com um dos seus investimentos do Pluggy e use sua sobra média mensal como aporte.",
            "Meta reversa: descubra quanto aportar por mês para chegar a um valor.",
            "Compare até 3 cenários lado a lado."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.7.0", "Fimbulwinter",
        listOf(
            "A versão atual fica visível no final da tela Mais.",
            "As versões podem ter um codinome personalizado, como Fimbulwinter ou Ragnarok.",
            "Configure um PIN próprio de 4 dígitos para desbloquear o app junto da biometria."
        ),
        codename = "Fimbulwinter"
    ),
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
        ),
        codename = "Fimbulwinter"
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
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.4.0", "Atualização pelo próprio app",
        listOf(
            "O app verifica novas versões e instala a atualização por conta própria.",
            "Ajustes nas notificações e no botão voltar entre as abas."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.3.0", "Redesign nórdico",
        listOf(
            "Novo visual em todo o app, claro e escuro, com montanhas no topo do Início.",
            "Extrato reformulado: cartões por dia, filtros em chips, seleção em lote e detalhe em painel.",
            "Calendário de gastos, carrossel de insights e categorias personalizadas."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.2.0", "Segurança, alertas e widget",
        listOf(
            "Bloqueio por biometria ou PIN e modo para ocultar valores.",
            "Alertas de orçamento, contas a vencer e faturas de cartão.",
            "Widget de saldo, contas a pagar, cartões, parcelas detectadas e backup em JSON."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.1.0", "Planejamento",
        listOf(
            "Orçamentos por categoria, metas de poupança e cobranças recorrentes.",
            "Lançamentos manuais, insights automáticos e exportação em CSV."
        ),
        codename = "Fimbulwinter"
    ),
    ReleaseNote(
        "1.0.0", "Primeira versão",
        listOf(
            "Sincronização direta com o Pluggy (Meu Pluggy), sem servidor.",
            "Resumo do mês, extrato e categorias automáticas."
        ),
        codename = "Fimbulwinter"
    )
)

private fun versionParts(v: String): List<Int> = v.trim().removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }

fun releaseLabel(note: ReleaseNote): String =
    "v${note.version}${note.codename?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""}"

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
                "Você está na versão ${
                    notes.firstOrNull { it.version == currentVersion }?.let(::releaseLabel) ?: "v$currentVersion"
                }",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(notes, key = { it.version }) { n ->
                    NCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(releaseLabel(n), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
