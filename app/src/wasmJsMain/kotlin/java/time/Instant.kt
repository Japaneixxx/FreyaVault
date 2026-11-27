package java.time

data class Instant(val epochMillis: Long) {
    fun toEpochMilli(): Long = epochMillis
    fun atZone(zone: ZoneId): ZonedDateTime =
        ZonedDateTime(LocalDate.ofEpochDay(epochMillis / 86_400_000L), epochMillis)

    companion object {
        fun ofEpochMilli(millis: Long): Instant = Instant(millis)
        fun now(): Instant = Instant(com.japaneixxx.freya.web.WebBridge.nowMillis())
    }
}
