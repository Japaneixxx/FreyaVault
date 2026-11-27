package java.util

class Locale(val language: String, val country: String = "") {
    companion object {
        val US = Locale("en", "US")
        val ROOT = Locale("", "")
    }

    override fun toString(): String = if (country.isEmpty()) language else "${language}_$country"
}
