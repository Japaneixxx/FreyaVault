package java.time

data class ZonedDateTime(val date: LocalDate, val epochMillis: Long) {
    fun toLocalDate(): LocalDate = date
    fun toInstant(): Instant = Instant(epochMillis)
}
