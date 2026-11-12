package com.japaneixxx.freya.data

import java.text.Normalizer

/** Tipo de movimentação, mostrado como sinalização no extrato em vez de texto repetido na descrição. */
enum class TxKind(val label: String, val emoji: String, val fallback: String = label) {
    DEBIT_PURCHASE("Débito", "💳"),
    CREDIT_PURCHASE("Crédito", "💳"),
    PURCHASE("Compra", "🛍️"),
    PIX_SENT("Pix enviado", "⚡", "Pix enviado"),
    PIX_RECEIVED("Pix recebido", "⚡"),
    TRANSFER_SENT("Transf. enviada", "📤"),
    TRANSFER_RECEIVED("Transf. recebida", "📥"),
    BOLETO("Boleto", "🧾", "Pagamento de boleto"),
    CARD_BILL("Fatura", "💳", "Pagamento de fatura"),
    AUTO_DEBIT("Débito automático", "🔁"),
    WITHDRAWAL("Saque", "🏧", "Saque em dinheiro"),
    FEE("Tarifa", "🏦", "Tarifa bancária"),
    INTEREST("Juros/IOF", "📉", "Juros e IOF"),
    YIELD("Rendimento", "📈"),
    APPLY("Aplicação", "📥"),
    REDEEM("Resgate", "📤"),
    SALARY("Salário", "💼"),
    REFUND("Estorno", "↩️")
}

/** Nome limpo do lançamento + o tipo extraído do texto original. */
data class Clean(val title: String, val kind: TxKind?, val parcel: String? = null)

private class KindRule(val regex: Regex, val resolve: (Double) -> TxKind)

private fun rule(pattern: String, kind: TxKind) = KindRule(Regex(pattern)) { kind }
private fun directional(pattern: String, sent: TxKind, received: TxKind) =
    KindRule(Regex(pattern)) { amount -> if (amount >= 0) received else sent }

// Todas as regras rodam sobre o texto sem acentos e em maiúsculas, ancoradas no início.
private val kindRules: List<KindRule> = listOf(
    rule("""^COMPRAS?\s+(?:NO\s+|NA\s+|COM\s+|EM\s+|DE\s+)?(?:CARTAO\s+(?:DE\s+)?)?DEBITO\b""", TxKind.DEBIT_PURCHASE),
    rule("""^(?:CARTAO\s+)?DEB(?:ITO)?\.?\s+(?:COMPRA|CARTAO)\b""", TxKind.DEBIT_PURCHASE),
    rule("""^COMPRAS?\s+(?:NO\s+|NA\s+|COM\s+|EM\s+|DE\s+)?(?:CARTAO\s+(?:DE\s+)?)?CREDITO\b""", TxKind.CREDIT_PURCHASE),
    rule("""^COMPRAS?\s+CARTAO\b""", TxKind.CREDIT_PURCHASE),

    rule("""^(?:TRANSFERENCIA\s+ENVIADA\s+(?:PELO\s+|VIA\s+|POR\s+)?PIX|PIX\s*[-:]?\s*(?:ENVIAD[OA]|PAGO|SAIDA|TRANSF(?:ERENCIA)?\s+ENVIAD[OA])|ENVIO\s+(?:DE\s+)?PIX|PAGAMENTO\s+(?:VIA\s+)?PIX|PIX\s+QR\s*CODE)""", TxKind.PIX_SENT),
    rule("""^(?:TRANSFERENCIA\s+RECEBIDA\s+(?:PELO\s+|VIA\s+|POR\s+)?PIX|PIX\s*[-:]?\s*(?:RECEBID[OA]|ENTRADA|TRANSF(?:ERENCIA)?\s+RECEBID[OA])|RECEBIMENTO\s+(?:DE\s+)?PIX|DEPOSITO\s+PIX)""", TxKind.PIX_RECEIVED),
    directional("""^PIX\b(?:\s+TRANSF(?:ERENCIA)?)?""", TxKind.PIX_SENT, TxKind.PIX_RECEIVED),

    rule("""^(?:TRANSFERENCIA|TRANSF)\.?\s+(?:PARA|ENVIADA)\b|^(?:TED|DOC)\s+ENVIAD[OA]\b|^ENVIO\s+(?:DE\s+)?(?:TED|DOC)\b|^TED\s+PARA\b""", TxKind.TRANSFER_SENT),
    rule("""^(?:TRANSFERENCIA|TRANSF)\.?\s+(?:DE\b|RECEBIDA)|^(?:TED|DOC)\s+RECEBID[OA]\b|^RECEBIMENTO\s+(?:DE\s+)?(?:TED|DOC)\b|^CREDITO\s+(?:DE\s+)?(?:TED|DOC)\b""", TxKind.TRANSFER_RECEIVED),
    directional("""^(?:TED|DOC|TRANSFERENCIA|TRANSF)\b\.?""", TxKind.TRANSFER_SENT, TxKind.TRANSFER_RECEIVED),

    rule("""^(?:PAG(?:AMENTO|TO)?|PGTO)\.?\s+(?:DE\s+|DA\s+)?FATURA\b|^FATURA\s+(?:DO\s+)?CARTAO\b""", TxKind.CARD_BILL),
    rule("""^(?:(?:PAG(?:AMENTO|TO)?|PGTO)\.?\s+(?:DE\s+)?(?:BOLETO|TITULO|CONVENIO|CONTA)|BOLETO|SISPAG)\b""", TxKind.BOLETO),
    rule("""^(?:DEBITO\s+AUTOMATICO|DEB\.?\s*AUT(?:OMATICO)?|DB\s+AUT)\b""", TxKind.AUTO_DEBIT),
    rule("""^SAQUE\b""", TxKind.WITHDRAWAL),
    rule("""^(?:TARIFA|ANUIDADE|MANUTENCAO\s+(?:DE\s+)?CONTA|PACOTE\s+(?:DE\s+)?SERVICOS?)\b""", TxKind.FEE),
    rule("""^(?:IOF|JUROS|MULTA|ENCARGOS|MORA)\b""", TxKind.INTEREST),
    rule("""^(?:RENDIMENTOS?|REND\.?\s+PAGO|RENDE\s+FACIL)\b""", TxKind.YIELD),
    rule("""^(?:APLICACAO|APLIC\.?)\b""", TxKind.APPLY),
    rule("""^RESGATE\b""", TxKind.REDEEM),
    rule("""^(?:SALARIO|PAGAMENTO\s+(?:DE\s+)?SALARIO|FOLHA\s+(?:DE\s+)?PAGAMENTO|PROVENTOS)\b""", TxKind.SALARY),
    rule("""^(?:ESTORNO|DEVOLUCAO|REEMBOLSO|CASHBACK)\b""", TxKind.REFUND),
    rule("""^COMPRAS?\b(?:\s+(?:EM|NO|NA|DE|COM)\b)?""", TxKind.PURCHASE)
)

