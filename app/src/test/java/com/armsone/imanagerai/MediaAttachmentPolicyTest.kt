package com.armsone.imanagerai

import com.armsone.imanagerai.ui.composer.MediaAttachmentPolicy
import com.armsone.imanagerai.ui.composer.MediaKind
import com.armsone.imanagerai.ui.composer.ComposerImagePipeline
import com.armsone.imanagerai.ui.composer.ComposerImagePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAttachmentPolicyTest {
    @Test
    fun `attachment slots stop at eight`() {
        assertEquals(8, MediaAttachmentPolicy.availableSlots(0))
        assertEquals(1, MediaAttachmentPolicy.availableSlots(7))
        assertEquals(0, MediaAttachmentPolicy.availableSlots(8))
        assertEquals(0, MediaAttachmentPolicy.availableSlots(9))
    }

    @Test
    fun `sharing accepts one through eight attachments only`() {
        assertFalse(MediaAttachmentPolicy.canShare(0))
        assertTrue(MediaAttachmentPolicy.canShare(1))
        assertTrue(MediaAttachmentPolicy.canShare(8))
        assertFalse(MediaAttachmentPolicy.canShare(9))
    }

    @Test
    fun `mime type resolves to image for image only attachments`() {
        assertEquals("image/*", MediaAttachmentPolicy.mimeTypeFor(listOf(MediaKind.IMAGE)))
        assertEquals(
            "image/*",
            MediaAttachmentPolicy.mimeTypeFor(listOf(MediaKind.IMAGE, MediaKind.IMAGE, MediaKind.IMAGE))
        )
    }

    @Test
    fun `mime type resolves to video for video only attachments`() {
        assertEquals("video/*", MediaAttachmentPolicy.mimeTypeFor(listOf(MediaKind.VIDEO)))
        assertEquals(
            "video/*",
            MediaAttachmentPolicy.mimeTypeFor(listOf(MediaKind.VIDEO, MediaKind.VIDEO))
        )
    }

    @Test
    fun `mime type generalizes to wildcard for mixed image and video attachments`() {
        assertEquals(
            "*/*",
            MediaAttachmentPolicy.mimeTypeFor(listOf(MediaKind.IMAGE, MediaKind.VIDEO))
        )
        assertEquals(
            "*/*",
            MediaAttachmentPolicy.mimeTypeFor(listOf(MediaKind.VIDEO, MediaKind.IMAGE))
        )
        assertEquals(
            "*/*",
            MediaAttachmentPolicy.mimeTypeFor(
                listOf(MediaKind.IMAGE, MediaKind.VIDEO, MediaKind.IMAGE)
            )
        )
    }

    @Test
    fun `mime type fallback for empty list is image`() {
        assertEquals("image/*", MediaAttachmentPolicy.mimeTypeFor(emptyList()))
    }

    @Test
    fun `composer images are bounded for storage and display decoding`() {
        val policy = ComposerImagePolicy()
        assertEquals(4_096, policy.maximumLongEdgePixels)
        assertEquals(8_000_000, policy.maximumBytes)
        assertEquals(8, ComposerImagePipeline.sampleSizeFor(12_000, 9_000, 1_400))
        assertEquals(32, ComposerImagePipeline.sampleSizeFor(12_000, 9_000, 320))
    }
}
