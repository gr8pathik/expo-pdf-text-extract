# expo-pdf-text-extract

Native PDF text extraction for React Native and Expo. Extract text content from PDF files using platform-native APIs - no OCR needed for digital PDFs.

[![npm version](https://img.shields.io/npm/v/expo-pdf-text-extract.svg)](https://www.npmjs.com/package/expo-pdf-text-extract)
[![license](https://img.shields.io/npm/l/expo-pdf-text-extract.svg)](https://github.com/gr8pathik/expo-pdf-text-extract/blob/main/LICENSE)
[![platforms](https://img.shields.io/badge/platforms-iOS%20%7C%20Android-lightgrey.svg)](https://reactnative.dev/)

## Features

- **Native Performance** - Uses PDFKit (iOS) and PDFBox (Android) for fast, reliable extraction
- **No OCR Required** - Extracts embedded text directly from digital PDFs
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

### `extractText(filePath)`

Extract all text from a PDF file.

```typescript
function extractText(filePath: string): Promise<string>
```

**Parameters:**
- `filePath` - Path to the PDF file. Supports:
  - `file:///path/to/file.pdf` - File URI
  - `/absolute/path/to/file.pdf` - Absolute path
  - `content://...` - Content URI (Android document picker)

**Returns:** Promise resolving to extracted text

**Throws:**
- Error if native module not available
- Error if file not found
- Error if PDF is invalid or corrupted

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

### `getPageCount(filePath)`

Get the number of pages in a PDF.

```typescript
function getPageCount(filePath: string): Promise<number>
```

**Example:**
```typescript
import { getPageCount } from 'expo-pdf-text-extract';

const pages = await getPageCount('/path/to/document.pdf');
console.log(`PDF has ${pages} pages`);
```

### `extractTextFromPage(filePath, pageNumber)`

Extract text from a specific page.

```typescript
function extractTextFromPage(filePath: string, pageNumber: number): Promise<string>
```

**Parameters:**
- `filePath` - Path to the PDF file
- `pageNumber` - Page number (1-indexed, first page is 1)

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

### `extractTextWithInfo(filePath)`

Extract text with additional metadata.

```typescript
function extractTextWithInfo(filePath: string): Promise<{
  text: string;
  pageCount: number;
  success: boolean;
  error?: string;
}>
```

**Example:**
```typescript
import { extractTextWithInfo } from 'expo-pdf-text-extract';

const result = await extractTextWithInfo('/path/to/document.pdf');

if (result.success) {
  console.log(`Extracted ${result.text.length} characters from ${result.pageCount} pages`);
} else {
  console.error('Extraction failed:', result.error);
}
```

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

## Platform Differences

### iOS (PDFKit)
- Uses Apple's native PDFKit framework
- Built into iOS, no additional dependencies
- Excellent support for standard PDF formats
- Minimum iOS version: 15.1

### Android (PDFBox)
- Uses Apache PDFBox (Android port)
- Text is sorted by position on page for better readability
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
2. **Protected PDF** - The PDF has copy protection. Text extraction may be blocked.
3. **Corrupted PDF** - Try opening the PDF in another app to verify it's valid.

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
