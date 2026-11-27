package java.text

/** Normalização suficiente para o português: remove acentos mantendo o tamanho do texto. */
object Normalizer {
    enum class Form { NFC, NFD, NFKD, NFKC }

    fun normalize(text: String, form: Form = Form.NFC): String {
        val out = StringBuilder(text.length)
        for (ch in text) out.append(fold(ch))
        return out.toString()
    }

    private fun fold(ch: Char): Char = when (ch) {
        'á', 'à', 'â', 'ã', 'ä', 'å', 'Á', 'À', 'Â', 'Ã', 'Ä', 'Å' -> if (ch.isUpperCase()) 'A' else 'a'
        'é', 'è', 'ê', 'ë', 'É', 'È', 'Ê', 'Ë' -> if (ch.isUpperCase()) 'E' else 'e'
        'í', 'ì', 'î', 'ï', 'Í', 'Ì', 'Î', 'Ï' -> if (ch.isUpperCase()) 'I' else 'i'
        'ó', 'ò', 'ô', 'õ', 'ö', 'Ó', 'Ò', 'Ô', 'Õ', 'Ö' -> if (ch.isUpperCase()) 'O' else 'o'
        'ú', 'ù', 'û', 'ü', 'Ú', 'Ù', 'Û', 'Ü' -> if (ch.isUpperCase()) 'U' else 'u'
        'ç', 'Ç' -> if (ch.isUpperCase()) 'C' else 'c'
        'ñ', 'Ñ' -> if (ch.isUpperCase()) 'N' else 'n'
        'ý', 'ÿ', 'Ý' -> if (ch.isUpperCase()) 'Y' else 'y'
        else -> ch
    }
}
