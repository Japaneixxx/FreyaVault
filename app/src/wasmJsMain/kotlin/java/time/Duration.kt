package java.time

class Duration private constructor(private val millis: Long) {
    fun toMillis(): Long = millis
    fun toMinutes(): Long = millis / 60_000
    fun toDays(): Long = millis / 86_400_000

    companion object {
        fun between(start: LocalDateTime, end: LocalDateTime): Duration {
            val startMillis = start.date.epochDay * 86_400_000L +
                (start.hour * 3600L + start.minute * 60L + start.second) * 1000
            val endMillis = end.date.epochDay * 86_400_000L +
                (end.hour * 3600L + end.minute * 60L + end.second) * 1000
            return Duration(endMillis - startMillis)
        }

        fun ofMillis(millis: Long): Duration = Duration(millis)
    }
}
