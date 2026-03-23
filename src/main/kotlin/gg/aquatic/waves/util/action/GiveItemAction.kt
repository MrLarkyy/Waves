package gg.aquatic.waves.util.action

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.common.argument.ObjectArgument
import gg.aquatic.execute.action.type.PlayerAction
import gg.aquatic.stacked.impl.StackedItemImpl
import gg.aquatic.waves.util.argument.ItemArgument
import org.bukkit.Material
import org.bukkit.entity.Player
import gg.aquatic.stacked.toCustomItem

object GiveItemAction : PlayerAction() {
    override suspend fun execute(
        binder: Player,
        args: ArgumentContext<Player>
    ) {
        val item = args.any("item") as? StackedItemImpl ?: return
        item.giveItem(binder)
    }

    override val arguments: List<ObjectArgument<*>> = listOf(
        ItemArgument("item", Material.STONE.toCustomItem(), true)
    )
}
