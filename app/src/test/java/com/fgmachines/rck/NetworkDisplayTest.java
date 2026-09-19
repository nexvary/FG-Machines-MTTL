package com.fgmachines.rck;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NetworkDisplayTest {
    @Test public void stripsIpv4EphemeralPort() {
        assertEquals("192.168.1.100", NetworkDisplay.peerHost("/192.168.1.100:30888"));
        assertEquals("192.168.1.100", NetworkDisplay.peerHost("192.168.1.100:49152"));
    }

    @Test public void preservesHostWithoutPort() {
        assertEquals("192.168.1.100", NetworkDisplay.peerHost("192.168.1.100"));
        assertEquals("", NetworkDisplay.peerHost(null));
    }

    @Test public void handlesBracketedIpv6() {
        assertEquals("fd00::1234", NetworkDisplay.peerHost("[fd00::1234]:30888"));
    }
}
