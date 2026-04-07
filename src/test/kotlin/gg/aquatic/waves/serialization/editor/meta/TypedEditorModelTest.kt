package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlPath
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TypedEditorModelTest {

    @Test
    fun `list nested fields resolve using separate wildcard segment`() {
        val schema = typedEditorSchema(ListRoot.serializer()) {
            list(ListRoot::items) {
                field(ListEntry::name, displayName = "Entry Name")
            }
        }

        val field = schema.resolve(
            EditorFieldContext(
                label = "name",
                path = "items.0.name",
                pathSegments = listOf("items", "0", "name"),
                description = emptyList(),
                descriptor = ListEntry.serializer().descriptor.getElementDescriptor(0),
                value = YamlScalar("hello", YamlPath.root),
                root = YamlScalar("root", YamlPath.root)
            )
        )

        assertNotNull(field)
        assertEquals("Entry Name", field.displayName)
    }

    @Test
    fun `map nested fields resolve using separate wildcard segment`() {
        val schema = typedEditorSchema(MapRoot.serializer()) {
            map(MapRoot::entries) {
                field(MapEntry::value, displayName = "Entry Value")
            }
        }

        val field = schema.resolve(
            EditorFieldContext(
                label = "value",
                path = "entries.test.value",
                pathSegments = listOf("entries", "test", "value"),
                description = emptyList(),
                descriptor = MapEntry.serializer().descriptor.getElementDescriptor(0),
                value = YamlScalar("hello", YamlPath.root),
                root = YamlScalar("root", YamlPath.root)
            )
        )

        assertNotNull(field)
        assertEquals("Entry Value", field.displayName)
    }

    @Serializable
    private data class ListRoot(
        val items: List<ListEntry> = emptyList()
    )

    @Serializable
    private data class ListEntry(
        val name: String = ""
    )

    @Serializable
    private data class MapRoot(
        val entries: Map<String, MapEntry> = emptyMap()
    )

    @Serializable
    private data class MapEntry(
        val value: String = ""
    )
}
