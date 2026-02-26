package gg.aquatic.waves.util.action

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.common.argument.ObjectArgument
import gg.aquatic.execute.action.type.PlayerAction
import gg.aquatic.kmenu.PaginatedMenu
import gg.aquatic.kmenu.packetInventory
import org.bukkit.entity.Player

object MenuNextPageAction: PlayerAction() {
    override suspend fun execute(
        binder: Player,
        args: ArgumentContext<Player>
    ) {
        val menu = binder.packetInventory() as? PaginatedMenu ?: return
        menu.handleNextPage()
    }

    override val arguments: List<ObjectArgument<*>> = listOf()
}