package com.japaneixxx.freya.data

/** Regras de versão do app (Major.Minor.Bug), usadas pelo atualizador. Puro, para poder ser testado. */
object Versions {
    /**
     * Versão beta: o último número da versão (Bug) tem 2 ou mais dígitos (ex.: 1.8.41, 1.8.51).
     * Com 1 dígito (1.8.5, 1.9.0) é estável.
     */
    fun isBeta(version: String): Boolean {
        val bug = version.split("-", limit = 2).first().split(".").getOrNull(2) ?: return false
        return bug.isNotEmpty() && bug.all { it.isDigit() } && bug.length >= 2
    }

    /** [candidate] é estritamente mais nova que [current]? Compara número a número; sufixos depois de "-" são ignorados. */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(value: String) = value.split("-", limit = 2).first()
            .split(".").map { it.toIntOrNull() ?: 0 }
        val newParts = parts(candidate)
        val currentParts = parts(current)
        for (i in 0 until maxOf(newParts.size, currentParts.size)) {
            val newer = newParts.getOrElse(i) { 0 }
            val existing = currentParts.getOrElse(i) { 0 }
            if (newer != existing) return newer > existing
        }
        return false
    }
}
