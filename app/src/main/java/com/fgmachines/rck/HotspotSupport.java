package com.fgmachines.rck;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;

/** Helpers for running FG Machines RCK with the phone acting as the local Wi-Fi AP/controller. */
final class HotspotSupport {
    private static final String ACTION_TETHER_SETTINGS = "android.settings.TETHER_SETTINGS";

    private HotspotSupport() { }

    static boolean supportsSamePhoneProvisioning(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false;
        WifiManager wifi = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        return wifi != null && wifi.isStaApConcurrencySupported();
    }

    /**
     * Best-effort discovery of the phone-side IPv4 address that a hotspot client can reach.
     * Prefers Wi-Fi/AP-looking interfaces and gateway-like .1 addresses, while excluding
     * cellular/VPN interfaces. The user can still override this value in the setup wizard.
     */
    static String findControllerIpv4() {
        Candidate best = null;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback()) continue;
                String name = network.getName() == null ? "" : network.getName().toLowerCase(Locale.US);
                if (isExcluded(name)) continue;

                Enumeration<java.net.InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) continue;
                    String host = address.getHostAddress();
                    if (host == null || !isPrivateIpv4(host)) continue;

                    int score = scoreInterface(name, host);
                    Candidate candidate = new Candidate(host, score);
                    if (best == null || candidate.score > best.score) best = candidate;
                }
            }
        } catch (Exception ignored) {
        }
        return best == null ? null : best.address;
    }

    static void openSystemHotspotSettings(Activity activity) {
        try {
            activity.startActivity(new Intent(ACTION_TETHER_SETTINGS));
        } catch (ActivityNotFoundException error) {
            activity.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        }
    }

    private static int scoreInterface(String name, String host) {
        int score = 0;
        if (name.startsWith("wlan") || name.startsWith("ap") || name.startsWith("swlan")
                || name.contains("wifi") || name.contains("softap")) score += 60;
        if (host.endsWith(".1")) score += 50;
        if (host.startsWith("192.168.")) score += 30;
        else if (host.startsWith("172.")) score += 20;
        else if (host.startsWith("10.")) score += 10;
        return score;
    }

    private static boolean isExcluded(String name) {
        return name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("pdp")
                || name.startsWith("tun") || name.startsWith("vpn") || name.startsWith("dummy")
                || name.startsWith("lo");
    }

    private static boolean isPrivateIpv4(String host) {
        if (host.startsWith("10.")) return true;
        if (host.startsWith("192.168.")) return true;
        if (!host.startsWith("172.")) return false;
        String[] parts = host.split("\\.");
        if (parts.length != 4) return false;
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException error) {
            return false;
        }
    }

    private static final class Candidate {
        final String address;
        final int score;

        Candidate(String address, int score) {
            this.address = address;
            this.score = score;
        }
    }
}
