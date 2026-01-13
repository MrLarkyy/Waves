package gg.aquatic.waves.util.message

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.common.createConfigurationSectionFromMap
import gg.aquatic.common.getSectionList
import gg.aquatic.execute.action.ActionSerializer
import gg.aquatic.execute.executeActions
import gg.aquatic.klocale.LocaleSerializer
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.klocale.impl.paper.toMMComponent
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

object AquaticMessageSerializer: LocaleSerializer<YamlConfiguration, PaperMessage> {
    override fun parse(data: YamlConfiguration): Map<String, Map<String, PaperMessage>> {
        val result = mutableMapOf<String, MutableMap<String, PaperMessage>>()
        for (string in data.getKeys(false)) {
            val map = HashMap<String, PaperMessage>()
            val lang = data.getConfigurationSection(string) ?: continue
            for (str in lang.getKeys(false)) {
                map[str] = parseMessage(lang, str)
            }
            result[string] = map
        }
        return result
    }

    fun parseMessage(section: ConfigurationSection, key: String): PaperMessage {
        val value = section.get(key) ?: return PaperMessage.of()
        return if (section.isString(key)) {
            PaperMessage.of((value as String).toMMComponent())
        } else if (section.isList(key)) {
            PaperMessage.of((value as List<*>).map { it.toString().toMMComponent() })
        } else {
            val section = section.getConfigurationSection(key)
            if (section == null) {
                if (value is List<*>) {
                    return PaperMessage.of((value.map { it.toString().toMMComponent() }))
                }
                return PaperMessage.of(value.toString().toMMComponent())
            }
            if (section.contains("messages")) {
                val messagesList = section.getList("messages")
                if (messagesList is List<*>) {
                    return parse(section)
                }
            }
            PaperMessage.of()
            /*
            if (section.contains("paginated")) {
                val messageList =
                    section.getList("paginated") ?: emptyList<String>()
                val messages = MessageParser.parse(messageList)
                val pageSize = section.getInt("page-size", 10)
                val header = section.getString("header")
                val footer = section.getString("footer")

                PaginatedMessage(messages, pageSize, header, footer)
            } else SimpleMessage(emptyList())

             */
        }
    }

    private fun parse(section: ConfigurationSection): PaperMessage {
        val actions = ActionSerializer.fromSections<Player>(section.getSectionList("actions"))
        val messageList =
            section.getList("messages") ?: (emptyList<Any>() + section.getList("message"))
        val messages = parse(messageList).map { it.toMMComponent() }
        //val view = MessageView.load(section)
        return PaperMessage.of(messages, listOf { sender, message ->
            VirtualsCtx {
                if (sender is Player) {
                    actions.executeActions(sender)
                }
            }
        })
    }

    fun parse(list: List<*>): List<String> {
        val messages = ArrayList<String>()
        for (any in list) {
            if (any is String) {
                messages.add(any)
                continue
            }
            if (any is ConfigurationSection) {
                messages.add(parseMessage(any))
                continue
            } else if (any is Map<*, *>) {
                messages.add(parseMessage(createConfigurationSectionFromMap(any)))
                continue
            }
        }
        return messages
    }

    private fun parseMessage(section: ConfigurationSection): String {
        val componentSections = section.getSectionList("components")
        val components = componentSections.mapNotNull { parseComponent(it) }
        return components.joinToString("<reset>")
    }

    private fun parseComponent(section: ConfigurationSection): String? {
        var text = section.getString("text") ?: return null
        val clickAction = section.getConfigurationSection("click")?.let {
            ClickAction.load(it)
        }
        val hover = section.getStringList("hover")
        text = clickAction?.bind(text) ?: text
        if (hover.isNotEmpty()) {
            text = "<hover:show_text:'${hover.joinToString("<newline>")}'>$text</hover>"
        }
        return text
    }
}