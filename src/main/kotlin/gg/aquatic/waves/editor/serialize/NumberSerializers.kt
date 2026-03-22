package gg.aquatic.waves.editor.serialize

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

val ValueSerializer.Companion.INT get() = ValueSerializer.Simple(1, encode = { it.toString().toIntOrNull() ?: 1 })
val ValueSerializer.Companion.FLOAT get() = ValueSerializer.Simple(1f, encode = { it.toString().toFloatOrNull() ?: 1f })
val ValueSerializer.Companion.DOUBLE get() = ValueSerializer.Simple(1.0, encode = { it.toString().toDoubleOrNull() ?: 1.0 })
val ValueSerializer.Companion.LONG get() = ValueSerializer.Simple(1L, encode = { it.toString().toLongOrNull() ?: 1L })
val ValueSerializer.Companion.SHORT get() = ValueSerializer.Simple(1, encode = { it.toString().toShortOrNull() ?: 1 })
val ValueSerializer.Companion.BYTE get() = ValueSerializer.Simple(0, encode = { it.toString().toByteOrNull() ?: 0 })

val ValueSerializer.Companion.OPTIONAL_INT get() = optionalValue { it.toIntOrNull() }
val ValueSerializer.Companion.OPTIONAL_FLOAT get() = optionalValue { it.toFloatOrNull() }
val ValueSerializer.Companion.OPTIONAL_DOUBLE get() = optionalValue { it.toDoubleOrNull() }
val ValueSerializer.Companion.OPTIONAL_LONG get() = optionalValue { it.toLongOrNull() }
val ValueSerializer.Companion.OPTIONAL_SHORT get() = optionalValue { it.toShortOrNull() }
val ValueSerializer.Companion.OPTIONAL_BYTE get() = optionalValue { it.toByteOrNull() }

val ValueSerializer.Companion.INT_LIST get() = ValueSerializer.Simple(
    emptyList<Int>(),
    encode = { raw -> parseList(raw) { it.toIntOrNull() } },
    decode = { it }
)
val ValueSerializer.Companion.FLOAT_LIST get() = ValueSerializer.Simple(
    emptyList<Float>(),
    encode = { raw -> parseList(raw) { it.toFloatOrNull() } },
    decode = { it }
)
val ValueSerializer.Companion.DOUBLE_LIST get() = ValueSerializer.Simple(
    emptyList<Double>(),
    encode = { raw -> parseList(raw) { it.toDoubleOrNull() } },
    decode = { it }
)
val ValueSerializer.Companion.LONG_LIST get() = ValueSerializer.Simple(
    emptyList<Long>(),
    encode = { raw -> parseList(raw) { it.toLongOrNull() } },
    decode = { it }
)
val ValueSerializer.Companion.SHORT_LIST get() = ValueSerializer.Simple(
    emptyList<Short>(),
    encode = { raw -> parseList(raw) { it.toShortOrNull() } },
    decode = { it }
)
val ValueSerializer.Companion.BYTE_LIST get() = ValueSerializer.Simple(
    emptyList<Byte>(),
    encode = { raw -> parseList(raw) { it.toByteOrNull() } },
    decode = { it }
)

val ValueSerializer.Companion.OPTIONAL_INT_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Int>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toIntOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.orElse(null) }
)
val ValueSerializer.Companion.OPTIONAL_FLOAT_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Float>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toFloatOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.orElse(null) }
)
val ValueSerializer.Companion.OPTIONAL_DOUBLE_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Double>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toDoubleOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.orElse(null) }
)
val ValueSerializer.Companion.OPTIONAL_LONG_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Long>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toLongOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.orElse(null) }
)
val ValueSerializer.Companion.OPTIONAL_SHORT_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Short>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toShortOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.orElse(null) }
)
val ValueSerializer.Companion.OPTIONAL_BYTE_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Byte>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toByteOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.orElse(null) }
)

val ValueSerializer.Companion.BIGINT get() = ValueSerializer.Simple(
    BigInteger.ONE,
    encode = { it.toString().toBigIntegerOrNull() },
    decode = { it.toString() }
)
val ValueSerializer.Companion.OPTIONAL_BIGINT get() = optionalValue { it.toBigIntegerOrNull() }
val ValueSerializer.Companion.BIGINT_LIST get() = ValueSerializer.Simple(
    emptyList<BigInteger>(),
    encode = { raw -> parseList(raw) { it.toBigIntegerOrNull() } },
    decode = { it.map(BigInteger::toString) }
)
val ValueSerializer.Companion.OPTIONAL_BIGINT_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<BigInteger>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toBigIntegerOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.map { list -> list.map(BigInteger::toString) }.orElse(null) }
)

