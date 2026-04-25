package gg.aquatic.waves.serialization.editor.meta

import gg.aquatic.common.toMMComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import java.util.Locale

internal object EditorItemStyling {
    private const val WRAP_WIDTH = 34
    private val TITLE = TextColor.color(0x6EF2FF)
    private val SECTION = TextColor.color(0x1FC8D6)
    private val HINT = TextColor.color(0xB8C7CF)
    private val LABEL = TextColor.color(0x8EA8B5)
    private val VALUE = TextColor.color(0xF4FBFF)
    private val ACTION_BULLET = TextColor.color(0x4E6572)
    private val ACTION = TextColor.color(0xFFE082)
    private val ACTION_CLICK = TextColor.color(0x9BE0EE)

    fun title(text: String): Component {
        return Component.text(text, TITLE)
            .decoration(TextDecoration.ITALIC, false)
    }

    fun section(text: String): Component {
        return Component.text(text, SECTION)
            .decoration(TextDecoration.ITALIC, false)
    }

    fun hint(text: String): Component {
        return Component.text(text, HINT)
            .decoration(TextDecoration.ITALIC, false)
    }

    fun action(text: String): Component {
        val tokens = text.split(' ', limit = 3)
        val clickType = tokens.getOrNull(0).orEmpty()
        val remainder = tokens.drop(1).joinToString(" ")

        return Component.text("• ", ACTION_BULLET)
            .decoration(TextDecoration.ITALIC, false)
            .append(Component.text(clickType.uppercase(Locale.ENGLISH), ACTION_CLICK).decoration(TextDecoration.ITALIC, false))
            .append(Component.text(if (remainder.isBlank()) "" else " $remainder", ACTION).decoration(TextDecoration.ITALIC, false))
    }

    fun wrappedHints(lines: Iterable<String>): List<Component> {
        return lines.flatMap(::wrapLine).map(::hint)
    }

    fun wrappedActions(lines: Iterable<String>): List<Component> {
        return lines.map(::action)
    }

    fun valueLine(label: String, value: String): Component {
        return Component.text(label, LABEL)
            .decoration(TextDecoration.ITALIC, false)
            .append(
                Component.text(value.ifBlank { "<empty>" }, VALUE)
                    .decoration(TextDecoration.ITALIC, false)
            )
    }

    fun wrappedValueLines(label: String, value: String): List<Component> {
        val normalized = value.ifBlank { "<empty>" }
        val wrapped = wrapLine(normalized, WRAP_WIDTH - label.length.coerceAtMost(WRAP_WIDTH - 8))
        if (wrapped.isEmpty()) {
            return listOf(valueLine(label, normalized))
        }

        return buildList {
            add(valueLine(label, wrapped.first()))
            wrapped.drop(1).forEach { line ->
                add(valueLine("  ", line))
            }
        }
    }

    fun rawValue(raw: String): String {
        return raw.ifBlank { "<empty>" }.take(160)
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

    private fun wrapLine(text: String, width: Int = WRAP_WIDTH): List<String> {
        if (text.isBlank()) {
            return listOf("")
        }

        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder()
            }
        }

        for (word in words) {
            if (word.length > width) {
                flush()
                var start = 0
                while (start < word.length) {
                    val end = (start + width).coerceAtMost(word.length)
                    lines += word.substring(start, end)
                    start = end
                }
                continue
            }

            val candidateLength = if (current.isEmpty()) word.length else current.length + 1 + word.length
            if (candidateLength > width) {
                flush()
            }

            if (current.isNotEmpty()) {
                current.append(' ')
            }
            current.append(word)
        }

        flush()
        return lines
    }
}
