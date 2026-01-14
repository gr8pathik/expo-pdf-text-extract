package expo.modules.pdfextractor

import android.content.Context
import android.net.Uri
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.InputStream

/**
 * PdfExtractorModule - Native module for extracting text from PDF files
 *
 * This module uses Apache PDFBox (Android version) to read PDF files and
 * extract their text content. PDFBox is a well-maintained library that handles:
 * - Compressed PDF streams (FlateDecode, etc.)
 * - Font encoding and character mapping
 * - Multi-page documents
 * - Various PDF versions
 *
 * HOW THIS WORKS:
 * 1. JavaScript calls: PdfExtractorModule.extractText(filePath)
 * 2. Expo bridges this call to the Kotlin function below
 * 3. We use PDFBox to read and extract text
 * 4. Result is returned back to JavaScript as a Promise
 */
class PdfExtractorModule : Module() {

  // Get the Android context (needed for file access and PDFBox initialization)
  private val context: Context
    get() = requireNotNull(appContext.reactContext) { "React context is null" }

  /**
   * ModuleDefinition - This is where we define what functions/properties
   * are exposed to JavaScript. Think of it as the "API" of your native module.
   */
  override fun definition() = ModuleDefinition {
    // The name that JavaScript will use to access this module
    // Usage in JS: import { extractText } from '@myvault/pdf-extractor'
    Name("PdfExtractor")

    // Initialize PDFBox when the module loads
    // PDFBox needs to load some resources for font handling
    OnCreate {
      try {
        PDFBoxResourceLoader.init(context)
        android.util.Log.d("PdfExtractor", "PDFBox initialized successfully")
      } catch (e: Exception) {
        android.util.Log.e("PdfExtractor", "Failed to initialize PDFBox: ${e.message}")
      }
    }

    // Define an async function that JavaScript can call
    // AsyncFunction means it returns a Promise in JavaScript
    AsyncFunction("extractText") { filePath: String, promise: Promise ->
      try {
        android.util.Log.d("PdfExtractor", "extractText called with path: $filePath")

        val text = extractTextFromPdf(filePath)

        android.util.Log.d("PdfExtractor", "Extracted ${text.length} characters")
        android.util.Log.d("PdfExtractor", "First 500 chars: ${text.take(500)}")

        promise.resolve(text)
      } catch (e: Exception) {
        android.util.Log.e("PdfExtractor", "Error extracting text: ${e.message}", e)
        promise.reject("PDF_EXTRACTION_ERROR", "Failed to extract text: ${e.message}", e)
      }
    }

    // A simple function to check if the module is available
    // Useful for the Expo Go fallback check
    Function("isAvailable") {
      return@Function true
    }

    // Get basic info about a PDF (page count, etc.)
    AsyncFunction("getPageCount") { filePath: String, promise: Promise ->
      try {
        val pageCount = getPageCountFromPdf(filePath)
        promise.resolve(pageCount)
      } catch (e: Exception) {
        promise.reject("PDF_ERROR", "Failed to get page count: ${e.message}", e)
      }
    }

    // Extract text from a specific page (1-indexed)
    AsyncFunction("extractTextFromPage") { filePath: String, pageNumber: Int, promise: Promise ->
      try {
        val text = extractTextFromPdfPage(filePath, pageNumber)
        promise.resolve(text)
      } catch (e: Exception) {
        promise.reject("PDF_EXTRACTION_ERROR", "Failed to extract page: ${e.message}", e)
      }
    }
  }

  /**
   * Extract all text from a PDF file
   *
   * @param filePath - Can be:
   *   - file:///path/to/file.pdf (file URI)
   *   - /path/to/file.pdf (absolute path)
   *   - content://... (content URI from document picker)
   */
  private fun extractTextFromPdf(filePath: String): String {
    android.util.Log.d("PdfExtractor", "Opening PDF: $filePath")

    val inputStream = getInputStream(filePath)

    return inputStream.use { stream ->
      PDDocument.load(stream).use { document ->
        android.util.Log.d("PdfExtractor", "PDF loaded, pages: ${document.numberOfPages}")

        val stripper = PDFTextStripper().apply {
          // Configure text extraction
          sortByPosition = true  // Sort text by position on page
          addMoreFormatting = false  // Don't add extra formatting
        }

        val text = stripper.getText(document)
        android.util.Log.d("PdfExtractor", "Text extraction complete")

        text
      }
    }
  }

  /**
   * Get the number of pages in a PDF
   */
  private fun getPageCountFromPdf(filePath: String): Int {
    val inputStream = getInputStream(filePath)

    return inputStream.use { stream ->
      PDDocument.load(stream).use { document ->
        document.numberOfPages
      }
    }
  }

  /**
   * Extract text from a specific page (1-indexed)
   */
  private fun extractTextFromPdfPage(filePath: String, pageNumber: Int): String {
    val inputStream = getInputStream(filePath)

    return inputStream.use { stream ->
      PDDocument.load(stream).use { document ->
        if (pageNumber < 1 || pageNumber > document.numberOfPages) {
          throw IllegalArgumentException("Page $pageNumber out of range (1-${document.numberOfPages})")
        }

        val stripper = PDFTextStripper().apply {
          sortByPosition = true
          startPage = pageNumber
          endPage = pageNumber
        }

        stripper.getText(document)
      }
    }
  }

  /**
   * Get an InputStream from various path formats
   * Handles: file://, content://, and absolute paths
   */
  private fun getInputStream(filePath: String): InputStream {
    return when {
      // Content URI (from document picker)
      filePath.startsWith("content://") -> {
        android.util.Log.d("PdfExtractor", "Opening content URI")
        val uri = Uri.parse(filePath)
        context.contentResolver.openInputStream(uri)
          ?: throw IllegalArgumentException("Cannot open content URI: $filePath")
      }

      // File URI
      filePath.startsWith("file://") -> {
        android.util.Log.d("PdfExtractor", "Opening file URI")
        val path = filePath.removePrefix("file://")
        File(path).inputStream()
      }

      // Absolute path
      else -> {
        android.util.Log.d("PdfExtractor", "Opening absolute path")
        File(filePath).inputStream()
      }
    }
  }
}
