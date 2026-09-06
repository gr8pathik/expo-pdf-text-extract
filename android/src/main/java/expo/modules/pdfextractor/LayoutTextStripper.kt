package expo.modules.pdfextractor

import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.roundToInt

/**
 * A [PDFTextStripper] that keeps the page's column layout.
 *
 * The default stripper returns reading order with horizontal position
 * discarded, even with `sortByPosition`. On a bank statement that erases the
 * difference between a transaction row and the continuation line printed
 * beneath it — a foreign currency amount such as "19/07/26 24.98 USD" is
 * indented under its parent row, and flattened it reads as a row of its own.
 * Downstream that becomes a phantom transaction, with a guessed direction, in
 * the user's totals.
 *
 * PDFBox hands every glyph's geometry to `writeString`, so the glyphs are
 * collected per line and re-spaced from their x offsets. Column width is the
 * font's own space advance (`widthOfSpace`), which is the unit the original
 * layout was set in.
 */
internal class LayoutTextStripper : PDFTextStripper() {

  private val lines = mutableListOf<List<TextPosition>>()
  private var current = mutableListOf<TextPosition>()

  init {
    sortByPosition = true
  }

  override fun writeString(text: String, textPositions: List<TextPosition>) {
    current.addAll(textPositions)
  }

  override fun writeLineSeparator() {
    if (current.isNotEmpty()) {
      lines.add(current)
      current = mutableListOf()
    }
  }

  /** The laid-out text. Call after [getText]; empty when the page has no text layer. */
  fun layoutText(): String {
    if (current.isNotEmpty()) {
      lines.add(current)
      current = mutableListOf()
    }
    if (lines.isEmpty()) return ""

    val all = lines.flatten()

    // Median rather than mean: one oversized heading glyph must not stretch
    // the whole grid. A page that reports no space advance falls back to a
    // typical 10pt body space.
    val spaces = all.map { it.widthOfSpace }.filter { it > 0f }.sorted()
    val spaceWidth = if (spaces.isEmpty()) 4f else spaces[spaces.size / 2]
    val originX = all.minOf { it.xDirAdj }

    return lines.joinToString("\n") { line ->
      val out = StringBuilder()

      // Spacing comes from the gap between one glyph and the next, not from an
      // absolute grid: glyph advance is smaller than a space, so placing each
      // glyph at `x / spaceWidth` inserts a space inside every word
      // ("E u _ S m a rty m e a p p"). Only the line's first glyph is placed
      // absolutely — that is the indent the layout carries.
      var cursor: Float? = null

      for (position in line.sortedBy { it.xDirAdj }) {
        // Space glyphs are dropped and re-derived from the gap: honouring both
        // the printed space and the gap it leaves would double it.
        if (position.unicode.isBlank()) continue

        val here = cursor
        if (here == null) {
          out.append(" ".repeat(maxOf(0, ((position.xDirAdj - originX) / spaceWidth).roundToInt())))
        } else {
          val gap = position.xDirAdj - here
          if (gap > spaceWidth * 0.5f) {
            out.append(" ".repeat(maxOf(1, (gap / spaceWidth).roundToInt())))
          }
        }

        out.append(position.unicode)
        cursor = position.xDirAdj + position.widthDirAdj
      }

      out.toString().trimEnd()
    }
  }
}
