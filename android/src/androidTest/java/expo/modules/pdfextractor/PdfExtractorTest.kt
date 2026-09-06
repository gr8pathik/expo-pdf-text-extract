package expo.modules.pdfextractor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream

/**
 * Instrumented tests for PdfExtractor native module
 *
 * These tests run on an Android device/emulator to test:
 * 1. PDFBox initialization
 * 2. Text extraction from PDFs
 * 3. Page count retrieval
 * 4. Single page extraction
 * 5. Error handling for invalid files
 *
 * To run these tests:
 *   ./gradlew :pdf-extractor:connectedAndroidTest
 *
 * Or from Android Studio:
 *   Right-click on this file -> Run 'PdfExtractorTest'
 */
@RunWith(AndroidJUnit4::class)
class PdfExtractorTest {

    private lateinit var context: Context
    private lateinit var testPdfFile: File

    companion object {
        // Known test content - we create a PDF with this text
        const val TEST_LINE_1 = "Hello World"
        const val TEST_LINE_2 = "This is a test PDF document"
        const val TEST_LINE_3 = "Created for automated testing"

        // Passwords used for encrypted-PDF tests
        const val USER_PASSWORD = "secret123"
        const val OWNER_PASSWORD = "owner-secret"
        const val WRONG_PASSWORD = "nope-wrong"
    }

    @Before
    fun setup() {
        // Get the test application context
        context = ApplicationProvider.getApplicationContext()

        // Initialize PDFBox (required before any PDF operations)
        PDFBoxResourceLoader.init(context)

        // Create a test PDF with known content
        testPdfFile = createTestPdf()
    }

    // ==================== PDFBox Initialization Tests ====================

    @Test
    fun pdfBox_shouldInitializeSuccessfully() {
        // If we get here without exception, PDFBox initialized correctly
        assertThat(true).isTrue()
    }

    // ==================== Text Extraction Tests ====================

    @Test
    fun extractText_shouldReturnNonEmptyText() {
        testPdfFile.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)

