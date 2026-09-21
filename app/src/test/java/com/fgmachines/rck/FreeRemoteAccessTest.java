package com.fgmachines.rck;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FreeRemoteAccessTest {
    @Test public void buildsPhoneZeroTierEndpoint() throws Exception {
        assertEquals("http://10.147.20.8:18086",
                FreeRemoteAccess.endpointForHost("10.147.20.8"));
    }

    @Test public void buildsRouterRoutedLanEndpoint() throws Exception {
        assertEquals("http://192.168.10.120:18086",
                FreeRemoteAccess.endpointForHost("192.168.10.120"));
    }

    @Test public void buildsRouterGatewayZeroTierEndpointWithoutManagedRoute() throws Exception {
        assertEquals("http://10.158.229.58:18086",
                FreeRemoteAccess.endpointForHost("10.158.229.58"));
    }

    @Test public void acceptsExistingPrivateEndpoint() throws Exception {
        assertEquals("http://100.90.8.7:18086",
                FreeRemoteAccess.endpointForHost("http://100.90.8.7:18086"));
    }

    @Test(expected = java.io.IOException.class)
    public void rejectsPublicHttpEndpoint() throws Exception {
        FreeRemoteAccess.endpointForHost("8.8.8.8");
    }

    @Test(expected = java.io.IOException.class)
    public void rejectsWrongPort() throws Exception {
        FreeRemoteAccess.endpointForHost("http://10.1.2.3:8080");
    }
}
