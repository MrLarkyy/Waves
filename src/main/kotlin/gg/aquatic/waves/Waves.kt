package gg.aquatic.waves

import gg.aquatic.blokk.factory.BlockFactory
import gg.aquatic.blokk.factory.IAFactory
import gg.aquatic.blokk.factory.NexoFactory
import gg.aquatic.blokk.factory.OraxenFactory
import gg.aquatic.blokk.initializeBlokk
import gg.aquatic.clientside.initializeClientside
import gg.aquatic.common.Config
import gg.aquatic.common.MiniMessageResolver
import gg.aquatic.common.coroutine.SingleThreadedContext
import gg.aquatic.common.event
import gg.aquatic.common.initializeCommon
import gg.aquatic.execute.Action
import gg.aquatic.execute.initializeExecute
import gg.aquatic.kholograms.HologramHandler
import gg.aquatic.klocale.LocaleManager
import gg.aquatic.klocale.impl.paper.KLocale
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.kmenu.initializeKMenu
import gg.aquatic.kregistry.bootstrap.BootstrapHolder
import gg.aquatic.kregistry.bootstrap.RegistryHolder
import gg.aquatic.kurrency.CurrencyCache
import gg.aquatic.kurrency.cache.HybridCurrencyCache
import gg.aquatic.kurrency.cache.LocalCurrencyCache
import gg.aquatic.kurrency.initializeKurrency
import gg.aquatic.pakket.Pakket
import gg.aquatic.quickminimessage.MMParser
import gg.aquatic.stacked.ItemFactory
import gg.aquatic.stacked.factory.Base64Factory
import gg.aquatic.stacked.factory.CraftEngineFactory
import gg.aquatic.stacked.factory.EcoFactory
import gg.aquatic.stacked.factory.EIFactory
import gg.aquatic.stacked.factory.HDBFactory
import gg.aquatic.stacked.factory.IAFactory as ItemIAFactory
import gg.aquatic.stacked.factory.MMFactory
import gg.aquatic.stacked.factory.MMOFactory
import gg.aquatic.stacked.factory.NexoFactory as ItemNexoFactory
import gg.aquatic.stacked.factory.OraxenFactory as ItemOraxenFactory
import gg.aquatic.stacked.factory.RegistryFactory
import gg.aquatic.stacked.initializeStacked
import gg.aquatic.statistik.initializeStatistik
import gg.aquatic.waves.input.impl.ChatInput
import gg.aquatic.waves.input.initializeInput
import gg.aquatic.waves.testing.data.TestingEditor
import gg.aquatic.waves.util.action.BossbarAction
import gg.aquatic.waves.util.action.GiveItemAction
import gg.aquatic.waves.util.action.MenuNextPageAction
import gg.aquatic.waves.util.action.MenuPreviousPageAction
import gg.aquatic.waves.util.action.MessageAction
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.UnifiedJedis

object Waves : JavaPlugin(), BootstrapHolder, RegistryHolder {

    private lateinit var registriesInject: () -> Unit
    private lateinit var pluginConfig: Config

    override fun onLoad() {
        registriesInject = inject()
    }

    lateinit var locale: LocaleManager<PaperMessage>

    override fun onEnable() {
        pluginConfig = Config("config.yml", this).also { it.loadSync() }

        val mmResolver = MiniMessageResolver {
            MMParser.parse(it)
        }

        initializeCommon(this, mmResolver)
        initializeBlokk(this, createBlockFactories())

        event<PlayerJoinEvent> {
            Pakket.handler.injectPacketListener(it.player)
        }
        event<PlayerQuitEvent> {
            Pakket.handler.unregisterPacketListener(it.player)
        }
        initializeStacked(this, SingleThreadedContext("stacked").scope, mmResolver, createItemFactories())
        initializeKMenu(SingleThreadedContext("kmenu").scope)
        initializeExecute(this, mmResolver)

        registryBootstrap(this) {
            registry(Action.REGISTRY_KEY) {
                add("message", MessageAction)
                add("give-item", GiveItemAction)
                add("bossbar", BossbarAction)
                add("next-page", MenuNextPageAction)
                add("previous-page", MenuPreviousPageAction)
            }
        }

        initializeClientside()
        initializeInput(mapOf("chat" to ChatInput))
        initializeStatistik(emptyMap())
        HologramHandler.initialize()

        TestingEditor.initialize()

        locale = KLocale.paper {}
        initializeKurrency(
            dbUrl = pluginConfig.configuration.getString("kurrency.database.url")
                ?: "jdbc:sqlite:${dataFolder.resolve("kurrency.db").absolutePath.replace("\\", "/")}",
            dbDriver = pluginConfig.configuration.getString("kurrency.database.driver") ?: "org.sqlite.JDBC",
            dbUser = pluginConfig.configuration.getString("kurrency.database.user") ?: "",
            dbPass = pluginConfig.configuration.getString("kurrency.database.password") ?: "",
            cache = { _, dbHandler -> createCurrencyCache(dbHandler) }
        )
        registriesInject()
    }

