package java.util

import com.japaneixxx.freya.web.Base64Codec

object Base64 {
    fun getEncoder(): Encoder = Encoder
    fun getDecoder(): Decoder = Decoder

    object Encoder {
        fun encodeToString(bytes: ByteArray): String = Base64Codec.encode(bytes)
        fun encode(bytes: ByteArray): ByteArray = Base64Codec.encode(bytes).encodeToByteArray()
    }

    object Decoder {
        fun decode(text: String): ByteArray = Base64Codec.decode(text)
        fun decode(bytes: ByteArray): ByteArray = Base64Codec.decode(bytes.decodeToString())
    }
}
