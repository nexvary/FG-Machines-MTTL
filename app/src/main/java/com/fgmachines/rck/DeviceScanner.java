package com.fgmachines.rck;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class DeviceScanner {
    static final int MTTL_PORT = 30300;

    interface ScanCallback {
        void onComplete(List<String> hosts, String subnet);
    }

    interface ProbeCallback {
        void onResult(String host, boolean reachable, String detail);
    }

    private DeviceScanner() {}

    static void probe(String host, ProbeCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            boolean ok = isOpen(host, MTTL_PORT, 800);
            callback.onResult(host, ok, ok
                    ? "TCP 30300 accepted the connection"
                    : "No TCP response on port 30300");
        });
    }

    static void scanLocal24(ScanCallback callback) {
        String local = findLocalIpv4();
        if (local == null || !local.contains(".")) {
            callback.onComplete(Collections.emptyList(), "unknown");
            return;
        }

        String prefix = local.substring(0, local.lastIndexOf('.') + 1);
        List<String> hits = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(48);
        AtomicInteger remaining = new AtomicInteger(254);

        for (int i = 1; i <= 254; i++) {
            final String host = prefix + i;
            pool.execute(() -> {
                try {
                    if (isOpen(host, MTTL_PORT, 180)) {
                        hits.add(host);
                    }
                } finally {
                    if (remaining.decrementAndGet() == 0) {
                        pool.shutdown();
                        List<String> result = new ArrayList<>(hits);
                        Collections.sort(result);
                        callback.onComplete(result, prefix + "0/24");
                    }
                }
            });
        }
    }

    private static boolean isOpen(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String findLocalIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback()) continue;
                Enumeration<java.net.InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address
                            && !address.isLoopbackAddress()
                            && address.isSiteLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