// Nomes comerciais que chegam sujos do banco ("IFD*RESTAURANTE", "UBER *TRIP"...).
private val aliases: List<Pair<Regex, String>> = listOf(
    Regex("""IFD\*|^IFOOD""") to "iFood",
    Regex("""^UBER\s*\*?\s*EATS""") to "Uber Eats",
    Regex("""^UBER""") to "Uber",
    Regex("""^99\s*(?:APP|POP|TAXI)|^99\*|^99TAXI""") to "99",
    Regex("""^99\s*FOOD""") to "99Food",
    Regex("""^(?:MP\s*\*\s*)?MERCADO\s*LIVRE|^MERCADOLIVRE""") to "Mercado Livre",
    Regex("""^(?:AMAZON\s*PRIME|PRIME\s*VIDEO)""") to "Amazon Prime",
    Regex("""^AMAZON""") to "Amazon",
    Regex("""^NETFLIX""") to "Netflix",
    Regex("""^SPOTIFY""") to "Spotify",
    Regex("""^(?:GOOGLE\s*\*?\s*)?(?:YOUTUBE|YT\s*PREMIUM)""") to "YouTube",
    Regex("""^APPLE\.?\s*COM|^APPLE\s*BILL""") to "Apple",
    Regex("""^DISNEY""") to "Disney+",
    Regex("""^SHOPEE""") to "Shopee",
    Regex("""^(?:MAGALU|MAGAZINE\s*LUIZA)""") to "Magalu",
    Regex("""^RAPPI""") to "Rappi",
    Regex("""^PIC\s*PAY""") to "PicPay",
    Regex("""^PAYPAL""") to "PayPal"
)

private val maskedIds = Regex(
    """[•*xX]{2,}\.?\d{3}\.\d{3}-?[•*xX]{0,2}|\d{3}\.\d{3}\.\d{3}-\d{2}|\d{2}\.\d{3}\.\d{3}/\d{4}-\d{2}"""
)
private val agencyAccount = Regex("""\s*\bAG(?:ENCIA)?\b\.?:?\s*\d.*$|\s*\bCONTA\b:?\s*[\d-]+.*$""", RegexOption.IGNORE_CASE)
private val noisePart = Regex("""^(?:[\d.\-/•*xX\s]+|(?:CPF|CNPJ|AGENCIA|AG|CONTA)\b.*)$""", RegexOption.IGNORE_CASE)
private val gatewayPrefix = Regex(
    """^(?:MERCADO\s*PAGO|MERCADOPAGO|MP|PAG|PAGSEGURO|PS|SUMUP|SUM\s*UP|ZIG|IZETTLE)\s*\*\s*""", RegexOption.IGNORE_CASE
)
private val trailingParcel = Regex(
    """(?:\s+PARC(?:ELA)?\.?)?\s+(\d{1,2})(?:/|\s+DE\s+)(\d{1,2})(/\d{2,4})?\s*$""", RegexOption.IGNORE_CASE
)
private val genericRemainder = setOf("CARTAO", "CARTAO DE CREDITO", "CREDITO", "DEBITO", "CONTA", "PIX", "TED", "DOC")
private val trailingCode = Regex("""\s+\d{4,}\s*$""")
private val smallWords = setOf("de", "da", "do", "das", "dos", "e", "em", "a", "o", "para")

