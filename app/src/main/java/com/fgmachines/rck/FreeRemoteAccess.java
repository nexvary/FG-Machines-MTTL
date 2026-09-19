package com.fgmachines.rck;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

/**
 * Builds VPN-only remote endpoints for the free-access modes.
 * No public Internet port is opened and no VPS is required.
 */
final class FreeRemoteAccess {
    static final int MODE_PHONE_ZEROTIER = 0;
    static final int MODE_ROUTER_GATEWAY = 1;

    private FreeRemoteAccess() { }

    static String endpointForHost(String rawHost) throws IOException {
        if (rawHost == null || rawHost.trim().isEmpty()) {
            throw new IOException("Remote host is empty");
        }
        String value = rawHost.trim();
        final URL url;
        if (value.startsWith("http://") || value.startsWith("https://")) {
            try { url = new URL(value); }
            catch (Exception error) { throw new IOException("Remote host is invalid", error); }
            if (!"http".equalsIgnoreCase(url.getProtocol())) {
                throw new IOException("Free VPN mode uses a private HTTP endpoint");
            }
            if (url.getPort() != -1 && url.getPort() != LocalApiServer.PORT) {
                throw new IOException("Free VPN mode must use port " + LocalApiServer.PORT);
            }
            value = url.getHost();
        }

        String host = normalizeHost(value);
        String endpoint = host.contains(":")
                ? "http://[" + host + "]:" + LocalApiServer.PORT
                : "http://" + host + ":" + LocalApiServer.PORT;

        EndpointSecurity.validateRemoteEndpoint(endpoint);
        return endpoint;
    }

    static String modeLabel(int mode) {
        return mode == MODE_ROUTER_GATEWAY ? "router-gateway" : "phone-zerotier";
    }

    private static String normalizeHost(String value) throws IOException {
        String host = value == null ? "" : value.trim();
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            host = host.substring(1, host.length() - 1);
        }
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        if (host.isEmpty()) throw new IOException("Remote host is empty");

        final InetAddress[] addresses;
        try { addresses = InetAddress.getAllByName(host); }
        catch (Exception error) { throw new IOException("Remote host could not be resolved", error); }
        if (addresses.length == 0) throw new IOException("Remote host has no address");
        for (InetAddress address : addresses) {
            if (!EndpointSecurity.isTrustedPeer(address)) {
                throw new IOException("Use a private ZeroTier/LAN address, not a public Internet address");
            }
        }
        return host;
    }
}
