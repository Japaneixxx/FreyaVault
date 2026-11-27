package java.time

data class LocalDateTime(
    val date: LocalDate,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
    val nano: Int = 0
) {
    fun toLocalDate(): LocalDate = date
    fun plusDays(days: Long): LocalDateTime = copy(date = date.plusDays(days))
    fun isAfter(other: LocalDateTime): Boolean = compare(other) > 0
    fun isBefore(other: LocalDateTime): Boolean = compare(other) < 0

    private fun compare(other: LocalDateTime): Int {
        val d = date.epochDay.compareTo(other.date.epochDay)
        if (d != 0) return d
        val h = hour.compareTo(other.hour); if (h != 0) return h
        val m = minute.compareTo(other.minute); if (m != 0) return m
        return second.compareTo(other.second)
    }

    companion object {
        fun now(): LocalDateTime {
            val now = com.japaneixxx.freya.web.WebBridge.nowMillis()
            val date = LocalDate.ofEpochDay(now / 86_400_000L)
            val seconds = (now % 86_400_000L / 1000).toInt()
            return LocalDateTime(date, seconds / 3600, (seconds % 3600) / 60, seconds % 60)
        }
    }
}
