package gg.aquatic.waves.editor.serialize

import gg.aquatic.waves.editor.serialize.ValueSerializer.Simple
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.util.Vector
import java.util.Optional

val ValueSerializer.Companion.MATERIAL get() = Simple(
    Material.STONE,
    encode = { Material.matchMaterial(it.toString()) },
    decode = { it.toString() }
)

val ValueSerializer.Companion.SOUND get() = Simple(
    Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
    encode = { Registry.SOUNDS.get(Key.key(it.toString())) },
    decode = { Registry.SOUNDS.getKey(it)?.toString() }
)

val ValueSerializer.Companion.VECTOR get() = ValueSerializer.Simple(
    Vector(0.0, 0.0, 0.0),
    encode = { raw -> parseVector(raw.toString()) },
    decode = { "${it.x};${it.y};${it.z}" }
)

val ValueSerializer.Companion.OPTIONAL_VECTOR get() = ValueSerializer.Simple(
    Optional.empty<Vector>(),
    encode = { raw -> Optional.ofNullable(parseVector(raw.toString())) },
    decode = { opt -> opt.map { "${it.x};${it.y};${it.z}" }.orElse(null) }
)

val ValueSerializer.Companion.VECTOR_LIST get() = ValueSerializer.Simple(
    emptyList<Vector>(),
    encode = { raw -> parseList(raw) { parseVector(it) } },
    decode = { list -> list.map { "${it.x};${it.y};${it.z}" } }
)

val ValueSerializer.Companion.OPTIONAL_VECTOR_LIST get() = ValueSerializer.Simple(
    Optional.empty<List<Vector>>(),
    encode = { raw ->
        val list = parseList(raw) { parseVector(it) }
        if (list.isEmpty()) Optional.empty() else Optional.of(list)
    },
    decode = { opt -> opt.map { list -> list.map { "${it.x};${it.y};${it.z}" } }.orElse(null) }
)

val ValueSerializer.Companion.BLOCK_VECTOR get() = ValueSerializer.Simple(
    Vector(0, 0, 0),
    encode = { raw -> parseBlockVector(raw.toString()) },
    decode = { "${it.blockX};${it.blockY};${it.blockZ}" }
)

val ValueSerializer.Companion.OPTIONAL_BLOCK_VECTOR get() = ValueSerializer.Simple(
    Optional.empty<Vector>(),
    encode = { raw -> Optional.ofNullable(parseBlockVector(raw.toString())) },
    decode = { opt -> opt.map { "${it.blockX};${it.blockY};${it.blockZ}" }.orElse(null) }
)

val ValueSerializer.Companion.WORLD get() = Simple(
    Bukkit.getWorlds().firstOrNull(),
    encode = { Bukkit.getWorld(it.toString()) },
    decode = { it.name }
)

val ValueSerializer.Companion.OPTIONAL_WORLD get() = Simple(
    Optional.empty<World>(),
    encode = { raw -> Optional.ofNullable(Bukkit.getWorld(raw.toString())) },
    decode = { opt -> opt.map { it.name }.orElse(null) }
)

val ValueSerializer.Companion.COLOR get() = Simple(
    org.bukkit.Color.WHITE,
    encode = { raw -> parseColor(raw.toString()) },
    decode = { "#${"%02X".format(it.red)}${"%02X".format(it.green)}${"%02X".format(it.blue)}" }
)

val ValueSerializer.Companion.OPTIONAL_COLOR get() = Simple(
    Optional.empty<org.bukkit.Color>(),
    encode = { raw -> Optional.ofNullable(parseColor(raw.toString())) },
    decode = { opt -> opt.map { "#${"%02X".format(it.red)}${"%02X".format(it.green)}${"%02X".format(it.blue)}" }.orElse(null) }
)

val ValueSerializer.Companion.NAMESPACED_KEY get() = Simple<org.bukkit.NamespacedKey?>(
    null,
    encode = { org.bukkit.NamespacedKey.fromString(it.toString()) },
    decode = { it?.toString() }
)

val ValueSerializer.Companion.OPTIONAL_NAMESPACED_KEY get() = Simple(
    Optional.empty<org.bukkit.NamespacedKey>(),
    encode = { raw -> Optional.ofNullable(org.bukkit.NamespacedKey.fromString(raw.toString())) },
    decode = { opt -> opt.map { it.toString() }.orElse(null) }
)

private fun parseVector(str: String): Vector? {
    val split = str.split(";")
    return if (split.size == 3) {
        Vector(
            split[0].toDoubleOrNull() ?: return null,
            split[1].toDoubleOrNull() ?: return null,
            split[2].toDoubleOrNull() ?: return null
        )
    } else null
}

private fun parseBlockVector(str: String): Vector? {
    val split = str.split(";")
    return if (split.size == 3) {
        Vector(
            split[0].toIntOrNull() ?: return null,
            split[1].toIntOrNull() ?: return null,
            split[2].toIntOrNull() ?: return null
        )
    } else null
}

private fun parseColor(str: String): org.bukkit.Color? {
    return if (str.startsWith("#") && str.length == 7) {
        try {
            org.bukkit.Color.fromRGB(
                str.substring(1, 3).toInt(16),
                str.substring(3, 5).toInt(16),
                str.substring(5, 7).toInt(16)
            )
        } catch (_: Exception) { null }
    } else {
        val split = str.split(";")
        if (split.size == 3) {
            org.bukkit.Color.fromRGB(
                split[0].toIntOrNull() ?: return null,
                split[1].toIntOrNull() ?: return null,
                split[2].toIntOrNull() ?: return null
            )
        } else null
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> parseList(raw: Any, parser: (String) -> T?): List<T> {
    return (raw as? List<String>)?.mapNotNull { parser(it) } ?: emptyList()
}