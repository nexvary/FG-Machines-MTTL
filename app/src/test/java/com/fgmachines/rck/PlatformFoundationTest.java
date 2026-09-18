package com.fgmachines.rck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
