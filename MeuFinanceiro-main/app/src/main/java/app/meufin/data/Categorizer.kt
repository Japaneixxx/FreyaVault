package app.meufin.data

import androidx.compose.ui.graphics.Color

enum class TransactionTypeBadge(
    val label: String,
    val color: Color,
    val iconName: String
) {
    PIX("Pix", Color(0xFF32BCAD), "pix"),
    CARD("Cartão", Color(0xFF7C4DFF), "credit_card"),
    BOLETO("Boleto", Color(0xFFFF9100), "receipt"),
    TRANSFER("Transf.", Color(0xFF29B6F6), "swap_horiz"),
    SALARY("Salário", Color(0xFF66BB6A), "payments"),
    INVESTMENT("Invest.", Color(0xFFAB47BC), "trending_up"),
    OTHER("Geral", Color(0xFF78909C), "label")
}

data class SanitizedTransaction(
    val cleanDescription: String,
    val badge: TransactionTypeBadge
)

object TransactionSanitizer {

    private val prefixRegex = Regex(
        "(?i)^(" +
                "compra\\s+no\\s+d[eé]bito|compra\\s+no\\s+cr[eé]dito|compra\\s+d[eé]bito|compra\\s+cr[eé]dito|" +
                "pix\\s+enviado\\s+para|pix\\s+recebido\\s+de|pix\\s+enviado|pix\\s+recebido|pix\\s+pago|" +
                "transfer[eê]ncia\\s+enviada\\s+para|transfer[eê]ncia\\s+recebida\\s+de|transfer[eê]ncia\\s+enviada|transfer[eê]ncia\\s+recebida|transf\\.|" +
                "pagamento\\s+de\\s+boleto|pagto\\s+boleto|pagamento\\s+efetuado|pagamento|" +
                "ted\\s+enviada|ted\\s+recebida|doc\\s+enviado|doc\\s+recebido" +
                ")\\s*[-:]?\\s*",
        RegexOption.IGNORE_CASE
    )

    fun sanitize(description: String): SanitizedTransaction {
        val raw = description.trim()
        val badge = detectBadge(raw)
        
        var clean = raw.replace(prefixRegex, "").trim()
        
        if (clean.isBlank()) {
            clean = raw
        } else {
            clean = clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        return SanitizedTransaction(
            cleanDescription = clean,
            badge = badge
        )
    }

    private fun detectBadge(text: String): TransactionTypeBadge {
        val lower = text.lowercase()
        return when {
            lower.contains("pix") -> TransactionTypeBadge.PIX
            lower.contains("d[eé]bito".toRegex()) || lower.contains("cr[eé]dito".toRegex()) || lower.contains("cart[aã]o".toRegex()) -> TransactionTypeBadge.CARD
            lower.contains("boleto") || lower.contains("convenio") -> TransactionTypeBadge.BOLETO
            lower.contains("transfer[eê]ncia".toRegex()) || lower.contains("ted") || lower.contains("doc") -> TransactionTypeBadge.TRANSFER
            lower.contains("sal[aá]rio".toRegex()) || lower.contains("proventos") -> TransactionTypeBadge.SALARY
            lower.contains("rendimento") || lower.contains("investimento") -> TransactionTypeBadge.INVESTMENT
            else -> TransactionTypeBadge.OTHER
        }
    }
}
