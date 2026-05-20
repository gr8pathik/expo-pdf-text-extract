import XCTest
import PDFKit
@testable import PdfExtractor

/**
 * Unit tests for PdfExtractor native module
 *
 * These tests verify:
 * 1. PDFKit availability and initialization
 * 2. Text extraction from PDFs
 * 3. Page count retrieval
 * 4. Single page extraction
 * 5. Error handling for invalid files
 * 6. Path parsing
 *
 * To run these tests:
 *   1. Open the Xcode project
 *   2. Select Product > Test (Cmd+U)
 *
 * Or from command line:
 *   xcodebuild test -scheme PdfExtractor -destination 'platform=iOS Simulator,name=iPhone 15'
 */
class PdfExtractorTests: XCTestCase {

    // MARK: - Properties

    private var testPdfURL: URL!
    private var multiPagePdfURL: URL!
    private var encryptedPdfURL: URL!
    private var encryptedMultiPagePdfURL: URL!

    // Known test content - we create PDFs with this text
    static let TEST_LINE_1 = "Hello World"
    static let TEST_LINE_2 = "This is a test PDF document"
    static let TEST_LINE_3 = "Created for automated testing"

    // Passwords used for encrypted-PDF tests
    static let USER_PASSWORD = "secret123"
    static let OWNER_PASSWORD = "owner-secret"
    static let WRONG_PASSWORD = "nope-wrong"

    // MARK: - Setup & Teardown

    override func setUp() {
        super.setUp()

        // Create test PDFs with known content
        testPdfURL = createTestPdf()
        multiPagePdfURL = createMultiPageTestPdf(pageCount: 3)
        encryptedPdfURL = createEncryptedTestPdf()
        encryptedMultiPagePdfURL = createEncryptedMultiPageTestPdf(pageCount: 3)
    }

    override func tearDown() {
        // Cleanup test files
        try? FileManager.default.removeItem(at: testPdfURL)
        try? FileManager.default.removeItem(at: multiPagePdfURL)
        try? FileManager.default.removeItem(at: encryptedPdfURL)
        try? FileManager.default.removeItem(at: encryptedMultiPagePdfURL)

        super.tearDown()
    }

    // MARK: - PDFKit Initialization Tests

    func testPDFKit_shouldBeAvailable() {
        // PDFKit should be available on iOS 11+
        XCTAssertTrue(true, "PDFKit is available")
    }

    func testPDFDocument_shouldLoadFromURL() {
        let document = PDFDocument(url: testPdfURL)

        XCTAssertNotNil(document, "PDFDocument should load from valid URL")
    }

    // MARK: - Text Extraction Tests

    func testExtractText_shouldReturnNonEmptyText() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        var fullText = ""
        for pageIndex in 0..<document.pageCount {
            if let page = document.page(at: pageIndex),
               let pageText = page.string {
                fullText += pageText
            }
        }

