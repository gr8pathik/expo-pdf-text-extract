# Changelog

All notable changes to this project will be documented in this file.

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
