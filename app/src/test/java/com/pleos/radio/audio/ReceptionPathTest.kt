package com.pleos.radio.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceptionPathTest {
    @Test fun audioStatusMapsToReceptionPath() {
        assertEquals(ReceptionPath.IP, AudioStatus.Live.receptionPath())
        assertEquals(ReceptionPath.BROADCAST, AudioStatus.Broadcast(fallback = true).receptionPath())
        assertEquals(ReceptionPath.BROADCAST, AudioStatus.Broadcast(fallback = false).receptionPath())
        assertEquals(ReceptionPath.NONE, AudioStatus.Connecting.receptionPath())
        assertEquals(ReceptionPath.NONE, AudioStatus.Error("x").receptionPath())
        assertEquals(ReceptionPath.NONE, AudioStatus.Idle.receptionPath())
    }
}
