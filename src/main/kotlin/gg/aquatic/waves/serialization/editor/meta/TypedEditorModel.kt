package gg.aquatic.waves.serialization.editor.meta

import kotlinx.serialization.KSerializer
import org.bukkit.Material
import kotlin.reflect.KProperty1

abstract class EditableModel<T>(
    serializer: KSerializer<T>
) : EditorSchema<T> {

    final override val serializer: KSerializer<T> = serializer

    final override val fields: List<FieldMeta> by lazy {
        typedEditorSchema(serializer) {
            define()
        }.fields
    }

    protected abstract fun TypedEditorSchemaBuilder<T>.define()
}

class TypedEditorSchemaBuilder<T>(
    serializer: KSerializer<T>,
    private val delegate: EditorSchemaBuilder<T> = EditorSchemaBuilder(serializer),
    private val prefix: List<String> = emptyList(),
    private val visibility: (EditorFieldContext) -> Boolean = { true },
) : EditorSchema<T> by delegate {

    fun <V> field(
        property: KProperty1<T, V>,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        adapter: EditorFieldAdapter = DefaultEditorFieldAdapter,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
    ) {
        delegate.field(
            pattern = (prefix + property.name).joinToString("."),
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            adapter = adapter,
            visibleWhen = combineVisibleWhen(visibleWhen)
        )
    }

    fun <V, C : Any> field(
        property: KProperty1<T, V>,
        adapter: ConfigurableFieldAdapter<C>,
        config: C,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
    ) {
        delegate.field(
            pattern = (prefix + property.name).joinToString("."),
            adapter = adapter,
            config = config,
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            visibleWhen = combineVisibleWhen(visibleWhen)
        )
    }

    fun <V> group(
        property: KProperty1<T, V>,
        block: TypedNestedSchemaBuilder<V>.() -> Unit
    ) {
        TypedNestedSchemaBuilder<V>(delegate, prefix + property.name, visibility).apply(block)
    }

    fun <V : Any> optionalGroup(
        property: KProperty1<T, V?>,
        block: TypedNestedSchemaBuilder<V>.() -> Unit
    ) {
        TypedNestedSchemaBuilder<V>(delegate, prefix + property.name, visibility).apply(block)
    }

    fun <V> list(
        property: KProperty1<T, List<V>>,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        adapter: EditorFieldAdapter = DefaultEditorFieldAdapter,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
        newValueFactory: EntryFactory? = null,
        block: (TypedNestedSchemaBuilder<V>.() -> Unit)? = null
    ) {
        delegate.field(
            pattern = (prefix + property.name).joinToString("."),
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            adapter = adapter,
            visibleWhen = combineVisibleWhen(visibleWhen),
            newValueFactory = newValueFactory
        )
        block?.let {
            TypedNestedSchemaBuilder<V>(delegate, prefix + property.name + "*", visibility).apply(it)
        }
    }

    fun <V> map(
        property: KProperty1<T, Map<String, V>>,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        adapter: EditorFieldAdapter = DefaultEditorFieldAdapter,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
        newMapEntryFactory: MapEntryFactory? = null,
        mapKeyPrompt: String? = null,
        block: (TypedNestedSchemaBuilder<V>.() -> Unit)? = null
    ) {
        delegate.field(
            pattern = (prefix + property.name).joinToString("."),
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            adapter = adapter,
            visibleWhen = combineVisibleWhen(visibleWhen),
            newMapEntryFactory = newMapEntryFactory,
            mapKeyPrompt = mapKeyPrompt
        )
        block?.let {
            TypedNestedSchemaBuilder<V>(delegate, prefix + property.name + "*", visibility).apply(it)
        }
    }

    fun <S> include(
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
        block: TypedNestedSchemaBuilder<S>.() -> Unit
    ) {
        TypedNestedSchemaBuilder<S>(delegate, prefix, combineVisibleWhen(visibleWhen)).apply(block)
    }

    private fun combineVisibleWhen(visibleWhen: (EditorFieldContext) -> Boolean): (EditorFieldContext) -> Boolean {
        return { context -> visibility(context) && visibleWhen(context) }
    }
}

