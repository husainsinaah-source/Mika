package com.wangzan101.mika.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MikaWakePhraseTest {
    @Test fun attachedCommand() {
        assertEquals("open YouTube", MikaWakePhrase.parse("Hey Mika, open YouTube", false)?.command)
        assertEquals("set an alarm", MikaWakePhrase.parse("Mika set an alarm", false)?.command)
    }

    @Test fun wakeOnly() {
        assertNull(MikaWakePhrase.parse("Hey Mika", false)?.command)
        assertNull(MikaWakePhrase.parse("Mika!", false)?.command)
    }

    @Test fun followUpAndNoFalseActivation() {
        assertNull(MikaWakePhrase.parse("Open YouTube", false))
        assertEquals("Open YouTube", MikaWakePhrase.parse("Open YouTube", true)?.command)
        assertNull(MikaWakePhrase.parse("Amika, open YouTube", false))
    }
}
