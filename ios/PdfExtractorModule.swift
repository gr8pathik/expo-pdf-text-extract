import ExpoModulesCore
import PDFKit

/**
 * PdfExtractorModule - Native module for extracting text from PDF files on iOS
 *
 * Uses Apple's native PDFKit framework.
 *
 * Error codes surfaced to JS (rejected promise `.code`):
 *  - PASSWORD_REQUIRED   : PDF is encrypted and no password was supplied
 *  - INCORRECT_PASSWORD  : Supplied password does not unlock the PDF
 *  - PDF_LOAD_ERROR      : PDF could not be opened
 *  - FILE_NOT_FOUND      : File does not exist at the given path
 *  - PDF_EXTRACTION_ERROR: Other failure during text extraction
 */
public class PdfExtractorModule: Module {

  public func definition() -> ModuleDefinition {
    Name("PdfExtractor")

    Function("isAvailable") { () -> Bool in
      return true
    }

    AsyncFunction("extractText") { (filePath: String, password: String?, promise: Promise) in
      self.extractText(filePath: filePath, password: password, promise: promise)
    }

    AsyncFunction("getPageCount") { (filePath: String, password: String?, promise: Promise) in
      self.getPageCount(filePath: filePath, password: password, promise: promise)
    }

    AsyncFunction("extractTextFromPage") { (filePath: String, pageNumber: Int, password: String?, promise: Promise) in
      self.extractTextFromPage(filePath: filePath, pageNumber: pageNumber, password: password, promise: promise)
    }

    AsyncFunction("isPasswordProtected") { (filePath: String, promise: Promise) in
      self.isPasswordProtected(filePath: filePath, promise: promise)
    }
  }

  // MARK: - Private Methods

  /// Loads a PDFDocument and unlocks it if needed.
  /// Returns the unlocked document on success, or rejects the promise with the
  /// appropriate error code.
  private func openAndUnlock(filePath: String, password: String?, promise: Promise) -> PDFDocument? {
    let url: URL
    do {
      url = try getFileURL(from: filePath)
    } catch let error as NSError {
      let code = error.domain == "PdfExtractor" && error.code == 2 ? "FILE_NOT_FOUND" : "PDF_LOAD_ERROR"
      promise.reject(code, error.localizedDescription)
      return nil
    }

    guard let document = PDFDocument(url: url) else {
      promise.reject("PDF_LOAD_ERROR", "Failed to load PDF document")
      return nil
    }

    if document.isLocked {
      guard let pwd = password, !pwd.isEmpty else {
        promise.reject("PASSWORD_REQUIRED", "PDF requires a password")
        return nil
      }
      if !document.unlock(withPassword: pwd) {
        promise.reject("INCORRECT_PASSWORD", "Provided password is incorrect")
        return nil
      }
    }

    return document
  }

  private func extractText(filePath: String, password: String?, promise: Promise) {
    DispatchQueue.global(qos: .userInitiated).async {
      guard let document = self.openAndUnlock(filePath: filePath, password: password, promise: promise) else {
        return
      }

      var fullText = ""
      for pageIndex in 0..<document.pageCount {
        if let page = document.page(at: pageIndex),
           let pageText = page.string {
          fullText += pageText
          if pageIndex < document.pageCount - 1 {
            fullText += "\n"
          }
        }
      }

      promise.resolve(fullText)
    }
  }

  private func getPageCount(filePath: String, password: String?, promise: Promise) {
    DispatchQueue.global(qos: .userInitiated).async {
      guard let document = self.openAndUnlock(filePath: filePath, password: password, promise: promise) else {
        return
      }
      promise.resolve(document.pageCount)
    }
  }

  private func extractTextFromPage(filePath: String, pageNumber: Int, password: String?, promise: Promise) {
    DispatchQueue.global(qos: .userInitiated).async {
      guard let document = self.openAndUnlock(filePath: filePath, password: password, promise: promise) else {
        return
      }

      let pageIndex = pageNumber - 1
      guard pageIndex >= 0 && pageIndex < document.pageCount else {
        promise.reject("PDF_PAGE_ERROR", "Page \(pageNumber) out of range (1-\(document.pageCount))")
        return
      }

      guard let page = document.page(at: pageIndex) else {
        promise.reject("PDF_PAGE_ERROR", "Failed to get page \(pageNumber)")
        return
      }

      promise.resolve(page.string ?? "")
    }
  }

  /// True iff the PDF is encrypted and not unlockable without a password.
  /// PDFs that declare encryption but open immediately return false.
  private func isPasswordProtected(filePath: String, promise: Promise) {
    DispatchQueue.global(qos: .userInitiated).async {
      let url: URL
      do {
        url = try self.getFileURL(from: filePath)
      } catch let error as NSError {
        let code = error.domain == "PdfExtractor" && error.code == 2 ? "FILE_NOT_FOUND" : "PDF_LOAD_ERROR"
        promise.reject(code, error.localizedDescription)
        return
      }

      guard let document = PDFDocument(url: url) else {
        promise.reject("PDF_LOAD_ERROR", "Failed to load PDF document")
        return
      }

      // isLocked is true iff the document is encrypted AND not yet unlocked.
      // It's false for both clear PDFs and encrypted-with-empty-password PDFs.
      promise.resolve(document.isLocked)
    }
  }

  private func getFileURL(from filePath: String) throws -> URL {
    if filePath.hasPrefix("file://") {
      guard let url = URL(string: filePath) else {
        throw NSError(domain: "PdfExtractor", code: 1, userInfo: [
          NSLocalizedDescriptionKey: "Invalid file URI: \(filePath)"
        ])
      }
      return url
    } else {
      let url = URL(fileURLWithPath: filePath)
      guard FileManager.default.fileExists(atPath: url.path) else {
        throw NSError(domain: "PdfExtractor", code: 2, userInfo: [
          NSLocalizedDescriptionKey: "File not found: \(filePath)"
        ])
      }
      return url
    }
  }
}