val ValueSerializer.Companion.BIGDECIMAL get() = ValueSerializer.Simple(
    BigDecimal.ONE,
    encode = { it.toString().toBigDecimalOrNull() },
    decode = { it.toString() }
)
val ValueSerializer.Companion.OPTIONAL_BIGDECIMAL get() = optionalValue { it.toBigDecimalOrNull() }
val ValueSerializer.Companion.BIGDECIMAL_LIST get() = ValueSerializer.Simple(
    emptyList<BigDecimal>(),
    encode = { raw -> parseList(raw) { it.toBigDecimalOrNull() } },
    decode = { it.map(BigDecimal::toString) }
)
val ValueSerializer.Companion.OPTIONAL_BIGDECIMAL_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<BigDecimal>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toBigDecimalOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.map { list -> list.map(BigDecimal::toString) }.orElse(null) }
)

val ValueSerializer.Companion.UINT get() = ValueSerializer.Simple(
    1u,
    encode = { it.toString().toUIntOrNull() ?: 1u },
    decode = { it.toLong() }
)
val ValueSerializer.Companion.OPTIONAL_UINT get() = optionalValue { it.toUIntOrNull() }
val ValueSerializer.Companion.UINT_LIST get() = ValueSerializer.Simple(
    emptyList<UInt>(),
    encode = { raw -> parseList(raw) { it.toUIntOrNull() } },
    decode = { it.map(UInt::toString) }
)
val ValueSerializer.Companion.OPTIONAL_UINT_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<UInt>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toUIntOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.map { list -> list.map(UInt::toString) }.orElse(null) }
)

val ValueSerializer.Companion.ULONG get() = ValueSerializer.Simple(
    1uL,
    encode = { it.toString().toULongOrNull() ?: 1uL },
    decode = { it.toString() }
)
val ValueSerializer.Companion.OPTIONAL_ULONG get() = optionalValue { it.toULongOrNull() }
val ValueSerializer.Companion.ULONG_LIST get() = ValueSerializer.Simple(
    emptyList<ULong>(),
    encode = { raw -> parseList(raw) { it.toULongOrNull() } },
    decode = { it.map(ULong::toString) }
)
val ValueSerializer.Companion.OPTIONAL_ULONG_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<ULong>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toULongOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.map { list -> list.map(ULong::toString) }.orElse(null) }
)

val ValueSerializer.Companion.USHORT get() = ValueSerializer.Simple(
    1u.toUShort(),
    encode = { it.toString().toUShortOrNull() ?: 1u.toUShort() },
    decode = { it.toInt() }
)
val ValueSerializer.Companion.OPTIONAL_USHORT get() = optionalValue { it.toUShortOrNull() }
val ValueSerializer.Companion.USHORT_LIST get() = ValueSerializer.Simple(
    emptyList<UShort>(),
    encode = { raw -> parseList(raw) { it.toUShortOrNull() } },
    decode = { it.map(UShort::toString) }
)
val ValueSerializer.Companion.OPTIONAL_USHORT_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<UShort>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toUShortOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.map { list -> list.map(UShort::toString) }.orElse(null) }
)

val ValueSerializer.Companion.UBYTE get() = ValueSerializer.Simple(
    0u.toUByte(),
    encode = { it.toString().toUByteOrNull() ?: 0u.toUByte() },
    decode = { it.toInt() }
)
val ValueSerializer.Companion.OPTIONAL_UBYTE get() = optionalValue { it.toUByteOrNull() }
val ValueSerializer.Companion.UBYTE_LIST get() = ValueSerializer.Simple(
    emptyList<UByte>(),
    encode = { raw -> parseList(raw) { it.toUByteOrNull() } },
    decode = { it.map(UByte::toString) }
)
val ValueSerializer.Companion.OPTIONAL_UBYTE_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<UByte>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toUByteOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.map { list -> list.map(UByte::toString) }.orElse(null) }
)

private fun <T> parseList(raw: Any, parser: (String) -> T?): List<T> {
    @Suppress("UNCHECKED_CAST")
    val list = raw as? List<Any?> ?: return emptyList()
    return list.mapNotNull { parser(it.toString()) }
}

private fun <T : Any> optionalValue(parser: (String) -> T?) = ValueSerializer.Simple(
    Optional.empty<T>(),
    encode = { raw -> optionalParse(raw, parser) },
    decode = { it.orElse(null) }
)

private fun <T : Any> optionalParse(raw: Any, parser: (String) -> T?): Optional<T> {
    val value = raw.toString().trim()
    if (value.isBlank() || value.equals("null", ignoreCase = true)) return Optional.empty<T>()
    return Optional.ofNullable(parser(value))
}
