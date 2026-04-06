package gg.aquatic.waves.serialization.editor.meta

import com.charleskorn.kaml.YamlNode

internal object NumberFieldSupport {
    fun <T : Comparable<T>> parseNode(
        raw: String,
        invalidMessage: String,
        parse: (String) -> T?,
        render: (T) -> String = { it.toString() },
    ): YamlNode? {
        val parsed = parse(raw) ?: return null
        return yamlScalar(render(parsed))
    }

    fun <T : Comparable<T>> parse(
        raw: String,
        invalidMessage: String,
        min: T? = null,
        max: T? = null,
        parse: (String) -> T?,
        render: (T) -> String = { it.toString() },
    ): Result<YamlNode> {
        val parsed = parse(raw) ?: return Result.failure(IllegalArgumentException(invalidMessage))
        if (min != null && parsed < min) {
            return Result.failure(IllegalArgumentException("Value must be at least $min."))
        }
        if (max != null && parsed > max) {
            return Result.failure(IllegalArgumentException("Value must be at most $max."))
        }
        return Result.success(yamlScalar(render(parsed)))
    }
}
