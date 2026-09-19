package com.fgmachines.rck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShareCodeTest {
    @Test
    public void deviceShareCodeRoundTripsEndpointTokenAndScope() {
        String code = ShareCode.encode(
                "http://192.168.1.20:18086",
                "secret-token_123",
                AccessControlStore.Role.CONTROL,
                "88:D0:39:1C:0C:50");

        ShareCode.Profile profile = ShareCode.parse(code);
        assertEquals("http://192.168.1.20:18086", profile.endpoint);
        assertEquals("secret-token_123", profile.token);
        assertEquals(AccessControlStore.Role.CONTROL, profile.role);
        assertEquals("88D0391C0C50", profile.scopeMac);
        assertTrue(profile.isDeviceScoped());
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedShareCodeIsRejected() {
        ShareCode.parse("not-a-share-code");
    }
}
