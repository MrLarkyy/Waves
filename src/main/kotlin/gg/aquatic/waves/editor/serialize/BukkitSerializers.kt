package gg.aquatic.waves.editor.serialize

import gg.aquatic.waves.editor.serialize.ValueSerializer.Simple
import net.kyori.adventure.key.Key
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemRarity
import org.bukkit.util.Vector
import java.util.*

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

val ValueSerializer.Companion.KEY get() = Simple(
    Key.key("minecraft", "stone"),
    encode = { raw -> parseAdventureKey(raw.toString()) },
    decode = { it.asString() }
)

val ValueSerializer.Companion.OPTIONAL_KEY get() = Simple(
    Optional.empty<Key>(),
    encode = { raw -> Optional.ofNullable(parseAdventureKey(raw.toString())) },
    decode = { opt -> opt.map { it.asString() }.orElse(null) }
)

val ValueSerializer.Companion.ITEM_RARITY get() = Simple(
    ItemRarity.values().first(),
    encode = { raw -> parseItemRarity(raw.toString()) },
    decode = { it.name }
)

val ValueSerializer.Companion.OPTIONAL_ITEM_RARITY get() = Simple(
    Optional.empty<ItemRarity>(),
    encode = { raw -> Optional.ofNullable(parseItemRarity(raw.toString())) },
    decode = { opt -> opt.map { it.name }.orElse(null) }
)

val ValueSerializer.Companion.ENTITY_TYPE get() = Simple(
    EntityType.PIG,
    encode = { raw -> parseEntityType(raw.toString()) },
    decode = { it.name }
)

val ValueSerializer.Companion.OPTIONAL_ENTITY_TYPE get() = Simple(
    Optional.empty<EntityType>(),
    encode = { raw -> Optional.ofNullable(parseEntityType(raw.toString())) },
    decode = { opt -> opt.map { it.name }.orElse(null) }
)

val ValueSerializer.Companion.ITEM_FLAG get() = Simple(
    ItemFlag.HIDE_ATTRIBUTES,
    encode = { raw -> parseItemFlag(raw.toString()) },
    decode = { it.name }
)

val ValueSerializer.Companion.OPTIONAL_ITEM_FLAG get() = Simple(
    Optional.empty<ItemFlag>(),
    encode = { raw -> Optional.ofNullable(parseItemFlag(raw.toString())) },
    decode = { opt -> opt.map { it.name }.orElse(null) }
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
    val value = str.trim()
    if (value.isEmpty()) return null

    return if (value.startsWith("#") && value.length == 7) {
        try {
            org.bukkit.Color.fromRGB(
                value.substring(1, 3).toInt(16),
                value.substring(3, 5).toInt(16),
                value.substring(5, 7).toInt(16)
            )
        } catch (_: Exception) { null }
    } else {
        val split = if (';' in value) value.split(";") else value.split(",")
        if (split.size == 3) {
            org.bukkit.Color.fromRGB(
                split[0].trim().toIntOrNull() ?: return null,
                split[1].trim().toIntOrNull() ?: return null,
                split[2].trim().toIntOrNull() ?: return null
            )
        } else null
    }
}

private fun parseAdventureKey(str: String): Key? {
    val value = str.trim()
    if (value.isEmpty()) return null

    val direct = runCatching { Key.key(value) }.getOrNull()
    if (direct != null) return direct

    if (':' !in value) {
        return runCatching { Key.key("minecraft", value.lowercase(Locale.ROOT)) }.getOrNull()
    }
    return null
}

private fun parseItemRarity(str: String): ItemRarity? {
    val value = str.trim()
    if (value.isEmpty()) return null
    return runCatching { ItemRarity.valueOf(value.uppercase(Locale.ROOT)) }.getOrNull()
}

private fun parseEntityType(str: String): EntityType? {
    val value = str.trim()
    if (value.isEmpty()) return null
    return runCatching { EntityType.valueOf(value.uppercase(Locale.ROOT)) }.getOrNull()
}

private fun parseItemFlag(str: String): ItemFlag? {
    val value = str.trim()
    if (value.isEmpty()) return null
    return runCatching { ItemFlag.valueOf(value.uppercase(Locale.ROOT)) }.getOrNull()
}

@Suppress("UNCHECKED_CAST")
private fun <T> parseList(raw: Any, parser: (String) -> T?): List<T> {
    return (raw as? List<String>)?.mapNotNull { parser(it) } ?: emptyList()
}
