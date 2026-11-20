package com.japaneixxx.freya.data

import java.text.Normalizer

const val CAT_CARD_PAYMENT = "Pagamento de fatura"
const val CAT_OWN_TRANSFER = "Transferência própria"

/** Categorias que não entram em receitas/despesas (evita contar duas vezes). */
val IGNORED_CATEGORIES = setOf(CAT_CARD_PAYMENT, CAT_OWN_TRANSFER)

val ALL_CATEGORIES = listOf(
    "Mercado", "Restaurantes", "Delivery", "Transporte", "Moradia", "Saúde", "Lazer",
    "Assinaturas", "Compras", "Educação", "Viagem", "Impostos e taxas", "Investimentos",
    "Receitas", "Transferências", CAT_CARD_PAYMENT, CAT_OWN_TRANSFER, "Outros"
)

/** Categorias que fazem sentido para orçamento (despesas). */
val BUDGET_CATEGORIES = ALL_CATEGORIES.filter {
    it !in setOf("Receitas", "Transferências", CAT_CARD_PAYMENT, CAT_OWN_TRANSFER, "Investimentos")
}

private val pluggyMap = mapOf(
    "Groceries" to "Mercado", "Supermarkets" to "Mercado",
    "Eating out" to "Restaurantes", "Restaurants" to "Restaurantes",
    "Food delivery" to "Delivery",
    "Transportation" to "Transporte", "Taxi and ride-hailing" to "Transporte",
    "Gas stations" to "Transporte", "Parking" to "Transporte",
    "Health" to "Saúde", "Pharmacy" to "Saúde",
    "Entertainment" to "Lazer", "Gaming" to "Lazer",
    "Digital services" to "Assinaturas", "Streaming" to "Assinaturas",
    "Shopping" to "Compras", "Online shopping" to "Compras",
    "Education" to "Educação", "Travel" to "Viagem",
    "Taxes" to "Impostos e taxas", "Income" to "Receitas", "Salary" to "Receitas",
    "Investments" to "Investimentos", "Transfers" to "Transferências",
    "Same person transfer" to CAT_OWN_TRANSFER,
    "Credit card payment" to CAT_CARD_PAYMENT,
    "Rent" to "Moradia", "Housing" to "Moradia", "Utilities" to "Moradia",
    "Electricity" to "Moradia", "Water" to "Moradia", "Internet" to "Moradia",
    "Mobile" to "Moradia", "Telecommunications" to "Moradia",
    "Food and drinks" to "Restaurantes", "Bakery" to "Restaurantes", "Bars" to "Restaurantes",
    "Healthcare" to "Saúde", "Pharmacies" to "Saúde", "Dentist" to "Saúde",
    "Leisure" to "Lazer", "Gambling" to "Lazer", "Sports" to "Lazer", "Hobbies" to "Lazer",
    "Clothing" to "Compras", "Electronics" to "Compras", "Home improvement" to "Compras",
    "Legal obligations" to "Impostos e taxas", "Bank fees" to "Impostos e taxas", "Fees" to "Impostos e taxas",
    "Interest charged" to "Impostos e taxas", "Insurance" to "Impostos e taxas",
    "Retirement" to "Receitas", "Entrepreneurial activities" to "Receitas",
    "Government aid" to "Receitas", "Non-recurring income" to "Receitas",
    "Fixed income" to "Investimentos", "Variable income" to "Investimentos"
)

private val keywords: List<Pair<String, List<String>>> = listOf(
    CAT_CARD_PAYMENT to listOf("pagamento de fatura", "pagamento fatura", "pgto fatura", "pagto fatura"),
    "Delivery" to listOf("ifood", "rappi", "uber eats", "99food"),
    "Restaurantes" to listOf("restaurante", "lanchonete", "padaria", "burger", "mcdonald", "subway", "pizzaria", "cafe"),
    "Compras" to listOf("mercado livre", "mercadolivre"), // antes de "mercado", senão vira Mercado
    "Mercado" to listOf("mercado", "supermercado", "carrefour", "assai", "atacadao", "pao de acucar", "hortifruti", "sacolao"),
    "Transporte" to listOf("uber", "99app", "99 pop", "99pop", "cabify", "posto", "shell", "ipiranga", "estacionamento", "sem parar", "veloe", "bilhete unico"),
    "Assinaturas" to listOf("netflix", "spotify", "youtube", "prime video", "amazon prime", "disney", "hbo", "apple com", "icloud", "google one", "deezer", "globoplay"),
    "Saúde" to listOf("farmacia", "drogaria", "drogasil", "raia", "pacheco", "unimed", "hospital", "clinica", "laboratorio"),
    "Moradia" to listOf("aluguel", "condominio", "enel", "cpfl", "sabesp", "copasa", "comgas", "vivo", "claro", "oi fibra"),
    "Compras" to listOf("amazon", "mercado livre", "mercadolivre", "shopee", "magalu", "magazine luiza", "aliexpress", "shein", "americanas"),
    "Lazer" to listOf("cinema", "ingresso", "steam", "playstation", "xbox", "nintendo"),
    "Educação" to listOf("faculdade", "universidade", "curso", "udemy", "escola", "alura"),
    "Investimentos" to listOf("rendimento", "aplicacao", "resgate", "cdb", "tesouro"),
    "Receitas" to listOf("salario", "folha de pagamento")
)

private fun normalizeText(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^a-z ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/** Para palavras-chave: como [normalizeText], mas mantém dígitos (99app, 99pop). Não serve para chaves de regra. */
private fun normalizeForMatch(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/** A palavra-chave tem de começar numa palavra: "uber" casa com "uber trip", mas não com "tuberculose". */
internal fun hasWordStart(text: String, keyword: String): Boolean {
    var from = 0
    while (true) {
        val i = text.indexOf(keyword, from)
        if (i < 0) return false
        if (i == 0 || text[i - 1] == ' ') return true
        from = i + 1
    }
}

/** Chave da regra de categoria: nome já sanitizado ("Uber"), sem o texto repetido do banco. */
fun ruleKeyFor(tx: Tx): String =
    normalizeText(sanitize(tx.description, tx.amount).title).take(28).trim().ifBlank { tx.id }

/** Chave usada até a v1.4: mantida só para continuar reconhecendo regras já aprendidas. */
fun legacyRuleKeyFor(tx: Tx): String = normalizeText(tx.description).take(28).trim().ifBlank { tx.id }

/** Ordem: regra do usuário -> categoria Pluggy -> palavras-chave -> categoria bruta -> padrão. */
fun categorize(tx: Tx, rules: Map<String, String>): String {
    rules[ruleKeyFor(tx)]?.let { return it }
    rules[legacyRuleKeyFor(tx)]?.let { return it }
    if (isManualAccountId(tx.accountId) && tx.pluggyCategory.isNotBlank()) return tx.pluggyCategory
    pluggyMap[tx.pluggyCategory]?.let { return it }
    val text = normalizeForMatch(tx.description)
    keywords.firstOrNull { (_, words) -> words.any { hasWordStart(text, it) } }?.let { return it.first }
    kindCategory(sanitize(tx.description, tx.amount).kind)?.let { return it }
    // Categoria bruta do Pluggy só vale se for uma das nossas: o resto vem em inglês e não entra em orçamentos.
    if (tx.pluggyCategory in ALL_CATEGORIES) return tx.pluggyCategory
    return if (tx.amount > 0) "Receitas" else "Outros"
}
