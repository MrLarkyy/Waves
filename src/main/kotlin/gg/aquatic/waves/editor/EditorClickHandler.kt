package gg.aquatic.waves.editor

import gg.aquatic.kmenu.inventory.ButtonType
import org.bukkit.entity.Player

fun interface EditorClickHandler<T> {
    fun handle(player: Player, current: T, clickType: ButtonType, update: (T?) -> Unit)
}
