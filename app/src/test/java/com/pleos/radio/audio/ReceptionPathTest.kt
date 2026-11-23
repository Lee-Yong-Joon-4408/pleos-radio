package com.pleos.radio.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceptionPathTest {
    @Test fun audioStatusMapsToReceptionPath() {
        assertEquals(ReceptionPath.IP, AudioStatus.Live.receptionPath())
        assertEquals(ReceptionPath.BROADCAST, AudioStatus.Broadcast(fallback = true).receptionPath())
        assertEquals(ReceptionPath.BROADCAST, AudioStatus.Broadcast(fallback = false).receptionPath())
        assertEquals(ReceptionPath.BROADCAST, AudioStatus.Broadcast(fallback = false, viaIp = true).receptionPath())
        assertEquals(ReceptionPath.NONE, AudioStatus.Connecting.receptionPath())
        assertEquals(ReceptionPath.NONE, AudioStatus.Error("x").receptionPath())
        assertEquals(ReceptionPath.NONE, AudioStatus.Idle.receptionPath())
        // Offline (stream lost, fallback off): no reception animation.
        assertEquals(ReceptionPath.NONE, AudioStatus.Offline.receptionPath())
    }

    @Test fun handoverAlternatesIpAndRf() {
        assertEquals(ReceptionPath.IP, handoverPath(0))
        assertEquals(ReceptionPath.BROADCAST, handoverPath(1))
        assertEquals(ReceptionPath.IP, handoverPath(2))
        assertEquals(ReceptionPath.BROADCAST, AudioStatus.Handover(ReceptionPath.BROADCAST, 3).receptionPath())
        assertEquals(ReceptionPath.IP, AudioStatus.Handover(ReceptionPath.IP, 4).receptionPath())
    }
}
