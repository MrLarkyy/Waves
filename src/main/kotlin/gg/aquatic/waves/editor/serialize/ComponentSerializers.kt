package gg.aquatic.waves.editor.serialize

import gg.aquatic.common.toMMString
import gg.aquatic.quickminimessage.MMParser
import net.kyori.adventure.text.Component
import java.util.Optional

val ValueSerializer.Companion.COMPONENT get() = ValueSerializer.Simple(
    Component.empty(),
    encode = { MMParser.deserialize(it.toString()) },
    decode = { it.toMMString() }
)

val ValueSerializer.Companion.OPTIONAL_COMPONENT get() = ValueSerializer.Simple(
    Optional.empty<Component>(),
    encode = { raw ->
        val str = raw.toString()
        if (str.isBlank() || str.equals("null", ignoreCase = true)) {
            Optional.empty()
        } else {
            Optional.ofNullable(MMParser.deserialize(str))
        }
    },
    decode = { opt -> opt.map { it.toMMString() }.orElse(null) }
)

val ValueSerializer.Companion.COMPONENT_LIST get() = ValueSerializer.Simple(
    emptyList<Component>(),
    encode = { raw ->
        @Suppress("UNCHECKED_CAST")
        (raw as? List<String>)?.map { MMParser.deserialize(it) } ?: emptyList()
    },
    decode = { list -> list.map { it.toMMString() } }
)

val ValueSerializer.Companion.OPTIONAL_COMPONENT_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Component>>(),
    encode = { raw ->
        @Suppress("UNCHECKED_CAST")
        val list = (raw as? List<String>)?.map { MMParser.deserialize(it) } ?: emptyList()
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { opt -> opt.map { list -> list.map { it.toMMString() } }.orElse(null) }
)