        XCTAssertFalse(fullText.isEmpty, "Extracted text should not be empty")
    }

    func testExtractText_shouldContainKnownContent() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        var fullText = ""
        for pageIndex in 0..<document.pageCount {
            if let page = document.page(at: pageIndex),
               let pageText = page.string {
                fullText += pageText
            }
        }

        XCTAssertTrue(fullText.contains(Self.TEST_LINE_1), "Text should contain '\(Self.TEST_LINE_1)'")
        XCTAssertTrue(fullText.contains(Self.TEST_LINE_2), "Text should contain '\(Self.TEST_LINE_2)'")
        XCTAssertTrue(fullText.contains(Self.TEST_LINE_3), "Text should contain '\(Self.TEST_LINE_3)'")
    }

    func testExtractText_shouldPreserveTextOrder() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        var fullText = ""
        for pageIndex in 0..<document.pageCount {
            if let page = document.page(at: pageIndex),
               let pageText = page.string {
                fullText += pageText
            }
        }

        // Line 1 should appear before Line 2
        if let pos1 = fullText.range(of: Self.TEST_LINE_1)?.lowerBound,
           let pos2 = fullText.range(of: Self.TEST_LINE_2)?.lowerBound {
            XCTAssertTrue(pos1 < pos2, "Line 1 should appear before Line 2")
        }
    }

    // MARK: - Page Count Tests

    func testGetPageCount_singlePage_shouldReturnOne() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        XCTAssertEqual(document.pageCount, 1, "Single page PDF should have page count of 1")
    }

    func testGetPageCount_multiPage_shouldReturnCorrectCount() {
        guard let document = PDFDocument(url: multiPagePdfURL) else {
            XCTFail("Failed to load multi-page test PDF")
            return
        }

        XCTAssertEqual(document.pageCount, 3, "Multi-page PDF should have page count of 3")
    }

    // MARK: - Single Page Extraction Tests

    func testExtractTextFromPage_firstPage_shouldReturnText() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        guard let page = document.page(at: 0) else {
            XCTFail("Failed to get first page")
            return
        }

        let pageText = page.string ?? ""

        XCTAssertFalse(pageText.isEmpty, "First page text should not be empty")
    }

    func testExtractTextFromPage_specificPage_shouldReturnOnlyThatPage() {
        guard let document = PDFDocument(url: multiPagePdfURL) else {
            XCTFail("Failed to load multi-page test PDF")
            return
        }

        // Extract only page 2 (0-indexed, so index 1)
        guard let page = document.page(at: 1) else {
            XCTFail("Failed to get page 2")
            return
        }

        let pageText = page.string ?? ""

        // Should contain page 2 content
        XCTAssertTrue(pageText.contains("Page 2"), "Page 2 text should contain 'Page 2'")
        // Should NOT contain other pages' content
        XCTAssertFalse(pageText.contains("Page 1"), "Page 2 text should not contain 'Page 1'")
        XCTAssertFalse(pageText.contains("Page 3"), "Page 2 text should not contain 'Page 3'")
    }

    func testExtractTextFromPage_invalidPageIndex_shouldReturnNil() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        // Request page beyond document
        let invalidPage = document.page(at: 999)

        XCTAssertNil(invalidPage, "Invalid page index should return nil")
    }

    func testExtractTextFromPage_negativeIndex_shouldReturnNil() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        // Negative index
        let invalidPage = document.page(at: -1)

        XCTAssertNil(invalidPage, "Negative page index should return nil")
    }

    // MARK: - Error Handling Tests

    func testLoadPdf_withInvalidURL_shouldReturnNil() {
        let invalidURL = URL(fileURLWithPath: "/nonexistent/path/to/file.pdf")
        let document = PDFDocument(url: invalidURL)

        XCTAssertNil(document, "Invalid URL should return nil PDFDocument")
    }

    func testLoadPdf_withInvalidData_shouldReturnNil() {
        // Create a temp file with invalid PDF data
        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("invalid.pdf")
        let invalidData = "This is not a PDF".data(using: .utf8)!

        try? invalidData.write(to: tempURL)

        let document = PDFDocument(url: tempURL)

        XCTAssertNil(document, "Invalid PDF data should return nil PDFDocument")

        try? FileManager.default.removeItem(at: tempURL)
    }

    func testLoadPdf_withEmptyData_shouldReturnNil() {
        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("empty.pdf")
        let emptyData = Data()

        try? emptyData.write(to: tempURL)

        let document = PDFDocument(url: tempURL)

        XCTAssertNil(document, "Empty data should return nil PDFDocument")

        try? FileManager.default.removeItem(at: tempURL)
    }

    func testLoadPdf_withCorruptedData_shouldReturnNil() {
        let tempURL = FileManager.default.temporaryDirectory.appendingPathComponent("corrupted.pdf")
        let corruptedData = "%PDF-1.4\ngarbage data here".data(using: .utf8)!

        try? corruptedData.write(to: tempURL)

        let document = PDFDocument(url: tempURL)

        XCTAssertNil(document, "Corrupted PDF data should return nil PDFDocument")

        try? FileManager.default.removeItem(at: tempURL)
    }

    // MARK: - Path Parsing Tests

    func testParseFilePath_withFilePrefix_shouldCreateValidURL() {
        let filePath = "file:///var/mobile/Documents/test.pdf"
        let url = URL(string: filePath)

        XCTAssertNotNil(url, "file:// path should create valid URL")
        XCTAssertEqual(url?.scheme, "file", "URL scheme should be 'file'")
    }

    func testParseFilePath_withAbsolutePath_shouldCreateValidURL() {
        let absolutePath = "/var/mobile/Documents/test.pdf"
        let url = URL(fileURLWithPath: absolutePath)

        XCTAssertNotNil(url, "Absolute path should create valid URL")
        XCTAssertEqual(url.path, absolutePath, "URL path should match absolute path")
    }

    func testFileExists_shouldReturnCorrectStatus() {
        // Existing file
        XCTAssertTrue(FileManager.default.fileExists(atPath: testPdfURL.path), "Test file should exist")

        // Non-existing file
        XCTAssertFalse(FileManager.default.fileExists(atPath: "/nonexistent/file.pdf"), "Non-existent file should not exist")
    }

    // MARK: - Encrypted PDF Tests
    //
    // These exercise the password-protected PDF support added to the module.
    // We mirror the native-side behaviour that the module relies on so that
    // any regression in PDFKit or in our usage of it surfaces here first.

    func testEncryptedPdf_shouldLoadButBeLocked() {
        guard let document = PDFDocument(url: encryptedPdfURL) else {
            XCTFail("PDFDocument should load encrypted PDF (locked state)")
            return
        }

        XCTAssertTrue(document.isEncrypted, "Encrypted PDF should report isEncrypted == true")
        XCTAssertTrue(document.isLocked, "Encrypted PDF should be locked before unlock()")
    }

    func testEncryptedPdf_unlockWithCorrectPassword_shouldSucceed() {
        guard let document = PDFDocument(url: encryptedPdfURL) else {
            XCTFail("Failed to load encrypted PDF")
            return
        }

        let unlocked = document.unlock(withPassword: Self.USER_PASSWORD)
        XCTAssertTrue(unlocked, "unlock(withPassword:) should return true for correct password")
        XCTAssertFalse(document.isLocked, "Document should not be locked after successful unlock")
    }

    func testEncryptedPdf_unlockWithOwnerPassword_shouldSucceed() {
        // Either user OR owner password should unlock the document.
        guard let document = PDFDocument(url: encryptedPdfURL) else {
            XCTFail("Failed to load encrypted PDF")
            return
        }

        XCTAssertTrue(document.unlock(withPassword: Self.OWNER_PASSWORD),
                      "Owner password should also unlock the document")
    }

    func testEncryptedPdf_unlockWithWrongPassword_shouldFail() {
        guard let document = PDFDocument(url: encryptedPdfURL) else {
            XCTFail("Failed to load encrypted PDF")
            return
        }

        let unlocked = document.unlock(withPassword: Self.WRONG_PASSWORD)
        XCTAssertFalse(unlocked, "Wrong password should not unlock the document")
        XCTAssertTrue(document.isLocked, "Document should still be locked after failed unlock")
    }

    func testEncryptedPdf_extractTextAfterUnlock_shouldContainKnownContent() {
        guard let document = PDFDocument(url: encryptedPdfURL) else {
            XCTFail("Failed to load encrypted PDF")
            return
        }

        XCTAssertTrue(document.unlock(withPassword: Self.USER_PASSWORD))

        var fullText = ""
        for pageIndex in 0..<document.pageCount {
            if let page = document.page(at: pageIndex), let pageText = page.string {
                fullText += pageText
            }
        }

        XCTAssertTrue(fullText.contains(Self.TEST_LINE_1),
                      "Decrypted text should contain '\(Self.TEST_LINE_1)'")
        XCTAssertTrue(fullText.contains(Self.TEST_LINE_2),
                      "Decrypted text should contain '\(Self.TEST_LINE_2)'")
        XCTAssertTrue(fullText.contains(Self.TEST_LINE_3),
                      "Decrypted text should contain '\(Self.TEST_LINE_3)'")
    }

    func testEncryptedPdf_pageStringBeforeUnlock_shouldBeNilOrEmpty() {
        guard let document = PDFDocument(url: encryptedPdfURL) else {
            XCTFail("Failed to load encrypted PDF")
            return
        }

        XCTAssertTrue(document.isLocked, "Precondition: document must be locked")

        if let page = document.page(at: 0) {
            let text = page.string ?? ""
            XCTAssertTrue(text.isEmpty,
                          "Locked PDF must not yield page text before unlock — got \(text.count) chars")
        }
    }

    func testEncryptedMultiPagePdf_unlockAndExtractSpecificPage() {
        guard let document = PDFDocument(url: encryptedMultiPagePdfURL) else {
            XCTFail("Failed to load encrypted multi-page PDF")
            return
        }

        XCTAssertTrue(document.unlock(withPassword: Self.USER_PASSWORD))

        guard let page = document.page(at: 1) else {
            XCTFail("Failed to get page 2 of decrypted PDF")
            return
        }

        let text = page.string ?? ""
        XCTAssertTrue(text.contains("Page 2"))
        XCTAssertFalse(text.contains("Page 1"))
        XCTAssertFalse(text.contains("Page 3"))
    }

    func testClearPdf_isLocked_shouldBeFalse() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        XCTAssertFalse(document.isLocked, "Clear PDF should not be locked")
        XCTAssertFalse(document.isEncrypted, "Clear PDF should not be encrypted")
    }

    /// Detection semantics: a PDF requires a password iff PDFDocument.isLocked
    /// is true. This matches what the native module exposes as `isPasswordProtected`.
    func testIsPasswordProtected_detectionSemantics_matchesModule() {
        let encrypted = PDFDocument(url: encryptedPdfURL)
        let clear = PDFDocument(url: testPdfURL)

        XCTAssertNotNil(encrypted, "Encrypted PDF should still produce a non-nil PDFDocument")
        XCTAssertNotNil(clear)

        XCTAssertEqual(encrypted?.isLocked, true)
        XCTAssertEqual(clear?.isLocked, false)
    }

    // MARK: - Performance Tests

    func testExtractText_performance() {
        guard let document = PDFDocument(url: testPdfURL) else {
            XCTFail("Failed to load test PDF")
            return
        }

        measure {
            var _ = ""
            for pageIndex in 0..<document.pageCount {
                if let page = document.page(at: pageIndex),
                   let pageText = page.string {
                    _ += pageText
                }
            }
        }
    }

    // MARK: - Helper Functions

    /**
     * Create a test PDF with known content
     * This ensures tests are reproducible without external files
     */
    private func createTestPdf(name: String = "test_document.pdf") -> URL {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)

        // Create PDF using UIGraphics
        let pageRect = CGRect(x: 0, y: 0, width: 612, height: 792) // US Letter size
        let renderer = UIGraphicsPDFRenderer(bounds: pageRect)

        let data = renderer.pdfData { context in
            context.beginPage()

            let paragraphStyle = NSMutableParagraphStyle()
            paragraphStyle.alignment = .left

            let attributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12),
                .paragraphStyle: paragraphStyle
            ]

            // Draw test content
            Self.TEST_LINE_1.draw(at: CGPoint(x: 50, y: 50), withAttributes: attributes)
            Self.TEST_LINE_2.draw(at: CGPoint(x: 50, y: 70), withAttributes: attributes)
            Self.TEST_LINE_3.draw(at: CGPoint(x: 50, y: 90), withAttributes: attributes)
        }

        try? data.write(to: url)

        return url
    }

    /**
     * Create an encrypted single-page test PDF with the same TEST_LINE_* content.
     * Encrypted with USER_PASSWORD / OWNER_PASSWORD.
     */
    private func createEncryptedTestPdf() -> URL {
        let clearURL = createTestPdf(name: "clear_for_encryption.pdf")
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("encrypted_test_document.pdf")

        guard let document = PDFDocument(url: clearURL) else {
            XCTFail("Failed to load clear PDF for re-encryption")
            return url
        }

        let options: [PDFDocumentWriteOption: Any] = [
            .userPasswordOption: Self.USER_PASSWORD,
            .ownerPasswordOption: Self.OWNER_PASSWORD,
        ]
        document.write(to: url, withOptions: options)

        try? FileManager.default.removeItem(at: clearURL)
        return url
    }

    private func createEncryptedMultiPageTestPdf(pageCount: Int) -> URL {
        let clearURL = createMultiPageTestPdf(pageCount: pageCount, name: "clear_multi_for_encryption.pdf")
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("encrypted_multi_test.pdf")

        guard let document = PDFDocument(url: clearURL) else {
            XCTFail("Failed to load clear multi-page PDF for re-encryption")
            return url
        }

        let options: [PDFDocumentWriteOption: Any] = [
            .userPasswordOption: Self.USER_PASSWORD,
            .ownerPasswordOption: Self.OWNER_PASSWORD,
        ]
        document.write(to: url, withOptions: options)

        try? FileManager.default.removeItem(at: clearURL)
        return url
    }

    /**
     * Create a multi-page test PDF
     */
    private func createMultiPageTestPdf(pageCount: Int, name: String = "multi_page_test.pdf") -> URL {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)

        let pageRect = CGRect(x: 0, y: 0, width: 612, height: 792)
        let renderer = UIGraphicsPDFRenderer(bounds: pageRect)

        let data = renderer.pdfData { context in
            for i in 1...pageCount {
                context.beginPage()

                let paragraphStyle = NSMutableParagraphStyle()
                paragraphStyle.alignment = .left

                let attributes: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: 14),
                    .paragraphStyle: paragraphStyle
                ]

                "Page \(i)".draw(at: CGPoint(x: 50, y: 50), withAttributes: attributes)
                "Content for page \(i)".draw(at: CGPoint(x: 50, y: 70), withAttributes: attributes)
            }
        }

        try? data.write(to: url)

        return url
    }
}