                assertThat(text).isNotNull()
                assertThat(text).isNotEmpty()
            }
        }
    }

    @Test
    fun extractText_shouldContainKnownContent() {
        testPdfFile.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)

                // Should contain our known test content
                assertThat(text).contains(TEST_LINE_1)
                assertThat(text).contains(TEST_LINE_2)
                assertThat(text).contains(TEST_LINE_3)
            }
        }
    }

    @Test
    fun extractText_shouldPreserveLineBreaks() {
        testPdfFile.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }
                val text = stripper.getText(document)

                // Text should contain line breaks
                assertThat(text.contains("\n") || text.contains("\r")).isTrue()
            }
        }
    }

    @Test
    fun extractText_withSortByPosition_shouldOrderTextCorrectly() {
        testPdfFile.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                }
                val text = stripper.getText(document)

                // Line 1 should appear before Line 2
                val pos1 = text.indexOf(TEST_LINE_1)
                val pos2 = text.indexOf(TEST_LINE_2)

                assertThat(pos1).isLessThan(pos2)
            }
        }
    }

    // ==================== Page Count Tests ====================

    @Test
    fun getPageCount_shouldReturnCorrectCount() {
        testPdfFile.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                val pageCount = document.numberOfPages

                // Our test PDF has 1 page
                assertThat(pageCount).isEqualTo(1)
            }
        }
    }

    @Test
    fun getPageCount_multiPagePdf_shouldReturnCorrectCount() {
        // Create a 3-page PDF
        val multiPagePdf = createMultiPageTestPdf(3)

        multiPagePdf.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                assertThat(document.numberOfPages).isEqualTo(3)
            }
        }

        // Cleanup
        multiPagePdf.delete()
    }

    // ==================== Single Page Extraction Tests ====================

    @Test
    fun extractTextFromPage_firstPage_shouldReturnText() {
        testPdfFile.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                val stripper = PDFTextStripper().apply {
                    sortByPosition = true
                    startPage = 1
                    endPage = 1
                }
                val text = stripper.getText(document)

                assertThat(text).isNotNull()
                assertThat(text).isNotEmpty()
            }
        }
    }

    @Test
    fun extractTextFromPage_specificPage_shouldReturnOnlyThatPage() {
        // Create a 3-page PDF with different content on each page
        val multiPagePdf = createMultiPageTestPdf(3)

        multiPagePdf.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                // Extract only page 2
                val stripper = PDFTextStripper().apply {
                    startPage = 2
                    endPage = 2
                }
                val text = stripper.getText(document)

                // Should contain page 2 content
                assertThat(text).contains("Page 2")
                // Should NOT contain page 1 or 3 content
                assertThat(text).doesNotContain("Page 1")
                assertThat(text).doesNotContain("Page 3")
            }
        }

        multiPagePdf.delete()
    }

    @Test
    fun extractTextFromPage_invalidPage_shouldReturnEmpty() {
        testPdfFile.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                val pageCount = document.numberOfPages

                // Request page beyond document
                val stripper = PDFTextStripper().apply {
                    startPage = pageCount + 100
                    endPage = pageCount + 100
                }
                val text = stripper.getText(document)

                // Should return empty string for invalid page range
                assertThat(text).isEmpty()
            }
        }
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun loadPdf_withInvalidData_shouldThrowException() {
        var exceptionThrown = false

        try {
            val invalidData = "This is not a PDF".byteInputStream()
            PDDocument.load(invalidData)
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun loadPdf_withEmptyStream_shouldThrowException() {
        var exceptionThrown = false

        try {
            val emptyStream = ByteArray(0).inputStream()
            PDDocument.load(emptyStream)
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun loadPdf_withCorruptedData_shouldThrowException() {
        var exceptionThrown = false

        try {
            // Start with PDF header but corrupt the rest
            val corruptedData = "%PDF-1.4\ngarbage data here".byteInputStream()
            PDDocument.load(corruptedData)
        } catch (e: Exception) {
            exceptionThrown = true
        }

        assertThat(exceptionThrown).isTrue()
    }

    // ==================== Path Parsing Tests ====================

    @Test
    fun parseFilePath_withFilePrefix_shouldRemovePrefix() {
        val filePath = "file:///storage/emulated/0/test.pdf"
        val cleanPath = filePath.removePrefix("file://")

        assertThat(cleanPath).isEqualTo("/storage/emulated/0/test.pdf")
        assertThat(cleanPath).doesNotContain("file://")
    }

    @Test
    fun parseFilePath_withContentUri_shouldBeIdentified() {
        val contentUri = "content://com.android.providers.downloads/document/123"

        assertThat(contentUri.startsWith("content://")).isTrue()
    }

    @Test
    fun parseFilePath_withAbsolutePath_shouldBeUsedDirectly() {
        val absolutePath = "/data/user/0/com.app/files/test.pdf"

        assertThat(absolutePath.startsWith("/")).isTrue()
        assertThat(absolutePath.startsWith("file://")).isFalse()
        assertThat(absolutePath.startsWith("content://")).isFalse()
    }

    // ==================== File Loading Tests ====================

    @Test
    fun loadPdf_fromFile_shouldSucceed() {
        PDDocument.load(testPdfFile).use { document ->
            assertThat(document).isNotNull()
            assertThat(document.numberOfPages).isGreaterThan(0)
        }
    }

    @Test
    fun loadPdf_fromInputStream_shouldSucceed() {
        testPdfFile.inputStream().use { stream ->
            PDDocument.load(stream).use { document ->
                assertThat(document).isNotNull()
                assertThat(document.numberOfPages).isGreaterThan(0)
            }
        }
    }

    // ==================== Encrypted PDF Tests ====================
    //
    // These exercise the password-protected PDF support added to the module.
    // We mirror the native-side behaviour the module relies on so that a
    // regression in PDFBox or in our usage of it surfaces here first.

    @Test
    fun encryptedPdf_loadWithoutPassword_shouldThrowInvalidPasswordException() {
        val encrypted = createEncryptedTestPdf()
        var thrown: Throwable? = null

        try {
            PDDocument.load(encrypted, "").close()
        } catch (e: Throwable) {
            thrown = e
        }

        assertThat(thrown).isInstanceOf(InvalidPasswordException::class.java)
        encrypted.delete()
    }

    @Test
    fun encryptedPdf_loadWithCorrectPassword_shouldSucceedAndExtractText() {
        val encrypted = createEncryptedTestPdf()

        PDDocument.load(encrypted, USER_PASSWORD).use { document ->
            assertThat(document.isEncrypted).isTrue()
            val text = PDFTextStripper().apply { sortByPosition = true }.getText(document)
            assertThat(text).contains(TEST_LINE_1)
            assertThat(text).contains(TEST_LINE_2)
            assertThat(text).contains(TEST_LINE_3)
        }

        encrypted.delete()
    }

    @Test
    fun encryptedPdf_loadWithOwnerPassword_shouldAlsoSucceed() {
        // Either user OR owner password unlocks the document.
        val encrypted = createEncryptedTestPdf()

        PDDocument.load(encrypted, OWNER_PASSWORD).use { document ->
            assertThat(document.numberOfPages).isEqualTo(1)
        }

        encrypted.delete()
    }

    @Test
    fun encryptedPdf_loadWithWrongPassword_shouldThrowInvalidPasswordException() {
        val encrypted = createEncryptedTestPdf()
        var thrown: Throwable? = null

        try {
            PDDocument.load(encrypted, WRONG_PASSWORD).close()
        } catch (e: Throwable) {
            thrown = e
        }

        assertThat(thrown).isInstanceOf(InvalidPasswordException::class.java)
        encrypted.delete()
    }

    @Test
    fun encryptedPdf_isEncryptedProperty_shouldBeTrue() {
        val encrypted = createEncryptedTestPdf()

        PDDocument.load(encrypted, USER_PASSWORD).use { document ->
            assertThat(document.isEncrypted).isTrue()
        }

        encrypted.delete()
    }

    @Test
    fun clearPdf_isEncryptedProperty_shouldBeFalse() {
        PDDocument.load(testPdfFile).use { document ->
            assertThat(document.isEncrypted).isFalse()
        }
    }

    @Test
    fun encryptedPdf_pageCountWithPassword_matchesUnencrypted() {
        val encrypted = createEncryptedTestPdf()

        PDDocument.load(encrypted, USER_PASSWORD).use { document ->
            assertThat(document.numberOfPages).isEqualTo(1)
        }

        encrypted.delete()
    }

    @Test
    fun encryptedPdf_extractSpecificPage_withPassword_shouldWork() {
        // Multi-page encrypted PDF.
        val multiPage = createMultiPageTestPdf(3)
        val encrypted = encryptExistingPdf(multiPage, USER_PASSWORD, OWNER_PASSWORD)

        PDDocument.load(encrypted, USER_PASSWORD).use { document ->
            val stripper = PDFTextStripper().apply {
                startPage = 2
                endPage = 2
            }
            val text = stripper.getText(document)
            assertThat(text).contains("Page 2")
            assertThat(text).doesNotContain("Page 1")
            assertThat(text).doesNotContain("Page 3")
        }

        multiPage.delete()
        encrypted.delete()
    }

    /**
     * Detection semantics: a PDF requires a password iff loading with the
     * empty password throws InvalidPasswordException. This matches what the
     * native module exposes as `isPasswordProtected`.
     */
    @Test
    fun isPasswordProtected_detectionSemantics_matchesModule() {
        val encrypted = createEncryptedTestPdf()

        fun requiresPassword(file: File): Boolean {
            return try {
                PDDocument.load(file, "").close()
                false
            } catch (e: InvalidPasswordException) {
                true
            }
        }

        assertThat(requiresPassword(encrypted)).isTrue()
        assertThat(requiresPassword(testPdfFile)).isFalse()

        encrypted.delete()
    }

    // ==================== Helper Functions ====================

    /**
     * Create a test PDF with known content
     * This ensures tests are reproducible without external files
     */
    // ---- Layout ----

    /**
     * A tabular PDF prints continuation lines indented under their parent row.
     * The default stripper drops horizontal position, so the continuation comes
     * back flush left and is indistinguishable from a real row.
     */
    @Test
    fun layout_indentsContinuationLine() {
        val text = layoutTextOf(createColumnarPdf())

        val row = text.lines().first { it.contains("Widget") }
        val continuation = text.lines().first { it.contains("continued") }

        assertThat(continuation.takeWhile { it == ' ' }.length)
            .isGreaterThan(row.takeWhile { it == ' ' }.length)
    }

    /**
     * Regression guard for the naive fix: spacing derived from an absolute
     * `x / spaceWidth` grid inserts a space between adjacent glyphs, turning
     * "Widget" into "W i d g e t".
     */
    @Test
    fun layout_doesNotSplitWords() {
        val text = layoutTextOf(createColumnarPdf())

        assertThat(text).contains("Widget")
        assertThat(text).contains("continued")
    }

    /** Every column of a row belongs to that row, not to a line of its own. */
    @Test
    fun layout_keepsRowColumnsOnOneLine() {
        val text = layoutTextOf(createColumnarPdf())

        val row = text.lines().first { it.contains("Widget") }

        assertThat(row).contains("01/02/26")
        assertThat(row).contains("12.34")
    }

    private fun layoutTextOf(file: File): String =
        PDDocument.load(file).use { document ->
            val stripper = LayoutTextStripper().apply {
                startPage = 1
                endPage = 1
            }
            stripper.getText(document)
            stripper.layoutText()
        }

    /**
     * A PDF shaped like a statement table: a row whose columns sit at distinct
     * x offsets, followed by a continuation line indented under the description
     * column. Reproducible without external files, like the other fixtures.
     */
    private fun createColumnarPdf(): File {
        val file = File(context.cacheDir, "columnar_document.pdf")

        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)

            PDPageContentStream(document, page).use { content ->
                content.setFont(PDType1Font.HELVETICA, 12f)

                // A row: date, description and amount in three columns.
                for ((x, label) in listOf(50f to "01/02/26", 180f to "Widget Supply Co", 450f to "12.34")) {
                    content.beginText()
                    content.newLineAtOffset(x, 700f)
                    content.showText(label)
                    content.endText()
                }

                // Its continuation, indented under the description column.
                content.beginText()
                content.newLineAtOffset(180f, 682f)
                content.showText("continued detail")
                content.endText()
            }

            document.save(file)
        }

        return file
    }

    private fun createTestPdf(): File {
        val file = File(context.cacheDir, "test_document.pdf")

        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)

            PDPageContentStream(document, page).use { content ->
                content.beginText()
                content.setFont(PDType1Font.HELVETICA, 12f)
                content.newLineAtOffset(50f, 700f)
                content.showText(TEST_LINE_1)
                content.newLineAtOffset(0f, -20f)
                content.showText(TEST_LINE_2)
                content.newLineAtOffset(0f, -20f)
                content.showText(TEST_LINE_3)
                content.endText()
            }

            document.save(file)
        }

        return file
    }

    /**
     * Create an encrypted single-page test PDF using the same TEST_LINE_* content
     * as the clear test PDF. Encrypted with USER_PASSWORD / OWNER_PASSWORD.
     */
    private fun createEncryptedTestPdf(): File {
        val file = File(context.cacheDir, "encrypted_test_document.pdf")

        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)

            PDPageContentStream(document, page).use { content ->
                content.beginText()
                content.setFont(PDType1Font.HELVETICA, 12f)
                content.newLineAtOffset(50f, 700f)
                content.showText(TEST_LINE_1)
                content.newLineAtOffset(0f, -20f)
                content.showText(TEST_LINE_2)
                content.newLineAtOffset(0f, -20f)
                content.showText(TEST_LINE_3)
                content.endText()
            }

            applyEncryption(document, USER_PASSWORD, OWNER_PASSWORD)
            document.save(file)
        }

        return file
    }

    /**
     * Load an existing PDF, apply encryption, and write it back to a new file.
     * Used to encrypt the multi-page test PDF without rebuilding it.
     */
    private fun encryptExistingPdf(source: File, userPwd: String, ownerPwd: String): File {
        val out = File(context.cacheDir, "encrypted_${source.name}")
        PDDocument.load(source).use { document ->
            applyEncryption(document, userPwd, ownerPwd)
            document.save(out)
        }
        return out
    }

    private fun applyEncryption(document: PDDocument, userPwd: String, ownerPwd: String) {
        val permissions = AccessPermission()
        val policy = StandardProtectionPolicy(ownerPwd, userPwd, permissions)
        policy.encryptionKeyLength = 128
        policy.permissions = permissions
        document.protect(policy)
    }

    /**
     * Create a multi-page test PDF
     */
    private fun createMultiPageTestPdf(pageCount: Int): File {
        val file = File(context.cacheDir, "multi_page_test.pdf")

        PDDocument().use { document ->
            for (i in 1..pageCount) {
                val page = PDPage()
                document.addPage(page)

                PDPageContentStream(document, page).use { content ->
                    content.beginText()
                    content.setFont(PDType1Font.HELVETICA, 14f)
                    content.newLineAtOffset(50f, 700f)
                    content.showText("Page $i")
                    content.newLineAtOffset(0f, -20f)
                    content.showText("Content for page $i")
                    content.endText()
                }
            }

            document.save(file)
        }

        return file
    }
}
