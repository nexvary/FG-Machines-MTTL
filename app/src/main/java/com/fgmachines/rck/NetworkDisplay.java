package com.fgmachines.rck;

/** Presentation helpers for network addresses. Never used for routing or identity checks. */
final class NetworkDisplay {
    private NetworkDisplay() {}

    static String peerHost(String remoteAddress) {
        if (remoteAddress == null) return "";
        String value = remoteAddress.trim();
        while (value.startsWith("/")) value = value.substring(1);

        int slash = value.lastIndexOf('/');
        if (slash >= 0 && slash < value.length() - 1) value = value.substring(slash + 1);

        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            if (end > 1) return value.substring(1, end);
        }

        int firstColon = value.indexOf(':');
        int lastColon = value.lastIndexOf(':');
        if (firstColon > 0 && firstColon == lastColon) return value.substring(0, firstColon);
        return value;
    }
}
