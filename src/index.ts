/**
 * expo-pdf-text-extract
 *
 * Native PDF text extraction for React Native / Expo
 *
 * Uses native platform APIs for reliable text extraction:
 * - Android: Apache PDFBox
 * - iOS: Apple PDFKit
 *
 * @packageDocumentation
 * @module expo-pdf-text-extract
 *
 * @example
 * ```typescript
 * import { extractText, isAvailable } from 'expo-pdf-text-extract';
 *
 * // Check if native module is available (false in Expo Go)
 * if (isAvailable()) {
 *   const text = await extractText('/path/to/document.pdf');
 *   console.log(text);
 * }
 * ```
 */

import { NativeModulesProxy, requireNativeModule } from 'expo-modules-core';

// Define the shape of our native module
interface PdfExtractorModule {
  /**
   * Extract all text from a PDF file
   * @param filePath - Path to the PDF file (file://, content://, or absolute path)
   * @returns Promise resolving to extracted text
   */
  extractText(filePath: string): Promise<string>;

  /**
   * Get the number of pages in a PDF
   * @param filePath - Path to the PDF file
   * @returns Promise resolving to page count
   */
  getPageCount(filePath: string): Promise<number>;

  /**
   * Extract text from a specific page (1-indexed)
   * @param filePath - Path to the PDF file
   * @param pageNumber - Page number (starting from 1)
   * @returns Promise resolving to text from that page
   */
  extractTextFromPage(filePath: string, pageNumber: number): Promise<string>;

  /**
   * Check if the native module is available
   * @returns true if native module is loaded
   */
  isAvailable(): boolean;
}

// Try to load the native module
// This will be null in Expo Go since native modules aren't available there
let PdfExtractor: PdfExtractorModule | null = null;

try {
  PdfExtractor = requireNativeModule<PdfExtractorModule>('PdfExtractor');
} catch (error) {
  // Native module not available (e.g., running in Expo Go)
  console.log('[PdfExtractor] Native module not available. PDF extraction disabled.');
  PdfExtractor = null;
}

/**
 * Check if the native PDF extractor is available
 *
 * Returns false when running in Expo Go or if the native module failed to load.
 * Use this to conditionally enable/disable PDF extraction features.
 *
 * @example
 * ```typescript
 * if (isAvailable()) {
 *   // Show "Upload PDF" button
 * } else {
 *   // Show "Manual entry only" or info message
 * }
 * ```
 */
export function isAvailable(): boolean {
  if (!PdfExtractor) {
    return false;
  }

  try {
    return PdfExtractor.isAvailable();
  } catch {
    return false;
  }
}

/**
 * Extract all text from a PDF file
 *
 * @param filePath - Path to the PDF file. Supports:
 *   - `file:///path/to/file.pdf` - File URI
 *   - `/absolute/path/to/file.pdf` - Absolute path
 *   - `content://...` - Content URI (Android document picker)
 *
 * @returns Promise resolving to the extracted text
 * @throws Error if native module not available or extraction fails
 *
 * @example
 * ```typescript
 * const text = await extractText(documentPickerResult.uri);
 * ```
 */
export async function extractText(filePath: string): Promise<string> {
  if (!PdfExtractor) {
    throw new Error(
      'PDF extraction is not available. ' +
        'This feature requires a development build. ' +
        'Run `npx expo run:android` or `npx expo run:ios` to create one.'
    );
  }

  if (!filePath) {
    throw new Error('File path is required');
  }

  return PdfExtractor.extractText(filePath);
}

/**
 * Get the number of pages in a PDF
 *
 * @param filePath - Path to the PDF file
 * @returns Promise resolving to page count
 * @throws Error if native module not available or file cannot be read
 */
export async function getPageCount(filePath: string): Promise<number> {
  if (!PdfExtractor) {
    throw new Error(
      'PDF extraction is not available. ' +
        'This feature requires a development build.'
    );
  }

  if (!filePath) {
    throw new Error('File path is required');
  }

  return PdfExtractor.getPageCount(filePath);
}

/**
 * Extract text from a specific page
 *
 * @param filePath - Path to the PDF file
 * @param pageNumber - Page number (1-indexed, first page is 1)
 * @returns Promise resolving to text from that page
 * @throws Error if page number is invalid or extraction fails
 */
export async function extractTextFromPage(
  filePath: string,
  pageNumber: number
): Promise<string> {
  if (!PdfExtractor) {
    throw new Error(
      'PDF extraction is not available. ' +
        'This feature requires a development build.'
    );
  }

  if (!filePath) {
    throw new Error('File path is required');
  }

  if (pageNumber < 1) {
    throw new Error('Page number must be at least 1');
  }

  return PdfExtractor.extractTextFromPage(filePath, pageNumber);
}

/**
 * Extract text with detailed result
 *
 * Returns additional metadata about the extraction including
 * page count and success status.
 */
export async function extractTextWithInfo(filePath: string): Promise<{
  text: string;
  pageCount: number;
  success: boolean;
  error?: string;
}> {
  if (!PdfExtractor) {
    return {
      text: '',
      pageCount: 0,
      success: false,
      error: 'Native module not available',
    };
  }

  try {
    const [text, pageCount] = await Promise.all([
      PdfExtractor.extractText(filePath),
      PdfExtractor.getPageCount(filePath),
    ]);

    return {
      text,
      pageCount,
      success: true,
    };
  } catch (error) {
    return {
      text: '',
      pageCount: 0,
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}

// Export types for consumers
export type { PdfExtractorModule };
