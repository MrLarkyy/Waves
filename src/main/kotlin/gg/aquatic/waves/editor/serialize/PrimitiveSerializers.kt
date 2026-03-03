package gg.aquatic.waves.editor.serialize

import java.math.BigDecimal
import java.math.BigInteger

val ValueSerializer.Companion.INT get() = ValueSerializer.Simple(1, encode = { it.toString().toIntOrNull() ?: 1 })
val ValueSerializer.Companion.FLOAT get() = ValueSerializer.Simple(1f, encode = { it.toString().toFloatOrNull() ?: 1f })
val ValueSerializer.Companion.DOUBLE get() = ValueSerializer.Simple(1.0, encode = { it.toString().toDoubleOrNull() ?: 1.0 })
val ValueSerializer.Companion.LONG get() = ValueSerializer.Simple(1L, encode = { it.toString().toLongOrNull() ?: 1L })
val ValueSerializer.Companion.SHORT get() = ValueSerializer.Simple(1, encode = { it.toString().toShortOrNull() ?: 1 })
val ValueSerializer.Companion.BYTE get() = ValueSerializer.Simple(0, encode = { it.toString().toByteOrNull() ?: 0 })
val ValueSerializer.Companion.BOOLEAN get() = ValueSerializer.Simple(false, encode = { it.toString().toBoolean() })
val ValueSerializer.Companion.STRING get() = ValueSerializer.Simple("", encode = { it.toString() })

val ValueSerializer.Companion.BIGINT get() = ValueSerializer.Simple(
    BigInteger.ONE,
    encode = { it.toString().toBigIntegerOrNull() },
    decode = { it.toString() }
)

val ValueSerializer.Companion.BIGDECIMAL get() = ValueSerializer.Simple(
    BigDecimal.ONE,
    encode = { it.toString().toBigDecimalOrNull() },
    decode = { it.toString() }
)

val ValueSerializer.Companion.UINT get() = ValueSerializer.Simple(
    1u,
    encode = { it.toString().toUIntOrNull() ?: 1u },
    decode = { it.toLong() }
)

val ValueSerializer.Companion.ULONG get() = ValueSerializer.Simple(
    1uL,
    encode = { it.toString().toULongOrNull() ?: 1uL },
    decode = { it.toString() }
)

val ValueSerializer.Companion.USHORT get() = ValueSerializer.Simple(
    1u.toUShort(),
    encode = { it.toString().toUShortOrNull() ?: 1u.toUShort() },
    decode = { it.toInt() }
)

val ValueSerializer.Companion.UBYTE get() = ValueSerializer.Simple(
    0u.toUByte(),
    encode = { it.toString().toUByteOrNull() ?: 0u.toUByte() },
    decode = { it.toInt() }
)

inline fun <reified T : Enum<T>> ValueSerializer.Companion.enum() = ValueSerializer.EnumSerializer(T::class.java)