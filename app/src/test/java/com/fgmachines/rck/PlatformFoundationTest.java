package com.fgmachines.rck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;

public class PlatformFoundationTest {
    @Test
    public void normalizesMacAddressesForFleetKeys() {
        assertEquals("88D0391C0C50", FleetStore.normalizeMac("88:D0:39:1C:0C:50"));
        assertEquals("88D0391C0C50", FleetStore.normalizeMac("88d0-391c-0c50"));
        assertEquals("", FleetStore.normalizeMac(null));
    }

    @Test
    public void accessRoleHierarchyIsMonotonic() {
        assertTrue(AccessControlStore.Role.ADMIN.allows(AccessControlStore.Role.CONTROL));
        assertTrue(AccessControlStore.Role.OWNER.allows(AccessControlStore.Role.ADMIN));
        assertTrue(AccessControlStore.Role.CONTROL.allows(AccessControlStore.Role.VIEW));
        assertFalse(AccessControlStore.Role.VIEW.allows(AccessControlStore.Role.CONTROL));
    }

    @Test
    public void localApiParsesOutletQuery() {
        LocalApiServer.ParsedTarget target =
                LocalApiServer.ParsedTarget.parse("/api/v1/devices/ABC/outlets/2?state=ON");
        assertEquals("/api/v1/devices/ABC/outlets/2", target.path);
        assertEquals("on", target.query.get("state"));
    }
    @Test
    public void trustedPeerBoundaryAcceptsLanAndPrivateVpnOnly() throws Exception {
        assertTrue(EndpointSecurity.isTrustedPeer(InetAddress.getByName("192.168.1.5")));
        assertTrue(EndpointSecurity.isTrustedPeer(InetAddress.getByName("10.43.167.72")));
        assertTrue(EndpointSecurity.isTrustedPeer(InetAddress.getByName("100.100.20.30")));
        assertFalse(EndpointSecurity.isTrustedPeer(InetAddress.getByName("8.8.8.8")));
    }

    @Test
    public void rejectsPlainHttpToPublicInternet() throws Exception {
        EndpointSecurity.validateRemoteEndpoint("http://192.168.1.20:18086");
        EndpointSecurity.validateRemoteEndpoint("https://example.com");
        boolean rejected = false;
        try {
            EndpointSecurity.validateRemoteEndpoint("http://8.8.8.8:18086");
        } catch (IOException expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    @Test
    public void csvEscapesQuotesAndCommas() {
        assertEquals("\"a,b\"", HistoryStore.csv("a,b"));
        assertEquals("\"a\"\"b\"", HistoryStore.csv("a\"b"));
    }
    @Test
    public void sceneMaskMapsToFourOutlets() {
        SceneStore.Scene scene = new SceneStore.Scene(
                "scene-1", "88D0391C0C50", "Night", 0b0101);
        assertTrue(scene.outletOn(1));
        assertFalse(scene.outletOn(2));
        assertTrue(scene.outletOn(3));
        assertFalse(scene.outletOn(4));
        assertEquals(2, scene.onCount());
    }
}
