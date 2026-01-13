package gg.aquatic.waves.util.argument

import gg.aquatic.execute.argument.ObjectArgument
import gg.aquatic.execute.argument.ObjectArgumentFactory
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.klocale.message.Message
import gg.aquatic.waves.util.message.AquaticMessageSerializer
import org.bukkit.configuration.ConfigurationSection

class MessageArgument(id: String, defaultValue: Message<PaperMessage>?, required: Boolean, aliases: Collection<String> = listOf()) :
    ObjectArgument<Message<PaperMessage>>(
        id, defaultValue,
        required, aliases
    ) {
    override val serializer: ObjectArgumentFactory<Message<PaperMessage>?> = Companion

    companion object : ObjectArgumentFactory<Message<PaperMessage>?>() {
        override fun load(
            section: ConfigurationSection,
            id: String,
        ): Message<PaperMessage>? {

            val msg = AquaticMessageSerializer.parseMessage(section, id)
            if (msg.lines.count() == 0) {
                return null
            }
            return msg
        }
    }
}