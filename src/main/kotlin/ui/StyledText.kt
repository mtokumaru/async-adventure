package ui

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles.*

/**
 * Represents a single line of text with optional color and style.
 * Used for simple styled text within TextBlocks.
 */
data class StyledLine(
    val text: String,
    val color: TextColors? = null,
    val style: ((String) -> String)? = null
) {
    override fun toString(): String {
        var result = text
        color?.let { result = it(result) }
        style?.let { result = it(result) }
        return result
    }
}

/**
 * Base class for simple text blocks composed of styled and plain lines.
 * Use this for static content that doesn't need conditional formatting.
 */
open class TextBlock(private val parts: List<Any>) {
    constructor(vararg parts: Any) : this(parts.toList())

    override fun toString() = parts.joinToString("\n")
}

/**
 * Abstract base for complex blocks requiring line-by-line conditional formatting.
 * Override render() to implement custom formatting logic.
 */
abstract class FormattedBlock {
    abstract fun render(): String
    override fun toString() = render()
}
