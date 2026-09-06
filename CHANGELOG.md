# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Fixed
- Extracted text now preserves the page's **column layout**. Both platforms
  previously returned reading order with horizontal position discarded
  (`PDFPage.string` on iOS, a default `PDFTextStripper` on Android), so an
  indented continuation line came back flush left and was indistinguishable
  from a real table row. Tabular documents — bank statements, invoices,
  reports — now keep the indentation that tells the two apart.

### Changed
- iOS layout logic lives in a new `PdfLayout` helper; Android's in a new
  `LayoutTextStripper`. Both are exercised directly by the tests.
- Pages that yield no positioned glyphs fall back to the previous behaviour,
  so image-only PDFs still return empty text.

### Compatibility
- No API change. `extractText`, `extractTextFromPage` and `extractTextWithInfo`
  keep their signatures; only the whitespace within the returned string differs.
  Callers that match on substrings are unaffected; callers that compare whole
  strings byte-for-byte will see added spacing.

## [1.1.0]

### Added
- `isPasswordProtected(filePath)` — fast detection check for whether a PDF
  requires a password to read.
- Optional `password` parameter on `extractText`, `getPageCount`,
  `extractTextFromPage`, and `extractTextWithInfo` to read encrypted PDFs.
- `extractTextWithInfo` now returns `isEncrypted`, `passwordRequired`, and
  `errorCode` fields alongside the existing result shape.
- Stable error codes thrown for password failures: `PASSWORD_REQUIRED` and
  `INCORRECT_PASSWORD` (on regular `Error` instances via `.code`).
- Native tests covering encrypted PDFs on both Android (PDFBox
  `StandardProtectionPolicy`) and iOS (`PDFDocument.write(to:withOptions:)`).

### Changed
- README documents the new password support and error codes.

### Compatibility
- Fully backward compatible: every existing call without the new `password`
  argument continues to work unchanged.

## [1.0.1]
- Fix iOS build failure by excluding test files from podspec sources.

## [1.0.0]
- Initial release.
