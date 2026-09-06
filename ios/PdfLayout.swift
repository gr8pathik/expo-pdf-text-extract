import Foundation
import PDFKit

/// Turns a PDF page into text that still carries its column layout.
///
/// Kept out of the module so it can be exercised directly by the tests, which
/// have no Expo runtime to instantiate a `Module` against.
enum PdfLayout {

  /// Page text with its column layout intact.
  ///
  /// `PDFPage.string` returns reading order with all horizontal position
  /// discarded. On a bank statement that erases the difference between a
  /// transaction row and the continuation line printed beneath it — a foreign
  /// currency amount such as "19/07/26 24.98 USD" is indented under its parent
  /// row, and flattened it reads as a row of its own. Downstream that becomes a
  /// phantom transaction, with a guessed direction, in the user's totals.
  ///
  /// Position is taken from line selections rather than `characterBounds(at:)`,
  /// which is not dependable on real statements: it reports glyphs 149pt wide
  /// and places a row's tail a line below its head. `selectionForLine(at:)`
  /// works at the line level, where PDFKit is reliable. Each selection gives
  /// text and a bounding box; fragments sharing a baseline are one visual row,
  /// and each is placed by its left edge.
  static func text(from page: PDFPage) -> String {
    let bounds = page.bounds(for: .mediaBox)

    struct Fragment {
      let y: CGFloat
      let x: CGFloat
      let width: CGFloat
      let text: String
    }

    var seen = Set<String>()
    var fragments: [Fragment] = []

    // Sweep the page for line selections. The vertical step is finer than body
    // leading so no line is stepped over, and the horizontal step is finer than
    // a date column so no column is missed. Duplicates — the same line found
    // from several probe points — are removed by (baseline, left edge, text).
    var probeY = bounds.maxY
    while probeY >= bounds.minY {
      defer { probeY -= 2.0 }

      for probeX in stride(from: bounds.minX + 4, to: bounds.maxX, by: 10.0) {
        guard let selection = page.selectionForLine(at: CGPoint(x: probeX, y: probeY)) else { continue }

        let text = selection.string?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if text.isEmpty { continue }

        let box = selection.bounds(for: page)
        let key = "\(Int(box.minY.rounded()))|\(Int(box.minX.rounded()))|\(text)"
        if seen.contains(key) { continue }
        seen.insert(key)

        fragments.append(Fragment(y: box.minY, x: box.minX, width: box.width, text: text))
      }
    }

    // Nothing measurable — an image-only page, or a PDF whose selections come
    // back empty. The caller's contract is unchanged text, so fall back.
    guard !fragments.isEmpty else { return page.string ?? "" }

    // One column is the page's own average advance: total fragment width over
    // total characters. Derived per page so a statement set in a wide or narrow
    // face indents by the same number of characters either way.
    let totalWidth = fragments.reduce(0.0) { $0 + Double($1.width) }
    let totalChars = fragments.reduce(0.0) { $0 + Double($1.text.count) }
    let columnWidth = totalChars > 0 ? max(2.0, totalWidth / totalChars) : 4.5

    // Fragments within half a line of one another are one visual row. PDF
    // origin is bottom-left, so rows come out top-first by descending y.
    var rows: [[Fragment]] = []
    for fragment in fragments.sorted(by: { $0.y == $1.y ? $0.x < $1.x : $0.y > $1.y }) {
      if let first = rows.last?.first, abs(first.y - fragment.y) <= 3.0 {
        rows[rows.count - 1].append(fragment)
      } else {
        rows.append([fragment])
      }
    }

    let originX = fragments.map { $0.x }.min() ?? 0
    var lines: [String] = []

    for row in rows {
      var line = ""

      for fragment in row.sorted(by: { $0.x < $1.x }) {
        let column = max(0, Int(((fragment.x - originX) / columnWidth).rounded()))

        if column > line.count {
          line += String(repeating: " ", count: column - line.count)
        } else if !line.isEmpty {
          // Two fragments closer than a column apart still need a separator, or
          // the last word of one runs into the first word of the next.
          line += " "
        }

        line += fragment.text
      }

      lines.append(line)
    }

    return lines.joined(separator: "\n")
  }
}
