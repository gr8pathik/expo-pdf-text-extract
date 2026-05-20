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

import { requireNativeModule } from 'expo-modules-core';

/**
 * Stable error codes thrown by the native module for password-protected PDFs
 * and other failure modes. Surfaced on thrown errors as `.code`.
 */
export type PdfErrorCode =
  | 'PASSWORD_REQUIRED'
  | 'INCORRECT_PASSWORD'
  | 'FILE_NOT_FOUND'
  | 'CORRUPT_PDF'
  | 'UNKNOWN';

/**
 * Rich result returned by {@link extractTextWithInfo}.
 */
export interface ExtractTextWithInfoResult {
  text: string;
  pageCount: number;
  success: boolean;
  /** True if the source PDF declared encryption. */
  isEncrypted: boolean;
  /** True if extraction failed because no password (or the wrong one) was supplied. */
  passwordRequired?: boolean;
  error?: string;
  errorCode?: PdfErrorCode;
}

// Define the shape of our native module
interface PdfExtractorModule {
  extractText(filePath: string, password?: string | null): Promise<string>;
  getPageCount(filePath: string, password?: string | null): Promise<number>;
  extractTextFromPage(
    filePath: string,
    pageNumber: number,
    password?: string | null
  ): Promise<string>;
  isPasswordProtected(filePath: string): Promise<boolean>;
  isAvailable(): boolean;
}

// Try to load the native module
// This will be null in Expo Go since native modules aren't available there
let PdfExtractor: PdfExtractorModule | null = null;

try {
  PdfExtractor = requireNativeModule<PdfExtractorModule>('PdfExtractor');
} catch {
  // Native module not available (e.g., running in Expo Go)
  console.log('[PdfExtractor] Native module not available. PDF extraction disabled.');
  PdfExtractor = null;
}

function requireModule(): PdfExtractorModule {
  if (!PdfExtractor) {
    throw new Error(
      'PDF extraction is not available. ' +
        'This feature requires a development build. ' +
        'Run `npx expo run:android` or `npx expo run:ios` to create one.'
    );
  }
  return PdfExtractor;
}

/**
 * Check if the native PDF extractor is available
 *
 * Returns false when running in Expo Go or if the native module failed to load.
 * Use this to conditionally enable/disable PDF extraction features.
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
 * @param filePath - Path to the PDF file. Supports `file://`, absolute paths,
 *   and (on Android) `content://` URIs.
 * @param password - Optional password for encrypted PDFs. Use
 *   {@link isPasswordProtected} to detect whether a password is needed.
 *
 * @throws Error with `.code === 'PASSWORD_REQUIRED'` if the PDF is encrypted
 *   and no password was provided.
 * @throws Error with `.code === 'INCORRECT_PASSWORD'` if the supplied password
 *   does not unlock the PDF.
 */
export async function extractText(
  filePath: string,
  password?: string
): Promise<string> {
  const mod = requireModule();
  if (!filePath) {
    throw new Error('File path is required');
  }
  return mod.extractText(filePath, password ?? null);
}

/**
 * Get the number of pages in a PDF
 *
 * @param filePath - Path to the PDF file
 * @param password - Optional password for encrypted PDFs.
 * @throws Error with `.code === 'PASSWORD_REQUIRED' | 'INCORRECT_PASSWORD'` on
 *   password failures (see {@link extractText}).
 */
export async function getPageCount(
  filePath: string,
  password?: string
): Promise<number> {
  const mod = requireModule();
  if (!filePath) {
    throw new Error('File path is required');
  }
  return mod.getPageCount(filePath, password ?? null);
}

/**
 * Extract text from a specific page
 *
 * @param filePath - Path to the PDF file
 * @param pageNumber - Page number (1-indexed, first page is 1)
 * @param password - Optional password for encrypted PDFs.
 */
export async function extractTextFromPage(
  filePath: string,
  pageNumber: number,
  password?: string
): Promise<string> {
  const mod = requireModule();
  if (!filePath) {
    throw new Error('File path is required');
  }
  if (pageNumber < 1) {
    throw new Error('Page number must be at least 1');
  }
  return mod.extractTextFromPage(filePath, pageNumber, password ?? null);
}

/**
 * Detect whether a PDF requires a password to read.
 *
 * Returns `true` only if the PDF is encrypted AND a password is actually
 * required (i.e., it cannot be opened with an empty password). PDFs that
 * declare encryption but unlock with no password return `false` here — they
 * can be read by {@link extractText} without supplying a password.
 *
 * @example
 * ```typescript
 * if (await isPasswordProtected(uri)) {
 *   const pwd = await promptUser();
 *   const text = await extractText(uri, pwd);
 * } else {
 *   const text = await extractText(uri);
 * }
 * ```
 */
export async function isPasswordProtected(filePath: string): Promise<boolean> {
  const mod = requireModule();
  if (!filePath) {
    throw new Error('File path is required');
  }
  return mod.isPasswordProtected(filePath);
}

/**
 * Extract text with detailed result
 *
 * Non-throwing variant: password issues are surfaced as `passwordRequired: true`
 * with a populated `errorCode`, rather than thrown.
 */
export async function extractTextWithInfo(
  filePath: string,
  password?: string
): Promise<ExtractTextWithInfoResult> {
  if (!PdfExtractor) {
    return {
      text: '',
      pageCount: 0,
      success: false,
      isEncrypted: false,
      error: 'Native module not available',
      errorCode: 'UNKNOWN',
    };
  }

  let isEncrypted = false;
  try {
    isEncrypted = await PdfExtractor.isPasswordProtected(filePath);
  } catch {
    // Detection failing is not fatal — fall through to extraction, which will
    // surface the real underlying error (file not found, corrupt, etc.).
  }

  try {
    const [text, pageCount] = await Promise.all([
      PdfExtractor.extractText(filePath, password ?? null),
      PdfExtractor.getPageCount(filePath, password ?? null),
    ]);
    return {
      text,
      pageCount,
      success: true,
      isEncrypted,
    };
  } catch (error) {
    const code = errorCodeOf(error);
    const passwordRequired =
      code === 'PASSWORD_REQUIRED' || code === 'INCORRECT_PASSWORD';
    return {
      text: '',
      pageCount: 0,
      success: false,
      isEncrypted: isEncrypted || passwordRequired,
      passwordRequired: passwordRequired || undefined,
      error: error instanceof Error ? error.message : 'Unknown error',
      errorCode: code,
    };
  }
}

function errorCodeOf(error: unknown): PdfErrorCode {
  const raw =
    error && typeof error === 'object' && 'code' in error
      ? (error as { code?: unknown }).code
      : undefined;
  if (typeof raw === 'string') {
    switch (raw) {
      case 'PASSWORD_REQUIRED':
      case 'INCORRECT_PASSWORD':
      case 'FILE_NOT_FOUND':
      case 'CORRUPT_PDF':
        return raw;
    }
  }
  return 'UNKNOWN';
}

// Export types for consumers
export type { PdfExtractorModule };