class TypedNestedSchemaBuilder<T>(
    private val delegate: EditorSchemaBuilder<*>,
    private val prefix: List<String>,
    private val visibility: (EditorFieldContext) -> Boolean = { true },
) {

    fun <V> field(
        property: KProperty1<T, V>,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        adapter: EditorFieldAdapter = DefaultEditorFieldAdapter,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
    ) {
        delegate.field(
            pattern = (prefix + property.name).joinToString("."),
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            adapter = adapter,
            visibleWhen = combineVisibleWhen(visibleWhen)
        )
    }

    fun <V, C : Any> field(
        property: KProperty1<T, V>,
        adapter: ConfigurableFieldAdapter<C>,
        config: C,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
    ) {
        delegate.field(
            pattern = (prefix + property.name).joinToString("."),
            adapter = adapter,
            config = config,
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            visibleWhen = combineVisibleWhen(visibleWhen)
        )
    }

    fun <V> group(
        property: KProperty1<T, V>,
        block: TypedNestedSchemaBuilder<V>.() -> Unit
    ) {
        TypedNestedSchemaBuilder<V>(delegate, prefix + property.name, visibility).apply(block)
    }

    fun <V : Any> optionalGroup(
        property: KProperty1<T, V?>,
        block: TypedNestedSchemaBuilder<V>.() -> Unit
    ) {
        TypedNestedSchemaBuilder<V>(delegate, prefix + property.name, visibility).apply(block)
    }

    fun <V> list(
        property: KProperty1<T, List<V>>,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        adapter: EditorFieldAdapter = DefaultEditorFieldAdapter,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
        newValueFactory: EntryFactory? = null,
        block: (TypedNestedSchemaBuilder<V>.() -> Unit)? = null
    ) {
        delegate.field(
            pattern = (prefix + property.name).joinToString("."),
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            adapter = adapter,
            visibleWhen = combineVisibleWhen(visibleWhen),
            newValueFactory = newValueFactory
        )
        block?.let {
            TypedNestedSchemaBuilder<V>(delegate, prefix + property.name + "*", visibility).apply(it)
        }
    }

    fun <V> map(
        property: KProperty1<T, Map<String, V>>,
        displayName: String? = null,
        description: List<String> = emptyList(),
        prompt: String? = null,
        iconMaterial: Material? = null,
        adapter: EditorFieldAdapter = DefaultEditorFieldAdapter,
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
        newMapEntryFactory: MapEntryFactory? = null,
        mapKeyPrompt: String? = null,
        block: (TypedNestedSchemaBuilder<V>.() -> Unit)? = null
    ) {
        delegate.field(
            pattern = (prefix + property.name).joinToString("."),
            displayName = displayName,
            description = description,
            prompt = prompt,
            iconMaterial = iconMaterial,
            adapter = adapter,
            visibleWhen = combineVisibleWhen(visibleWhen),
            newMapEntryFactory = newMapEntryFactory,
            mapKeyPrompt = mapKeyPrompt
        )
        block?.let {
            TypedNestedSchemaBuilder<V>(delegate, prefix + property.name + "*", visibility).apply(it)
        }
    }

    fun <S> include(
        visibleWhen: (EditorFieldContext) -> Boolean = { true },
        block: TypedNestedSchemaBuilder<S>.() -> Unit
    ) {
        TypedNestedSchemaBuilder<S>(delegate, prefix, combineVisibleWhen(visibleWhen)).apply(block)
    }

    private fun combineVisibleWhen(visibleWhen: (EditorFieldContext) -> Boolean): (EditorFieldContext) -> Boolean {
        return { context -> visibility(context) && visibleWhen(context) }
    }
}

fun <T> typedEditorSchema(
    serializer: KSerializer<T>,
    block: TypedEditorSchemaBuilder<T>.() -> Unit
): EditorSchema<T> {
    return TypedEditorSchemaBuilder(serializer).apply(block)
}