/** Mesmo comprimento do original: assim os índices do texto sem acento servem para recortar o original. */
private fun fold(s: String): String = buildString(s.length) {
    for (ch in s) {
        val n = Normalizer.normalize(ch.toString(), Normalizer.Form.NFD)
        append(if (n.isNotEmpty()) n[0] else ch)
    }
}.uppercase()

/** Texto sem acentos e em maiúsculas, para comparar palavras do extrato. */
fun foldText(s: String): String = fold(s)

private fun prettify(s: String): String {
    val t = s.trim()
    val letters = t.filter { it.isLetter() }
    if (letters.isEmpty() || letters.count { it.isUpperCase() } < letters.length * 0.7) return t
    return t.lowercase().split(' ').filter { it.isNotBlank() }.mapIndexed { i, w ->
        if (i > 0 && w in smallWords) w else w.replaceFirstChar { it.uppercase() }
    }.joinToString(" ")
}

/**
 * Remove o texto redundante do banco ("Compra no débito", "Transferência enviada pelo Pix", CPF mascarado,
 * agência/conta) e devolve só o nome do estabelecimento ou pessoa, mais o tipo como sinalização.
 */
fun sanitize(raw: String, amount: Double = 0.0): Clean {
    val original = raw.trim()
    if (original.isEmpty()) return Clean("Sem descrição", null)
    val folded = fold(original)
    val src = if (folded.length == original.length) original else folded

    var kind: TxKind? = null
    var rest = src
    for (r in kindRules) {
        val m = r.regex.find(folded) ?: continue
        kind = r.resolve(amount)
        rest = src.substring(m.range.last + 1)
        break
    }

    rest = rest.replace(maskedIds, " ").replace(agencyAccount, "")
        .trim().trimStart('-', '–', ':', '|', '/', '•', '*', '.', ' ')
    val parts = rest.split(Regex("""\s+[-–|]\s+""")).map { it.trim() }
        .filter { it.isNotBlank() && !noisePart.matches(it) }
    var title = parts.firstOrNull().orEmpty().replace(gatewayPrefix, "")

    // "LOJA X 03/10" vira título "Loja X" + sinalização de parcela; datas soltas ("12/05") são apenas removidas.
    var parcel: String? = null
    trailingParcel.find(title)?.let { m ->
        val cur = m.groupValues[1].toIntOrNull() ?: 0
        val tot = m.groupValues[2].toIntOrNull() ?: 0
        if (m.groupValues[3].isEmpty() && tot in 2..48 && cur in 1..tot) parcel = "$cur/$tot"
        title = title.removeRange(m.range)
    }

    val foldedTitle = fold(title)
    aliases.firstOrNull { it.first.containsMatchIn(foldedTitle) }?.let { title = it.second }

    title = title.replace(trailingCode, "").replace(Regex("""\s+"""), " ").trim()
    if (fold(title) in genericRemainder || title.count { it.isLetter() } < 3) title = ""
    title = prettify(title)
    if (title.length > 48) title = title.take(47).trimEnd() + "…"
    if (title.isBlank()) title = kind?.fallback ?: prettify(original)
    return Clean(title, kind, parcel)
}

/** Categoria sugerida pelo tipo da movimentação, quando o nome não ajuda. */
fun kindCategory(kind: TxKind?): String? = when (kind) {
    TxKind.CARD_BILL -> CAT_CARD_PAYMENT
    TxKind.APPLY, TxKind.REDEEM -> CAT_OWN_TRANSFER
    TxKind.SALARY -> "Receitas"
    TxKind.YIELD -> "Investimentos"
    TxKind.FEE, TxKind.INTEREST -> "Impostos e taxas"
    TxKind.PIX_SENT, TxKind.PIX_RECEIVED, TxKind.TRANSFER_SENT, TxKind.TRANSFER_RECEIVED -> "Transferências"
    TxKind.WITHDRAWAL -> "Outros"
    else -> null
}
