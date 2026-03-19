package gg.aquatic.waves.serialization.editor.meta

import gg.aquatic.common.toMMComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

internal object EditorItemStyling {

    fun title(text: String): Component {
        return Component.text(text, NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false)
    }

    fun section(text: String): Component {
        return Component.text(text, NamedTextColor.DARK_AQUA)
            .decoration(TextDecoration.ITALIC, false)
    }

    fun hint(text: String): Component {
        return Component.text(text, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
    }

    fun valueLine(label: String, value: String): Component {
        return Component.text(label, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
            .append(
                Component.text(value.ifBlank { "<empty>" }, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false)
            )
    }

    fun rawValue(raw: String): String {
        return raw.ifBlank { "<empty>" }.take(120)
    }

    fun formattedPreview(raw: String, force: Boolean = false): List<Component> {
        if (!force && !looksLikeMiniMessage(raw)) {
            return emptyList()
        }

        return runCatching {
            listOf(
                section("Preview"),
                raw.toMMComponent().decoration(TextDecoration.ITALIC, false)
            )
        }.getOrDefault(emptyList())
    }

    private fun looksLikeMiniMessage(raw: String): Boolean {
        return '<' in raw && '>' in raw
    }
}
