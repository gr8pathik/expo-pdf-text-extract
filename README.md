# expo-pdf-text-extract

Native PDF text extraction for React Native and Expo. Extract text content from PDF files using platform-native APIs - no OCR needed for digital PDFs.

[![npm version](https://img.shields.io/npm/v/expo-pdf-text-extract.svg)](https://www.npmjs.com/package/expo-pdf-text-extract)
[![license](https://img.shields.io/npm/l/expo-pdf-text-extract.svg)](https://github.com/gr8pathik/expo-pdf-text-extract/blob/main/LICENSE)
[![platforms](https://img.shields.io/badge/platforms-iOS%20%7C%20Android-lightgrey.svg)](https://reactnative.dev/)

## Features

- **Native Performance** - Uses PDFKit (iOS) and PDFBox (Android) for fast, reliable extraction
- **No OCR Required** - Extracts embedded text directly from digital PDFs
- **Password-Protected PDFs** - First-class support for encrypted PDFs on both platforms
- **Expo Compatible** - Works with Expo development builds (SDK 49+)
- **TypeScript Support** - Full type definitions included
- **Simple API** - Just one function to extract text
- **Page-level Control** - Extract from specific pages or get page count
- **Multiple Path Formats** - Supports `file://`, `content://`, and absolute paths

## When to Use This

| Scenario | This Package | Alternative |
|----------|-------------|-------------|
| Digital PDFs (from email, downloads) | Yes | - |
| Scanned PDFs (images of paper) | No | Use OCR library |
| Need text content only | Yes | - |
| Need to render/view PDF | No | Use react-native-pdf |
| Expo Go | No | Requires dev build |

## Requirements

- **Expo SDK**: 49.0.0 or higher
- **React Native**: 0.72.0 or higher
- **iOS**: 15.1 or higher
- **Android**: API 21 (Lollipop) or higher

> **Important**: This package requires an Expo development build. It will not work in Expo Go.

## Installation

### Using Expo

```bash
npx expo install expo-pdf-text-extract
```

### Using npm/yarn

```bash
npm install expo-pdf-text-extract
# or
yarn add expo-pdf-text-extract
```

### Create Development Build

Since this is a native module, you need to create a development build:

```bash
# For iOS
npx expo run:ios

# For Android
npx expo run:android

# Or create a development build
eas build --profile development --platform all
```

## Quick Start

```typescript
import { extractText, isAvailable } from 'expo-pdf-text-extract';

// Check if native module is available
if (isAvailable()) {
  // Extract text from a PDF file
  const text = await extractText('/path/to/document.pdf');
  console.log(text);
}
```

## API Reference

### `isAvailable()`

Check if the native PDF extractor is available.

```typescript
function isAvailable(): boolean
```

Returns `false` when:
- Running in Expo Go
- Native module failed to load
- Platform not supported

**Example:**
```typescript
import { isAvailable } from 'expo-pdf-text-extract';

if (isAvailable()) {
  // Show PDF upload option
} else {
  // Show message: "PDF extraction requires a development build"
}
```

### `extractText(filePath, password?)`

Extract all text from a PDF file.

```typescript
function extractText(filePath: string, password?: string): Promise<string>
```

**Parameters:**
- `filePath` - Path to the PDF file. Supports:
  - `file:///path/to/file.pdf` - File URI
  - `/absolute/path/to/file.pdf` - Absolute path
  - `content://...` - Content URI (Android document picker)
- `password` *(optional)* - Password for encrypted PDFs. Omit for clear PDFs.

**Returns:** Promise resolving to extracted text

**Throws:** an `Error` with a stable `.code`:
- `'PASSWORD_REQUIRED'` - PDF is encrypted and no password was supplied
- `'INCORRECT_PASSWORD'` - The supplied password does not unlock the PDF
- generic error if file not found or PDF is invalid / corrupted
- error if native module not available (Expo Go)

**Example:**
```typescript
import { extractText } from 'expo-pdf-text-extract';
import * as DocumentPicker from 'expo-document-picker';

// Pick a PDF file
const result = await DocumentPicker.getDocumentAsync({
  type: 'application/pdf',
});

if (!result.canceled) {
  const text = await extractText(result.assets[0].uri);
  console.log('Extracted text:', text);
}
```

### `getPageCount(filePath, password?)`

Get the number of pages in a PDF.

```typescript
function getPageCount(filePath: string, password?: string): Promise<number>
```

Throws `PASSWORD_REQUIRED` / `INCORRECT_PASSWORD` for encrypted PDFs without a
valid password (same error semantics as `extractText`).

**Example:**
```typescript
import { getPageCount } from 'expo-pdf-text-extract';

const pages = await getPageCount('/path/to/document.pdf');
console.log(`PDF has ${pages} pages`);
```

### `extractTextFromPage(filePath, pageNumber, password?)`

Extract text from a specific page.

```typescript
function extractTextFromPage(
  filePath: string,
  pageNumber: number,
  password?: string,
): Promise<string>
```

**Parameters:**
- `filePath` - Path to the PDF file
- `pageNumber` - Page number (1-indexed, first page is 1)
- `password` *(optional)* - Password for encrypted PDFs

**Example:**
```typescript
import { extractTextFromPage, getPageCount } from 'expo-pdf-text-extract';

// Extract text from first page only
const firstPageText = await extractTextFromPage('/path/to/document.pdf', 1);

// Extract text from each page separately
const pageCount = await getPageCount('/path/to/document.pdf');
for (let i = 1; i <= pageCount; i++) {
  const pageText = await extractTextFromPage('/path/to/document.pdf', i);
  console.log(`Page ${i}:`, pageText);
}
```

### `extractTextWithInfo(filePath, password?)`

Extract text with additional metadata. This is the **non-throwing variant** —
password failures and other errors are returned as data rather than thrown.

```typescript
function extractTextWithInfo(
  filePath: string,
  password?: string,
): Promise<{
  text: string;
  pageCount: number;
  success: boolean;
  isEncrypted: boolean;          // true if the PDF declared encryption
  passwordRequired?: boolean;    // true if the call failed because of password
  error?: string;
  errorCode?:
    | 'PASSWORD_REQUIRED'
    | 'INCORRECT_PASSWORD'
    | 'FILE_NOT_FOUND'
    | 'CORRUPT_PDF'
    | 'UNKNOWN';
}>
```

**Example:**
```typescript
import { extractTextWithInfo } from 'expo-pdf-text-extract';

const result = await extractTextWithInfo('/path/to/document.pdf');

if (result.success) {
  console.log(`Extracted ${result.text.length} chars from ${result.pageCount} pages`);
} else if (result.passwordRequired) {
  // prompt user for a password and retry with extractTextWithInfo(uri, pwd)
} else {
  console.error('Extraction failed:', result.error, result.errorCode);
}
```

### `isPasswordProtected(filePath)`

Detect whether a PDF actually requires a password to read.

```typescript
function isPasswordProtected(filePath: string): Promise<boolean>
```

Returns `true` only if the PDF cannot be opened without a password. PDFs that
declare encryption but unlock with an empty password return `false` — they can
be read by `extractText` without supplying a password.

## Password-Protected PDFs

`extractText`, `getPageCount`, and `extractTextFromPage` accept an optional
`password` parameter. If the PDF is encrypted and no password (or the wrong
password) is provided, the call throws an `Error` with a stable `.code`:

| `.code`              | Meaning                                            |
| -------------------- | -------------------------------------------------- |
| `PASSWORD_REQUIRED`  | PDF is encrypted and no password was supplied      |
| `INCORRECT_PASSWORD` | Supplied password does not unlock the PDF          |

Use `isPasswordProtected(filePath)` for a fast detection check before
prompting the user for a password.

### Example

```typescript
import {
  isPasswordProtected,
  extractText,
} from 'expo-pdf-text-extract';

async function readPdf(uri: string) {
  if (await isPasswordProtected(uri)) {
    const password = await promptUserForPassword();
    try {
      return await extractText(uri, password);
    } catch (e: any) {
      if (e.code === 'INCORRECT_PASSWORD') {
        // ask the user again
        return readPdf(uri);
      }
      throw e;
    }
  }
  return extractText(uri);
}
```

If you prefer error-as-data over try/catch, use `extractTextWithInfo` — it
never throws on password issues and returns `passwordRequired: true` plus an
`errorCode` instead.

## Usage with Document Picker

```typescript
import { extractText, isAvailable } from 'expo-pdf-text-extract';
import * as DocumentPicker from 'expo-document-picker';

async function handlePdfUpload() {
  // Check if extraction is available
  if (!isAvailable()) {
    Alert.alert(
      'Not Available',
      'PDF extraction requires a development build. Please rebuild the app.'
    );
    return;
  }

  // Pick PDF file
  const result = await DocumentPicker.getDocumentAsync({
    type: 'application/pdf',
    copyToCacheDirectory: true,
  });

  if (result.canceled) {
    return;
  }

  try {
    // Extract text
    const text = await extractText(result.assets[0].uri);

    // Use the extracted text
    console.log('Extracted text:', text.substring(0, 500));

    // Parse the text, search for patterns, etc.
    const hasKeyword = text.includes('invoice');

  } catch (error) {
    Alert.alert('Error', `Failed to extract text: ${error.message}`);
  }
}
```

## Error Handling

```typescript
import { extractText, isAvailable } from 'expo-pdf-text-extract';

async function safeExtract(filePath: string): Promise<string | null> {
  // Check availability first
  if (!isAvailable()) {
    console.warn('PDF extraction not available');
    return null;
  }

  try {
    return await extractText(filePath);
  } catch (error) {
    if (error.message.includes('not found')) {
      console.error('File not found:', filePath);
    } else if (error.message.includes('PDF_LOAD_ERROR')) {
      console.error('Invalid or corrupted PDF');
    } else {
      console.error('Extraction failed:', error.message);
    }
    return null;
  }
}
```

## Layout Preservation

Extracted text keeps the page's column layout. Tabular documents — bank
statements, invoices, reports — print continuation lines indented under the row
they belong to, and that indentation is the only thing separating them from a
real row:

```
  01/02/26   03/02/26   Widget Supply Co        12.34
                        continued detail
```

Flattened to reading order the second line reads as a row of its own, which is
how a downstream parser ends up inventing entries. Both platforms reconstruct
spacing from glyph geometry, so the distinction survives.

Pages with no text layer (scans) still return an empty string, so the usual
"is this PDF image-based?" check is unchanged.

## Platform Differences

### iOS (PDFKit)
- Uses Apple's native PDFKit framework
- Built into iOS, no additional dependencies
- Excellent support for standard PDF formats
- Column layout is reconstructed from line geometry (see Layout Preservation)
- Minimum iOS version: 15.1

### Android (PDFBox)
- Uses Apache PDFBox (Android port)
- Text is sorted by position on page, and column layout is reconstructed
  from glyph offsets (see Layout Preservation)
- Handles compressed PDF streams (FlateDecode, etc.)
- Minimum API level: 21

## Troubleshooting

### "PDF extraction is not available"

This error occurs when running in Expo Go. Solution:

```bash
# Create a development build
npx expo run:ios
# or
npx expo run:android
```

### Empty text returned

If `extractText()` returns empty string:
1. **Scanned PDF** - The PDF contains images, not text. Use OCR instead.
2. **Corrupted PDF** - Try opening the PDF in another app to verify it's valid.

> Password-protected PDFs no longer return empty text — they throw an `Error`
> with `.code === 'PASSWORD_REQUIRED'`. See [Password-Protected PDFs](#password-protected-pdfs).

### Slow extraction on large PDFs

For PDFs with many pages, consider:
1. Extract page by page using `extractTextFromPage()`
2. Show progress indicator to users
3. Process in background using a worker

## Performance

| PDF Size | Pages | Extraction Time (approx) |
|----------|-------|--------------------------|
| Small    | 1-5   | < 100ms |
| Medium   | 10-50 | 100-500ms |
| Large    | 100+  | 500ms-2s |

*Times measured on iPhone 13 and Pixel 6*

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

MIT License - see [LICENSE](LICENSE) for details.

## Credits

- iOS implementation uses Apple's [PDFKit](https://developer.apple.com/documentation/pdfkit)
- Android implementation uses [PDFBox-Android](https://github.com/TomRoush/PdfBox-Android) by Tom Roush

## Related Packages

- [expo-document-picker](https://docs.expo.dev/versions/latest/sdk/document-picker/) - Pick documents from device
- [react-native-pdf](https://github.com/wonday/react-native-pdf) - Display PDFs (viewing, not extraction)
- [pdf-lib](https://pdf-lib.js.org/) - Create and modify PDFs in JavaScript
