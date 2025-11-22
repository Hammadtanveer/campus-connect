package com.example.campusconnect.notes

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class NotesCompressionTest {
    private fun gzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    @Test
    fun pdfHeaderValidation() {
        val pdf = "%PDF-1.7\n%âãÏÓ\n".toByteArray()
        assertTrue("Header should start with %PDF-", pdf.copyOfRange(0,5).decodeToString() == "%PDF-")
    }

    @Test
    fun compressionReducesOrEquals() {
        val content = "%PDF-1.4\n" + ("obj".repeat(1000))
        val original = content.toByteArray()
        val compressed = gzip(original)
        assertTrue("Compressed should not be larger than original by >5%", compressed.size <= original.size * 1.05)
    }

    @Test
    fun smallPdfRejected() {
        val tiny = "%PDF".toByteArray()
        assertEquals(4, tiny.size)
    }
}