    private fun createCurrencyCache(dbHandler: gg.aquatic.kurrency.db.CurrencyDBHandler): CurrencyCache {
        val config = pluginConfig.configuration
        val localTtlMinutes = config.getLong("kurrency.cache.local-ttl-minutes", 10L)
        val redisEnabled = config.getBoolean("kurrency.cache.redis.enabled", false)

        return if (redisEnabled) {
            val redisHost = config.getString("kurrency.cache.redis.host", "127.0.0.1") ?: "127.0.0.1"
            val redisPort = config.getInt("kurrency.cache.redis.port", 6379)
            val redisUser = config.getString("kurrency.cache.redis.user", "") ?: ""
            val redisPassword = config.getString("kurrency.cache.redis.password", "") ?: ""
            val redisDatabase = config.getInt("kurrency.cache.redis.database", 0)
            val redisTtlSeconds = config.getLong("kurrency.cache.redis.ttl-seconds", 1800L)

            val redisConfig = DefaultJedisClientConfig.builder()
                .user(if (redisUser.isBlank()) null else redisUser)
                .password(if (redisPassword.isBlank()) null else redisPassword)
                .database(redisDatabase)
                .build()

            val redisCache = gg.aquatic.kurrency.cache.RedisCurrencyCache(
                jedis = UnifiedJedis(HostAndPort(redisHost, redisPort), redisConfig),
                dbHandler = dbHandler,
                ttlSeconds = redisTtlSeconds
            )

            HybridCurrencyCache(
                parent = redisCache,
                localTtlMinutes = localTtlMinutes
            )
        } else {
            LocalCurrencyCache(
                dbHandler = dbHandler,
                ttlMinutes = localTtlMinutes
            )
        }
    }

    private fun createBlockFactories(): Map<String, BlockFactory> {
        val factories = linkedMapOf<String, BlockFactory>()
        if (server.pluginManager.getPlugin("ItemsAdder") != null) {
            factories["ia"] = IAFactory
        }
        if (server.pluginManager.getPlugin("Oraxen") != null) {
            factories["oraxen"] = OraxenFactory
        }
        if (server.pluginManager.getPlugin("Nexo") != null) {
            factories["nexo"] = NexoFactory
        }
        return factories
    }

    private fun createItemFactories(): Map<String, ItemFactory> {
        val factories = linkedMapOf<String, ItemFactory>()
        factories["base64"] = Base64Factory
        factories["registry"] = RegistryFactory
        if (server.pluginManager.getPlugin("ItemsAdder") != null) {
            factories["ia"] = ItemIAFactory
        }
        if (server.pluginManager.getPlugin("Oraxen") != null) {
            factories["oraxen"] = ItemOraxenFactory
        }
        if (server.pluginManager.getPlugin("Nexo") != null) {
            factories["nexo"] = ItemNexoFactory
        }
        if (server.pluginManager.getPlugin("MythicMobs") != null) {
            factories["mm"] = MMFactory
        }
        if (server.pluginManager.getPlugin("MMOItems") != null) {
            factories["mmo"] = MMOFactory
        }
        if (server.pluginManager.getPlugin("HeadDatabase") != null) {
            factories["hdb"] = HDBFactory
        }
        if (server.pluginManager.getPlugin("eco") != null || server.pluginManager.getPlugin("Eco") != null) {
            factories["eco"] = EcoFactory
        }
        if (server.pluginManager.getPlugin("CraftEngine") != null) {
            factories["craftengine"] = CraftEngineFactory
        }
        if (server.pluginManager.getPlugin("ExecutableItems") != null) {
            factories["ei"] = EIFactory
        }
        return factories
    }
}
