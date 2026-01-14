import ExpoModulesCore
import PDFKit

/**
 * PdfExtractorModule - Native module for extracting text from PDF files on iOS
 *
 * This module uses Apple's native PDFKit framework to read PDF files and
 * extract their text content. PDFKit is built into iOS and handles:
 * - Compressed PDF streams
 * - Font encoding and character mapping
 * - Multi-page documents
 * - Various PDF versions
 *
 * HOW THIS WORKS:
 * 1. JavaScript calls: PdfExtractorModule.extractText(filePath)
 * 2. Expo bridges this call to the Swift function below
 * 3. We use PDFKit to read and extract text
 * 4. Result is returned back to JavaScript as a Promise
 */
public class PdfExtractorModule: Module {

  public func definition() -> ModuleDefinition {
    // The name that JavaScript will use to access this module
    // Must match the Android module name for cross-platform compatibility
    Name("PdfExtractor")

    // A simple function to check if the module is available
    Function("isAvailable") { () -> Bool in
      return true
    }

    // Extract all text from a PDF file
    AsyncFunction("extractText") { (filePath: String, promise: Promise) in
      self.extractText(filePath: filePath, promise: promise)
    }

    // Get the number of pages in a PDF
    AsyncFunction("getPageCount") { (filePath: String, promise: Promise) in
      self.getPageCount(filePath: filePath, promise: promise)
    }

    // Extract text from a specific page (1-indexed)
    AsyncFunction("extractTextFromPage") { (filePath: String, pageNumber: Int, promise: Promise) in
      self.extractTextFromPage(filePath: filePath, pageNumber: pageNumber, promise: promise)
    }
  }

  // MARK: - Private Methods

  /**
   * Extract all text from a PDF file
   */
  private func extractText(filePath: String, promise: Promise) {
    DispatchQueue.global(qos: .userInitiated).async {
      do {
        let url = try self.getFileURL(from: filePath)

        guard let document = PDFDocument(url: url) else {
          promise.reject("PDF_LOAD_ERROR", "Failed to load PDF document")
          return
        }

        var fullText = ""

        for pageIndex in 0..<document.pageCount {
          if let page = document.page(at: pageIndex),
             let pageText = page.string {
            fullText += pageText
            // Add newline between pages for readability
            if pageIndex < document.pageCount - 1 {
              fullText += "\n"
            }
          }
        }

        NSLog("[PdfExtractor] Extracted \(fullText.count) characters from \(document.pageCount) pages")

        promise.resolve(fullText)
      } catch {
        NSLog("[PdfExtractor] Error extracting text: \(error.localizedDescription)")
        promise.reject("PDF_EXTRACTION_ERROR", "Failed to extract text: \(error.localizedDescription)")
      }
    }
  }

  /**
   * Get the number of pages in a PDF
   */
  private func getPageCount(filePath: String, promise: Promise) {
    DispatchQueue.global(qos: .userInitiated).async {
      do {
        let url = try self.getFileURL(from: filePath)

        guard let document = PDFDocument(url: url) else {
          promise.reject("PDF_LOAD_ERROR", "Failed to load PDF document")
          return
        }

        promise.resolve(document.pageCount)
      } catch {
        promise.reject("PDF_ERROR", "Failed to get page count: \(error.localizedDescription)")
      }
    }
  }

  /**
   * Extract text from a specific page (1-indexed)
   */
  private func extractTextFromPage(filePath: String, pageNumber: Int, promise: Promise) {
    DispatchQueue.global(qos: .userInitiated).async {
      do {
        let url = try self.getFileURL(from: filePath)

        guard let document = PDFDocument(url: url) else {
          promise.reject("PDF_LOAD_ERROR", "Failed to load PDF document")
          return
        }

        // Convert 1-indexed to 0-indexed
        let pageIndex = pageNumber - 1

        guard pageIndex >= 0 && pageIndex < document.pageCount else {
          promise.reject("PDF_PAGE_ERROR", "Page \(pageNumber) out of range (1-\(document.pageCount))")
          return
        }

        guard let page = document.page(at: pageIndex) else {
          promise.reject("PDF_PAGE_ERROR", "Failed to get page \(pageNumber)")
          return
        }

        let pageText = page.string ?? ""
        promise.resolve(pageText)
      } catch {
        promise.reject("PDF_EXTRACTION_ERROR", "Failed to extract page: \(error.localizedDescription)")
      }
    }
  }

  /**
   * Convert various file path formats to a URL
   * Handles: file://, absolute paths
   */
  private func getFileURL(from filePath: String) throws -> URL {
    if filePath.hasPrefix("file://") {
      // File URI
      guard let url = URL(string: filePath) else {
        throw NSError(domain: "PdfExtractor", code: 1, userInfo: [
          NSLocalizedDescriptionKey: "Invalid file URI: \(filePath)"
        ])
      }
      return url
    } else {
      // Absolute path
      let url = URL(fileURLWithPath: filePath)

      // Verify file exists
      guard FileManager.default.fileExists(atPath: url.path) else {
        throw NSError(domain: "PdfExtractor", code: 2, userInfo: [
          NSLocalizedDescriptionKey: "File not found: \(filePath)"
        ])
      }

      return url
    }
  }
}
