package gg.aquatic.waves.editor.serialize

import java.util.Optional

val ValueSerializer.Companion.BOOLEAN get() = ValueSerializer.Simple(false, encode = { it.toString().toBoolean() })
val ValueSerializer.Companion.STRING get() = ValueSerializer.Simple("", encode = { it.toString() })

val ValueSerializer.Companion.OPTIONAL_BOOLEAN get() = ValueSerializer.Simple(
    Optional.empty<Boolean>(),
    encode = { raw ->
        val value = raw.toString().trim()
        if (value.isBlank() || value.equals("null", ignoreCase = true)) Optional.empty()
        else value.toBooleanStrictOrNull()?.let { Optional.of(it) } ?: Optional.empty()
    },
    decode = { it.orElse(null) }
)

val ValueSerializer.Companion.OPTIONAL_STRING get() = ValueSerializer.Simple(
    Optional.empty<String>(),
    encode = { raw ->
        val value = raw.toString().trim()
        if (value.isBlank() || value.equals("null", ignoreCase = true)) Optional.empty() else Optional.of(value)
    },
    decode = { it.orElse(null) }
)

val ValueSerializer.Companion.BOOLEAN_LIST get() = ValueSerializer.Simple(
    emptyList<Boolean>(),
    encode = { raw -> parseList(raw) { it.toBooleanStrictOrNull() } },
    decode = { it }
)
val ValueSerializer.Companion.STRING_LIST get() = ValueSerializer.Simple(
    emptyList<String>(),
    encode = { raw -> parseList(raw) { it } },
    decode = { it }
)

val ValueSerializer.Companion.OPTIONAL_BOOLEAN_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Boolean>>(),
    encode = { raw ->
        val list = parseList(raw) { it.toBooleanStrictOrNull() }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.orElse(null) }
)
val ValueSerializer.Companion.OPTIONAL_STRING_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<String>>(),
    encode = { raw ->
        val list = parseList(raw) { it }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { it.orElse(null) }
)

inline fun <reified T : Enum<T>> ValueSerializer.Companion.enum() = ValueSerializer.EnumSerializer(T::class.java)

private fun <T> parseList(raw: Any, parser: (String) -> T?): List<T> {
    @Suppress("UNCHECKED_CAST")
    val list = raw as? List<Any?> ?: return emptyList()
    return list.mapNotNull { parser(it.toString()) }
}
