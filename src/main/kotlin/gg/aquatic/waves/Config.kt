package gg.aquatic.waves

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException

class Config {
    private var file: File
    private var config: FileConfiguration? = null
    private var main: JavaPlugin

    constructor(file: File, main: JavaPlugin) {
        this.main = main
        this.file = file
    }
    constructor(path: String, main: JavaPlugin) {
        this.main = main
        file = File(main.dataFolder, path)
    }

    suspend fun load() = withContext(Dispatchers.IO) {
        loadSync()
    }

    fun loadSync() {
        if (!file.exists()) {
            try {
                main.saveResource(file.name, false)
            } catch (_: IllegalArgumentException) {
                try {
                    file.createNewFile()
                } catch (var3: IOException) {
                    var3.printStackTrace()
                }
            }
        }
        config = YamlConfiguration.loadConfiguration(file)
    }

    fun getConfiguration(): FileConfiguration {
        return config ?: error("Configuration has not been loaded!")
    }

    suspend fun save() = withContext(Dispatchers.IO) {
        saveSync()
    }

    fun saveSync() {
        try {
            config!!.save(file)
        } catch (var2: IOException) {
            var2.printStackTrace()
        }
    }

    fun getFile(): File {
        return file
    }
}