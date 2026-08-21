package com.armsone.starmanager

import com.armsone.starmanager.ui.composer.MediaAttachmentPolicy
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
}
