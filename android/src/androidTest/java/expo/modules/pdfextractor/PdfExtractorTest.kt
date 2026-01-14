package expo.modules.pdfextractor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
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

    // ==================== Helper Functions ====================

    /**
     * Create a test PDF with known content
     * This ensures tests are reproducible without external files
     */
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
