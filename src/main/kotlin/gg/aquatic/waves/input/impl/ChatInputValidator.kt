package gg.aquatic.waves.input.impl

import org.bukkit.entity.Player

class ChatInputValidator {
    private var validation: (String) -> Boolean = { true }
    private var onFail: (Player, String) -> Unit = { _, _ -> }

    fun validate(predicate: (String) -> Boolean) {
        this.validation = predicate
    }

    fun onFail(block: (Player, String) -> Unit) {
        this.onFail = block
    }

    fun isValid(player: Player, input: String): Boolean {
        val result = validation(input)
        if (!result) onFail(player, input)
        return result
    }
}

fun chatInputValidation(block: ChatInputValidator.() -> Unit): ChatInputValidator {
    return ChatInputValidator().apply(block)
}
