package expo.modules.pdfextractor

import android.content.Context
import android.net.Uri
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.InputStream

/**
 * PdfExtractorModule - Native module for extracting text from PDF files
 *
 * Uses Apache PDFBox (Android port) for PDF reading and decryption.
 *
 * Error codes surfaced to JS via CodedException:
 *  - PASSWORD_REQUIRED   : PDF is encrypted and no password was supplied
 *  - INCORRECT_PASSWORD  : Supplied password does not unlock the PDF
 *  - PDF_LOAD_ERROR      : PDF could not be opened (missing / corrupt / I/O)
 *  - PDF_EXTRACTION_ERROR: Other failure during text extraction
 */
class PdfExtractorModule : Module() {

  private val context: Context
    get() = requireNotNull(appContext.reactContext) { "React context is null" }

  override fun definition() = ModuleDefinition {
    Name("PdfExtractor")

    OnCreate {
      try {
        PDFBoxResourceLoader.init(context)
        android.util.Log.d("PdfExtractor", "PDFBox initialized successfully")
      } catch (e: Exception) {
        android.util.Log.e("PdfExtractor", "Failed to initialize PDFBox: ${e.message}")
      }
    }

    AsyncFunction("extractText") { filePath: String, password: String?, promise: Promise ->
      try {
        val text = extractTextFromPdf(filePath, password)
        promise.resolve(text)
      } catch (e: InvalidPasswordException) {
        promise.reject(passwordCodedException(password, e))
      } catch (e: Exception) {
        android.util.Log.e("PdfExtractor", "Error extracting text: ${e.message}", e)
        promise.reject("PDF_EXTRACTION_ERROR", "Failed to extract text: ${e.message}", e)
      }
    }

    Function("isAvailable") {
      return@Function true
    }

    AsyncFunction("getPageCount") { filePath: String, password: String?, promise: Promise ->
      try {
        val pageCount = getPageCountFromPdf(filePath, password)
        promise.resolve(pageCount)
      } catch (e: InvalidPasswordException) {
        promise.reject(passwordCodedException(password, e))
      } catch (e: Exception) {
        promise.reject("PDF_ERROR", "Failed to get page count: ${e.message}", e)
      }
    }

    AsyncFunction("extractTextFromPage") { filePath: String, pageNumber: Int, password: String?, promise: Promise ->
      try {
        val text = extractTextFromPdfPage(filePath, pageNumber, password)
        promise.resolve(text)
      } catch (e: InvalidPasswordException) {
        promise.reject(passwordCodedException(password, e))
      } catch (e: Exception) {
        promise.reject("PDF_EXTRACTION_ERROR", "Failed to extract page: ${e.message}", e)
      }
    }

    // Detection check: true iff opening the PDF with no password fails because
    // a password is actually required. PDFs that declare encryption but unlock
    // with an empty password return false (they can be read without one).
    AsyncFunction("isPasswordProtected") { filePath: String, promise: Promise ->
      try {
        getInputStream(filePath).use { stream ->
          PDDocument.load(stream, "").close()
        }
        promise.resolve(false)
      } catch (e: InvalidPasswordException) {
        promise.resolve(true)
      } catch (e: Exception) {
        promise.reject("PDF_LOAD_ERROR", "Failed to inspect PDF: ${e.message}", e)
      }
    }
  }

  private fun passwordCodedException(password: String?, cause: Throwable): CodedException =
    if (password.isNullOrEmpty()) {
      CodedException("PASSWORD_REQUIRED", "PDF requires a password", cause)
    } else {
      CodedException("INCORRECT_PASSWORD", "Provided password is incorrect", cause)
    }

  private fun extractTextFromPdf(filePath: String, password: String?): String {
    return getInputStream(filePath).use { stream ->
      PDDocument.load(stream, password ?: "").use { document ->
        (1..document.numberOfPages).joinToString("\n") { page ->
          layoutTextForPage(document, page)
        }
      }
    }
  }

  private fun getPageCountFromPdf(filePath: String, password: String?): Int {
    return getInputStream(filePath).use { stream ->
      PDDocument.load(stream, password ?: "").use { document ->
        document.numberOfPages
      }
    }
  }

  private fun extractTextFromPdfPage(filePath: String, pageNumber: Int, password: String?): String {
    return getInputStream(filePath).use { stream ->
      PDDocument.load(stream, password ?: "").use { document ->
        if (pageNumber < 1 || pageNumber > document.numberOfPages) {
          throw IllegalArgumentException("Page $pageNumber out of range (1-${document.numberOfPages})")
        }
        layoutTextForPage(document, pageNumber)
      }
    }
  }

  /**
   * One page, with its columns intact. Falls back to the default stripper when
   * the page yields no positioned glyphs, so an image-only page still returns
   * the empty text the import flow reads as `no-text`.
   */
  private fun layoutTextForPage(document: PDDocument, pageNumber: Int): String {
    val stripper = LayoutTextStripper().apply {
      startPage = pageNumber
      endPage = pageNumber
    }
    stripper.getText(document)

    val laidOut = stripper.layoutText()
    if (laidOut.isNotBlank()) return laidOut

    return PDFTextStripper().apply {
      sortByPosition = true
      startPage = pageNumber
      endPage = pageNumber
    }.getText(document)
  }

  private fun getInputStream(filePath: String): InputStream {
    return when {
      filePath.startsWith("content://") -> {
        val uri = Uri.parse(filePath)
        context.contentResolver.openInputStream(uri)
          ?: throw IllegalArgumentException("Cannot open content URI: $filePath")
      }
      filePath.startsWith("file://") -> {
        File(filePath.removePrefix("file://")).inputStream()
      }
      else -> File(filePath).inputStream()
    }
  }
}
