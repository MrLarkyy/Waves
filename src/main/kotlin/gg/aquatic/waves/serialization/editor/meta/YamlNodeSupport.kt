package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlPath
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

private val rootYamlPath = YamlPath.root

fun yamlScalar(content: Any?): YamlScalar = YamlScalar(content?.toString() ?: "", rootYamlPath)

fun yamlNull(): YamlNull = YamlNull(rootYamlPath)

fun yamlList(items: List<YamlNode>): YamlList = YamlList(items, rootYamlPath)

fun yamlMap(entries: Map<String, YamlNode>): YamlMap = YamlMap(
    entries.entries.associate { (key, value) -> yamlScalar(key) to value },
    rootYamlPath
)

val YamlNode.stringContentOrNull: String?
    get() = (this as? YamlScalar)?.content

val YamlNode.booleanOrNull: Boolean?
    get() = stringContentOrNull?.toBooleanStrictOrNull()

val YamlNode.intOrNull: Int?
    get() = stringContentOrNull?.toIntOrNull()

val YamlNode.longOrNull: Long?
    get() = stringContentOrNull?.toLongOrNull()

val YamlNode.floatOrNull: Float?
    get() = stringContentOrNull?.toFloatOrNull()

val YamlNode.doubleOrNull: Double?
    get() = stringContentOrNull?.toDoubleOrNull()

fun YamlNode.displayString(): String {
    return when (this) {
        is YamlNull -> "null"
        is YamlMap -> "${entries.size} entries"
        is YamlList -> "${items.size} items"
        else -> stringContentOrNull ?: toString()
    }
}

fun YamlNode.getMapValue(key: String): YamlNode? = (this as? YamlMap)?.get<YamlNode>(key)

fun YamlNode.getListValue(index: Int): YamlNode? = (this as? YamlList)?.items?.getOrNull(index)

@OptIn(ExperimentalSerializationApi::class)
fun defaultYamlElement(descriptor: SerialDescriptor, useNullForNullable: Boolean = true): YamlNode {
    if (descriptor.isNullable && useNullForNullable) return yamlNull()

    return when (descriptor.kind) {
        is kotlinx.serialization.descriptors.PolymorphicKind -> yamlMap(emptyMap())
        StructureKind.CLASS, StructureKind.OBJECT -> yamlMap(
            buildMap {
                repeat(descriptor.elementsCount) { index ->
                    put(
                        descriptor.getElementName(index),
                        defaultYamlElement(descriptor.getElementDescriptor(index))
                    )
                }
            }
        )

        StructureKind.LIST -> yamlList(emptyList())
        StructureKind.MAP -> yamlMap(emptyMap())
        PrimitiveKind.STRING -> yamlScalar("")
        PrimitiveKind.BOOLEAN -> yamlScalar("false")
        PrimitiveKind.INT -> yamlScalar("0")
        PrimitiveKind.LONG -> yamlScalar("0")
        PrimitiveKind.FLOAT -> yamlScalar("0.0")
        PrimitiveKind.DOUBLE -> yamlScalar("0.0")
        PrimitiveKind.BYTE -> yamlScalar("0")
        PrimitiveKind.SHORT -> yamlScalar("0")
        SerialKind.ENUM -> yamlScalar(descriptor.getElementName(0))
        else -> yamlScalar("")
    }
}
