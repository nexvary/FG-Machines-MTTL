package com.fgmachines.rck;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Portable Device Sharing payload.
 *
 * The code contains a bearer token and must be treated like a password. It is
 * intentionally text-only so it can be copied between two FG Machines RCK
 * phones without any cloud dependency.
 */
public final class ShareCode {
    private static final String PREFIX = "FGRCK1";
    private static final int MAX_CODE_LENGTH = 8192;

    private ShareCode() { }

    public static String encode(String endpoint, String token,
                                AccessControlStore.Role role, String scopeMac) {
        String safeEndpoint = endpoint == null ? "" : endpoint.trim();
        String safeToken = token == null ? "" : token.trim();
        if (safeEndpoint.isEmpty()) throw new IllegalArgumentException("Endpoint is required");
        if (safeToken.isEmpty()) throw new IllegalArgumentException("Token is required");
        AccessControlStore.Role safeRole = role == null
                ? AccessControlStore.Role.VIEW : role;
        String safeMac = FleetStore.normalizeMac(scopeMac);

        return PREFIX + "|" + enc(safeEndpoint)
                + "|" + enc(safeToken)
                + "|" + safeRole.name()
                + "|" + enc(safeMac);
    }

    public static Profile parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("Share code is empty");
        String code = raw.trim();
        if (code.isEmpty() || code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Share code is invalid");
        }
        String[] parts = code.split("\\|", -1);
        if (parts.length != 5 || !PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported share code");
        }

        String endpoint = dec(parts[1]).trim();
        String token = dec(parts[2]).trim();
        AccessControlStore.Role role;
        try {
            role = AccessControlStore.Role.valueOf(parts[3]);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid share role", error);
        }
        String scopeMac = FleetStore.normalizeMac(dec(parts[4]));

        if (endpoint.isEmpty() || token.isEmpty()) {
            throw new IllegalArgumentException("Incomplete share code");
        }
        return new Profile(endpoint, token, role, scopeMac);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String dec(String value) {
        return URLDecoder.decode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public static final class Profile {
        public final String endpoint;
        public final String token;
        public final AccessControlStore.Role role;
        public final String scopeMac;

        Profile(String endpoint, String token,
                AccessControlStore.Role role, String scopeMac) {
            this.endpoint = endpoint;
            this.token = token;
            this.role = role;
            this.scopeMac = scopeMac;
        }

        public boolean isDeviceScoped() {
            return scopeMac != null && !scopeMac.isEmpty();
        }
    }
}
