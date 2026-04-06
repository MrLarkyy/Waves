package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import gg.aquatic.kmenu.inventory.ButtonType
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

data class EditorFieldContext(
    val label: String,
    val path: String,
    val pathSegments: List<String>,
    val description: List<String>,
    val descriptor: SerialDescriptor,
    val value: YamlNode,
    val root: YamlNode,
)

sealed interface FieldEditResult {
    data object NoChange : FieldEditResult
    data object PassThrough : FieldEditResult
    data class Updated(val value: YamlNode) : FieldEditResult
    data class UpdatedRoot(val value: YamlNode) : FieldEditResult
}

fun interface EntryFactory {
    suspend fun create(player: org.bukkit.entity.Player, context: EditorFieldContext): YamlNode?
}

fun interface MapEntryFactory {
    suspend fun create(player: org.bukkit.entity.Player, context: EditorFieldContext): Pair<String, YamlNode>?
}

interface EditorFieldAdapter {
    fun createItem(context: EditorFieldContext, defaultItem: () -> ItemStack): ItemStack = defaultItem()

    suspend fun edit(player: org.bukkit.entity.Player, context: EditorFieldContext): FieldEditResult = FieldEditResult.NoChange

    suspend fun edit(
        player: org.bukkit.entity.Player,
        context: EditorFieldContext,
        buttonType: ButtonType
    ): FieldEditResult = edit(player, context)
}

interface ConfigurableFieldAdapter<C : Any> {
    fun createItem(context: EditorFieldContext, config: C, defaultItem: () -> ItemStack): ItemStack = defaultItem()

    suspend fun edit(player: org.bukkit.entity.Player, context: EditorFieldContext, config: C): FieldEditResult = FieldEditResult.NoChange

    suspend fun edit(
        player: org.bukkit.entity.Player,
        context: EditorFieldContext,
        config: C,
        buttonType: ButtonType
    ): FieldEditResult = edit(player, context, config)
}

fun <C : Any> ConfigurableFieldAdapter<C>.bind(config: C): EditorFieldAdapter {
    val delegate = this
    return object : EditorFieldAdapter {
        override fun createItem(context: EditorFieldContext, defaultItem: () -> ItemStack): ItemStack {
            return delegate.createItem(context, config, defaultItem)
        }

        override suspend fun edit(player: org.bukkit.entity.Player, context: EditorFieldContext): FieldEditResult {
            return delegate.edit(player, context, config)
        }

        override suspend fun edit(
            player: org.bukkit.entity.Player,
            context: EditorFieldContext,
            buttonType: ButtonType
        ): FieldEditResult {
            return delegate.edit(player, context, config, buttonType)
        }
    }
}

data class FieldMeta(
    val pattern: String,
    val displayName: String? = null,
    val description: List<String> = emptyList(),
    val prompt: String? = null,
    val iconMaterial: Material? = null,
    val adapter: EditorFieldAdapter = DefaultEditorFieldAdapter,
    val visibleWhen: (EditorFieldContext) -> Boolean = { true },
    val newValueFactory: EntryFactory? = null,
    val newMapEntryFactory: MapEntryFactory? = null,
    val mapKeyPrompt: String? = null,
) {
    fun matches(pathSegments: List<String>): Boolean {
        val patternSegments = pattern.split('.').filter { it.isNotBlank() }
        if (patternSegments.size != pathSegments.size) return false

        return patternSegments.zip(pathSegments).all { (patternSegment, actualSegment) ->
            patternSegment == "*" || patternSegment == actualSegment
        }
    }
}

object DefaultEditorFieldAdapter : EditorFieldAdapter

interface EditorSchema<T> {
    val serializer: KSerializer<T>
    val fields: List<FieldMeta>

    fun resolve(context: EditorFieldContext): FieldMeta? {
        return fields.lastOrNull { it.matches(context.pathSegments) }
    }

    fun resolveDescriptor(context: EditorFieldContext): SerialDescriptor? = null
}

class EditorSchemaBuilder<T>(
    override val serializer: KSerializer<T>
) : EditorSchema<T> {
    private val mutableFields = mutableListOf<FieldMeta>()

    override val fields: List<FieldMeta>
        get() = mutableFields

    fun field(
        pattern: String,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        adapter: EditorFieldAdapter = DefaultEditorFieldAdapter,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
        newValueFactory: EntryFactory? = null,
        newMapEntryFactory: MapEntryFactory? = null,
        mapKeyPrompt: String? = null,
    ) {
        mutableFields += FieldMeta(
            pattern = pattern,
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            adapter = adapter,
            visibleWhen = visibleWhen,
            newValueFactory = newValueFactory,
            newMapEntryFactory = newMapEntryFactory,
            mapKeyPrompt = mapKeyPrompt
        )
    }

    fun <C : Any> field(
        pattern: String,
        adapter: ConfigurableFieldAdapter<C>,
        config: C,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
        newValueFactory: EntryFactory? = null,
        newMapEntryFactory: MapEntryFactory? = null,
        mapKeyPrompt: String? = null,
    ) {
        field(
            pattern = pattern,
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            adapter = adapter.bind(config),
            visibleWhen = visibleWhen,
            newValueFactory = newValueFactory,
            newMapEntryFactory = newMapEntryFactory,
            mapKeyPrompt = mapKeyPrompt
        )
    }
}

fun <T> editorSchema(
    serializer: KSerializer<T>,
    block: EditorSchemaBuilder<T>.() -> Unit
): EditorSchema<T> {
    return EditorSchemaBuilder(serializer).apply(block)
}
