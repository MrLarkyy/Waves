package gg.aquatic.waves.serialization.editor

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.FieldMeta
import gg.aquatic.waves.serialization.editor.meta.getListValue
import gg.aquatic.waves.serialization.editor.meta.getMapValue
import gg.aquatic.waves.serialization.editor.meta.yamlFieldKey
import gg.aquatic.waves.serialization.editor.meta.yamlList
import gg.aquatic.waves.serialization.editor.meta.yamlMap
import gg.aquatic.waves.serialization.editor.meta.yamlNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor

internal data class EditorEntry(
    val label: String,
    val descriptor: SerialDescriptor,
    val element: YamlNode,
    val root: YamlNode,
    val path: List<PathSegment>,
    val removable: Boolean,
    val kind: NodeKind,
    val meta: FieldMeta?,
)

internal data class NodeEditorState(
    val resolvedDescriptor: SerialDescriptor,
    val entries: List<EditorEntry>,
)

internal enum class NodeKind {
    OBJECT,
    LIST,
    MAP,
    STRING,
    BOOLEAN,
    NUMBER,
    ENUM
}

internal sealed interface PathSegment {
    data class Key(val value: String) : PathSegment
    data class Index(val value: Int) : PathSegment
}

internal class SerializableEditorDocument<T>(
    val yaml: Yaml,
    private val serializer: KSerializer<T>,
    value: T
) {
    private var root: YamlNode = yaml.parseToYamlNode(yaml.encodeToString(serializer, value))

    fun root(): YamlNode = root

    fun replaceRoot(value: YamlNode) {
        root = value
    }

    fun decode(): T {
        return yaml.decodeFromYamlNode(serializer, root)
    }

    fun get(path: List<PathSegment>): YamlNode {
        var current = root
        for (segment in path) {
            current = when (segment) {
                is PathSegment.Key -> current.resolveMapValue(segment.value) ?: yamlNull()
                is PathSegment.Index -> current.getListValue(segment.value) ?: yamlNull()
            }
        }
        return current
    }

    fun set(path: List<PathSegment>, value: YamlNode) {
        root = update(root, path, value)
    }

    fun remove(path: List<PathSegment>) {
        root = removeAt(root, path)
    }

    fun addToList(path: List<PathSegment>, value: YamlNode) {
        val current = get(path) as? YamlList ?: yamlList(emptyList())
        set(path, yamlList(current.items + value))
    }

    fun addAllToList(path: List<PathSegment>, values: YamlList) {
        val current = get(path) as? YamlList ?: yamlList(emptyList())
        set(path, yamlList(current.items + values.items))
    }

    fun putToMap(path: List<PathSegment>, key: String, value: YamlNode) {
        val current = get(path) as? YamlMap ?: yamlMap(emptyMap())
        val mutable = current.entries.entries.associate { it.key.content to it.value }.toMutableMap()
        mutable[key] = value
        set(path, yamlMap(mutable))
    }

    private fun update(current: YamlNode, path: List<PathSegment>, value: YamlNode, depth: Int = 0): YamlNode {
        if (depth >= path.size) return value

        return when (val segment = path[depth]) {
            is PathSegment.Key -> {
                val obj = current as? YamlMap ?: yamlMap(emptyMap())
                val mutable = obj.entries.entries.associate { it.key.content to it.value }.toMutableMap()
                val yamlKey = obj.resolveYamlKey(segment.value)
                mutable[yamlKey] = update(mutable[yamlKey] ?: yamlNull(), path, value, depth + 1)
                yamlMap(mutable)
            }

            is PathSegment.Index -> {
                val arr = (current as? YamlList)?.items?.toMutableList() ?: mutableListOf()
                while (arr.size <= segment.value) {
                    arr += yamlNull()
                }
                arr[segment.value] = update(arr[segment.value], path, value, depth + 1)
                yamlList(arr)
            }
        }
    }

    private fun removeAt(current: YamlNode, path: List<PathSegment>, depth: Int = 0): YamlNode {
        if (depth >= path.size) return current

        return when (val segment = path[depth]) {
            is PathSegment.Key -> {
                val obj = current as? YamlMap ?: return current
                val currentEntries = obj.entries.entries.associate { it.key.content to it.value }
                val yamlKey = obj.resolveYamlKey(segment.value)
                if (depth == path.lastIndex) {
                    yamlMap(currentEntries.toMutableMap().apply { remove(yamlKey) })
                } else {
                    val mutable = currentEntries.toMutableMap()
                    val existing = mutable[yamlKey] ?: return current
                    mutable[yamlKey] = removeAt(existing, path, depth + 1)
                    yamlMap(mutable)
                }
            }

            is PathSegment.Index -> {
                val arr = current as? YamlList ?: return current
                val mutable = arr.items.toMutableList()
                if (segment.value !in mutable.indices) return current
                if (depth == path.lastIndex) {
                    mutable.removeAt(segment.value)
                } else {
                    mutable[segment.value] = removeAt(mutable[segment.value], path, depth + 1)
                }
                yamlList(mutable)
            }
        }
    }
}

private fun YamlNode.resolveMapValue(key: String): YamlNode? {
    val map = this as? YamlMap ?: return null
    val yamlKey = map.resolveYamlKey(key)
    return map.getMapValue(yamlKey)
}

private fun YamlMap.resolveYamlKey(key: String): String {
    if (entries.keys.any { it.content == key }) {
        return key
    }

    val kebab = yamlFieldKey(key)
    if (kebab != key && entries.keys.any { it.content == kebab }) {
        return kebab
    }

    return kebab
}

internal fun EditorEntry.toFieldContext(root: YamlNode): EditorFieldContext {
    return EditorFieldContext(
        label = label,
        path = path.toSchemaPath(),
        pathSegments = path.toSchemaSegments(),
        description = meta?.description.orEmpty(),
        descriptor = descriptor,
        value = element,
        root = root
    )
}

internal fun List<PathSegment>.toSchemaSegments(): List<String> {
    return map {
        when (it) {
            is PathSegment.Key -> it.value
            is PathSegment.Index -> it.value.toString()
        }
    }
}

internal fun List<PathSegment>.toSchemaPath(): String {
    return toSchemaSegments().joinToString(".")
}
