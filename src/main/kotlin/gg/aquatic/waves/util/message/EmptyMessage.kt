package gg.aquatic.waves.util.message

import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.klocale.message.Message

object EmptyMessage : Message<PaperMessage> by PaperMessage.of(emptyList())