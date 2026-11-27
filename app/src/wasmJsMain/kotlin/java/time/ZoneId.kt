package java.time

class ZoneId(val id: String) {
    override fun toString(): String = id

    companion object {
        fun of(id: String): ZoneId = ZoneId(id)
        fun systemDefault(): ZoneId = ZoneId("UTC")
    }
}
