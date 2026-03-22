package gg.aquatic.waves.testing.data

import gg.aquatic.stacked.ItemHandler
import gg.aquatic.stacked.StackedItem
import gg.aquatic.stacked.impl.StackedItemImpl
import gg.aquatic.stacked.option.*
import gg.aquatic.waves.editor.Configurable
import gg.aquatic.waves.editor.edit.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import java.util.*

class StackedItemData(
    initialMaterial: String = Material.STONE.name,
    initialDisplayName: Optional<Component> = Optional.empty(),
    initialLore: List<Component> = emptyList(),
    initialAmount: Int = 1
) : Configurable<StackedItemData>() {

    constructor(
        initialMaterial: Material,
        initialDisplayName: Optional<Component> = Optional.empty(),
        initialLore: List<Component> = emptyList(),
        initialAmount: Int = 1
    ) : this(initialMaterial.name, initialDisplayName, initialLore, initialAmount)

    val material = editString(
        "material",
        initialMaterial,
        "Enter material or factory item (MATERIAL or Factory:ItemId):"
    )

    val amount = editInt("amount", initialAmount, "Enter Amount (1-64):", min = 1, max = 64)

    val displayName = editOptionalComponent(
        "display-name",
        initialDisplayName,
        "Enter Display Name (type null to remove):"
    )
    val lore = editComponentList("lore", initialLore)

    val customModelDataLegacy = editOptionalInt(
        "custom-model-data-legacy",
        initial = Optional.empty(),
        prompt = "Enter legacy custom model data integer (null to unset):"
    )

    val customModelColors = editColorList(
        key = "custom-model-data-colors",
        prompt = "Enter color (#RRGGBB or r;g;b):"
    )
    val customModelFloats = editFloatList(
        key = "custom-model-data-floats",
        prompt = "Enter float value:"
    )
    val customModelFlags = editBooleanList(
        key = "custom-model-data-flags",
        prompt = "Enter boolean value (true/false):"
    )
    val customModelStrings = editStringList(
        key = "custom-model-data-strings",
        prompt = "Enter string value:"
    )

    val itemModel = editOptionalKey("item-model", prompt = "Enter item model key (namespace:key), null to unset:")
    val damage = editOptionalInt("damage", prompt = "Enter damage integer (null to unset):")
    val maxDamage = editOptionalInt("max-damage", prompt = "Enter max damage integer (null to unset):")
    val maxStackSize = editOptionalInt("max-stack-size", prompt = "Enter max stack size integer (null to unset):")

    val unbreakable = editBoolean("unbreakable", false)
    val hideTooltip = editBoolean("hide-tooltip", false)

    val rarity = editOptionalItemRarity("rarity", prompt = "Enter rarity (null to unset):")
    val spawnerType = editOptionalEntityType("spawner-type", prompt = "Enter spawner entity type (null to unset):")
    val tooltipStyle = editOptionalKey("tooltip-style", prompt = "Enter tooltip style key (namespace:key), null to unset:")
    val dyeColor = editOptionalColor("dye-color", prompt = "Enter dye color (#RRGGBB or r;g;b), null to unset:")

    val enchants = editStringList(
        key = "enchants",
        prompt = "Enter enchant in format enchant:level (e.g. sharpness:5):"
    )

    val flags = editItemFlagList(
        key = "flags",
        prompt = "Enter item flag (e.g. HIDE_ATTRIBUTES):"
    )

    fun asStacked(): StackedItemImpl {
        val baseItem = resolveBaseItem()
        val options = mutableListOf<ItemOptionHandle>()

        options += AmountOptionHandle(amount.value)

        displayName.value.ifPresent {
            options += DisplayNameOptionHandle(it)
        }

        val loreLines = lore.value.map { it.value }
        if (loreLines.isNotEmpty()) {
            options += LoreOptionHandle(loreLines)
        }

        customModelDataLegacy.value.ifPresent {
            options += CustomModelDataLegacyOptionHandle(it)
        }

        val modelColors = customModelColors.value.map { it.value }
        val modelFloats = customModelFloats.value.map { it.value }
        val modelFlagValues = customModelFlags.value.map { it.value }
        val modelStringValues = customModelStrings.value.map { it.value }

        if (modelColors.isNotEmpty() || modelFloats.isNotEmpty() || modelFlagValues.isNotEmpty() || modelStringValues.isNotEmpty()) {
            options += CustomModelDataOptionHandle(
                colors = modelColors,
                floats = modelFloats,
                flags = modelFlagValues,
                strings = modelStringValues
            )
        }

        itemModel.value.ifPresent {
            options += ItemModelOptionHandle(it)
        }

        damage.value.ifPresent {
            options += DamageOptionHandle(it)
        }

        maxDamage.value.ifPresent {
            options += MaxDamageOptionHandle(it)
        }

        maxStackSize.value.ifPresent {
            options += MaxStackSizeOptionHandle(it)
        }

        if (unbreakable.value) {
            options += UnbreakableOptionHandle()
        }

        if (hideTooltip.value) {
            options += HideTooltipOptionHandle(true)
        }

        rarity.value.ifPresent {
            options += RarityOptionHandle(it)
        }

        spawnerType.value.ifPresent {
            options += SpawnerTypeOptionHandle(it)
        }

        tooltipStyle.value.ifPresent {
            options += TooltipStyleOptionHandle(it)
        }

        dyeColor.value.ifPresent {
            options += DyeOptionHandle(it)
        }

        val parsedEnchants = enchants.value.mapNotNull { parseEnchantLine(it.value) }.toMap()
        if (parsedEnchants.isNotEmpty()) {
            options += EnchantsOptionHandle(parsedEnchants)
        }

        val parsedFlags = flags.value.map { it.value }
        if (parsedFlags.isNotEmpty()) {
            options += FlagsOptionHandle(parsedFlags)
        }

        return ItemHandler.Impl.create(baseItem, options)
    }

    private fun resolveBaseItem(): ItemStack {
        val raw = material.value.trim()
        if (raw.isEmpty()) return ItemStack(Material.STONE)

        parseVanillaMaterial(raw)?.let { vanilla ->
            return ItemStack(vanilla)
        }

        val splitIdx = raw.indexOf(':')
        if (splitIdx <= 0 || splitIdx >= raw.lastIndex) {
            return ItemStack(Material.STONE)
        }

        val factoryId = raw.substring(0, splitIdx).trim()
        val itemId = raw.substring(splitIdx + 1).trim()
        if (factoryId.isEmpty() || itemId.isEmpty()) return ItemStack(Material.STONE)

        return resolveFactoryItem(factoryId, itemId)?.clone() ?: ItemStack(Material.STONE)
    }

    private fun resolveFactoryItem(factory: String, itemId: String): ItemStack? {
        val candidates = linkedSetOf(
            factory,
            factory.lowercase(Locale.ROOT),
            factory.uppercase(Locale.ROOT)
        )

        for (candidate in candidates) {
            val built = StackedItem.ITEM_FACTORIES[candidate]?.create(itemId)
            if (built != null) return built
        }
        return null
    }

    private fun parseVanillaMaterial(raw: String): Material? {
        return Material.matchMaterial(raw)
            ?: Material.matchMaterial(raw.uppercase(Locale.ROOT))
    }

    private fun parseEnchantLine(raw: String): Pair<String, Int>? {
        val value = raw.trim()
        val splitIdx = value.lastIndexOf(':')
        if (splitIdx <= 0 || splitIdx >= value.lastIndex) return null

        val enchant = value.substring(0, splitIdx).trim()
        val level = value.substring(splitIdx + 1).trim().toIntOrNull() ?: return null
        if (enchant.isEmpty()) return null

        return enchant to level
    }

}
