package com.fgmachines.rck;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Provisions a known MTTL setup AP without manufacturer cloud access.
 * Android presents its normal Wi-Fi approval dialog before joining the strip AP.
 */
public final class MttlProvisioner {
    private static final Pattern IPV4 = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"
    );

    public interface Callback {
        void onStatus(String status);
        void onComplete();
        void onError(String message, Throwable error);
    }

    private final Context context;
    private final ConnectivityManager connectivity;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private ConnectivityManager.NetworkCallback networkCallback;

    public MttlProvisioner(Context context) {
        this.context = context.getApplicationContext();
        this.connectivity = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isActive() {
        return active.get();
    }

    public void provision(String setupSsid, String homeSsid, String homePassword,
                          String controllerIp, Callback callback) {
        if (!active.compareAndSet(false, true)) {
            callback.onError("Provisioning is already running", null);
            return;
        }

        String setupPassword = ModelCatalog.setupPassword(setupSsid);
        if (setupPassword == null) {
            fail(callback, "Unsupported setup SSID. Expected TONLY_TAP_* or ONLY_TAP_*", null);
            return;
        }
        if (homeSsid == null || homeSsid.trim().isEmpty()) {
            fail(callback, "Home Wi-Fi SSID is required", null);
            return;
        }
        if (homeSsid.contains(":") || (homePassword != null && homePassword.contains(":"))) {
            fail(callback, "':' is not supported in SSID/password by the text provisioning dialect", null);
            return;
        }
        if (controllerIp == null || !IPV4.matcher(controllerIp.trim()).matches()) {
            fail(callback, "A valid controller IPv4 address is required", null);
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            fail(callback, "Automatic setup-AP connection requires Android 10 or newer", null);
            return;
        }

        callback.onStatus("Requesting Android Wi-Fi connection to " + setupSsid + "…");
        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(setupSsid.trim())
                .setWpa2Passphrase(setupPassword)
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                callback.onStatus("Connected to strip setup AP. Writing local controller settings…");
                worker.execute(() -> configure(network, homeSsid.trim(),
                        homePassword == null ? "" : homePassword,
                        controllerIp.trim(), callback));
            }

            @Override
            public void onUnavailable() {
                fail(callback, "Android could not connect to the strip setup AP", null);
            }
        };

        try {
            connectivity.requestNetwork(request, networkCallback, 45_000);
        } catch (SecurityException error) {
            fail(callback, "Nearby Wi-Fi permission is required", error);
        } catch (RuntimeException error) {
            fail(callback, "Could not start setup-AP connection", error);
        }
    }

    private void configure(Network network, String homeSsid, String homePassword,
                           String controllerIp, Callback callback) {
        try (Socket socket = network.getSocketFactory().createSocket()) {
            socket.connect(new InetSocketAddress(ModelCatalog.SETUP_ADDRESS, ModelCatalog.SETUP_PORT), 7_000);
            socket.setSoTimeout(3_500);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));

            send(writer, reader, "up:ip:" + controllerIp, true);
            callback.onStatus("Controller address accepted. Sending home Wi-Fi settings…");
            send(writer, reader, "up:connect:" + homeSsid + ":" + homePassword, true);
            callback.onStatus("Wi-Fi settings accepted. Rebooting strip…");
            send(writer, reader, "up:reboot:0", false);

            finishNetwork();
            active.set(false);
            callback.onComplete();
        } catch (Exception error) {
            fail(callback, "MTTL provisioning failed", error);
        }
    }

    private static void send(BufferedWriter writer, BufferedReader reader,
                             String command, boolean requireResponse) throws IOException {
        writer.write(command);
        writer.write("\r\n");
        writer.flush();
        if (requireResponse) {
            String response = reader.readLine();
            if (response == null) throw new IOException("Device closed the setup connection");
        } else {
            try {
                reader.readLine();
            } catch (IOException ignored) {
                // Reboot commonly closes the AP/socket before an acknowledgement is readable.
            }
        }
    }

    public void cancel() {
        finishNetwork();
        active.set(false);
    }

    public void close() {
        cancel();
        worker.shutdownNow();
    }

    private void fail(Callback callback, String message, Throwable error) {
        finishNetwork();
        active.set(false);
        callback.onError(message, error);
    }

    private void finishNetwork() {
        ConnectivityManager.NetworkCallback callback = networkCallback;
        networkCallback = null;
        if (callback != null) {
            try {
                connectivity.unregisterNetworkCallback(callback);
            } catch (RuntimeException ignored) { }
        }
    }
}
