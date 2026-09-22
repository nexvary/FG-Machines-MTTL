package com.fgmachines.rck;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One-tap field diagnostics for LAN / ZeroTier / FG Link connectivity.
 *
 * The goal is to replace manual browser, ping and RouterOS trial-and-error with
 * a single report that says which transport and endpoint are actually usable.
 */
public final class NetworkDoctorActivity extends AppCompatActivity {
    private static final String PREFS = "fg_rck_settings";
    private static final String PREF_REMOTE_ENDPOINT = "remote_endpoint";
    private static final String PREF_FREE_REMOTE_HOST = "free_remote_host";
    private static final String PREF_DOCTOR_GATEWAY = "doctor_gateway";
    private static final String PREF_DOCTOR_CONTROLLER_ZT = "doctor_controller_zt";
    private static final String PREF_DOCTOR_CONTROLLER_LAN = "doctor_controller_lan";

    // Deployment defaults remain editable and are persisted after the first run.
    private static final String DEFAULT_GATEWAY = "10.158.229.58";
    private static final String DEFAULT_CONTROLLER_ZT = "10.158.229.228";
    private static final String DEFAULT_CONTROLLER_LAN = "192.168.1.104";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private TextInputEditText gatewayInput;
    private TextInputEditText controllerZtInput;
    private TextInputEditText controllerLanInput;
    private MaterialButton runButton;
    private MaterialButton copyButton;
    private MaterialButton applyBestButton;
    private TextView summaryView;
    private TextView reportView;

    private volatile String lastReport = "";
    private volatile String bestEndpoint = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_doctor);
        applyInsets();

        gatewayInput = findViewById(R.id.doctorGatewayInput);
        controllerZtInput = findViewById(R.id.doctorControllerZtInput);
        controllerLanInput = findViewById(R.id.doctorControllerLanInput);
        runButton = findViewById(R.id.doctorRunButton);
        copyButton = findViewById(R.id.doctorCopyButton);
        applyBestButton = findViewById(R.id.doctorApplyBestButton);
        summaryView = findViewById(R.id.doctorSummary);
        reportView = findViewById(R.id.doctorReport);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        gatewayInput.setText(prefs.getString(PREF_DOCTOR_GATEWAY, DEFAULT_GATEWAY));
        controllerZtInput.setText(prefs.getString(PREF_DOCTOR_CONTROLLER_ZT, DEFAULT_CONTROLLER_ZT));
        controllerLanInput.setText(prefs.getString(PREF_DOCTOR_CONTROLLER_LAN, DEFAULT_CONTROLLER_LAN));

        findViewById(R.id.doctorBackButton).setOnClickListener(v -> finish());
        runButton.setOnClickListener(v -> runDiagnostics());
        copyButton.setOnClickListener(v -> copyReport());
        applyBestButton.setOnClickListener(v -> applyBestEndpoint());

        copyButton.setEnabled(false);
        applyBestButton.setEnabled(false);

        String captureKey = getIntent().getStringExtra("fg_ui_capture_key");
        if (captureKey != null) UiGateCapture.capture(this, captureKey);
    }

    private void applyInsets() {
        android.view.View root = findViewById(R.id.doctorRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void runDiagnostics() {
        final String gateway = textOf(gatewayInput);
        final String configuredControllerZt = textOf(controllerZtInput);
        final String controllerLan = textOf(controllerLanInput);

        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_DOCTOR_GATEWAY, gateway)
                .putString(PREF_DOCTOR_CONTROLLER_ZT, configuredControllerZt)
                .putString(PREF_DOCTOR_CONTROLLER_LAN, controllerLan)
                .apply();

        runButton.setEnabled(false);
        copyButton.setEnabled(false);
        applyBestButton.setEnabled(false);
        summaryView.setText(R.string.network_doctor_running);
        reportView.setText(R.string.network_doctor_running);
        bestEndpoint = "";

        worker.execute(() -> {
            DiagnosticSnapshot snapshot = collectSnapshot();

            ControllerHub hub = ControllerHub.get(getApplicationContext());
            int connectedDevices = hub.connectedStates().size();
            boolean controllerRunning = hub.isRunning();

            String controllerZt = configuredControllerZt;
            boolean controllerIsThisPhone = controllerRunning && !snapshot.fgmIpv4.isEmpty();
            if (controllerIsThisPhone) {
                controllerZt = snapshot.fgmIpv4;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(PREF_DOCTOR_CONTROLLER_ZT, controllerZt)
                        .apply();
                final String detectedControllerZt = controllerZt;
                runOnUiThread(() -> controllerZtInput.setText(detectedControllerZt));
            }

            Probe localApi = probeHealth("Local API", "127.0.0.1");
            Probe controllerPort = probeTcp("MTTL controller", "127.0.0.1", ModelCatalog.CONTROLLER_PORT);
            Probe gatewayProbe = probeHealth("KT708 gateway", gateway);
            Probe controllerZtProbe = controllerIsThisPhone
                    ? Probe.skip("Controller ZeroTier",
                    "this controller owns " + controllerZt
                            + "; remote-peer reachability must be tested from another FGM ZeroTier device")
                    : probeHealth("Controller ZeroTier", controllerZt);
            Probe controllerLanProbe = probeHealth("Controller LAN", controllerLan);

            String recommendation;
            String chosen = "";
            if (controllerIsThisPhone && localApi.ok) {
                if (!snapshot.hasVpn || !snapshot.hasFgmAddress) {
                    recommendation = "CONTROLLER_LOCAL: controller/API are healthy, but no active FGM ZeroTier address is available.";
                } else if (gatewayProbe.ok) {
                    chosen = endpoint(gateway);
                    recommendation = "ROUTER_GATEWAY: controller/API are healthy and KT708 answers from this controller. Use " + chosen;
                } else {
                    recommendation = "CONTROLLER_READY: local controller/API are healthy. ZeroTier address auto-detected as "
                            + controllerZt
                            + ". A failed KT708 self-loop from the controller is not treated as proof that remote ZeroTier is broken; run Network Doctor on the remote FGM phone to validate the peer path.";
                }
            } else if (controllerZtProbe.ok) {
                chosen = endpoint(controllerZt);
                recommendation = "DIRECT_ZEROTIER: controller is reachable directly. Prefer " + chosen;
            } else if (gatewayProbe.ok) {
                chosen = endpoint(gateway);
                recommendation = "ROUTER_GATEWAY: KT708 path is reachable. Use " + chosen;
            } else if (controllerLanProbe.ok) {
                chosen = endpoint(controllerLan);
                recommendation = "LAN_ONLY: controller API works on LAN, but the tested remote ZeroTier paths did not answer.";
            } else if (!snapshot.hasVpn || !snapshot.hasFgmAddress) {
                recommendation = "ZEROTIER_ROUTE: no active FGM ZeroTier address was detected on this phone.";
            } else if (localApi.ok) {
                recommendation = "REMOTE_PATH: this app's local API works, but no configured remote target answered.";
            } else {
                recommendation = "CONTROLLER_API: no tested FG Link API endpoint answered on TCP 18086.";
            }

            StringBuilder out = new StringBuilder();
            out.append("FG LINK NETWORK DOCTOR\n");
            String versionName = "unknown";
            try {
                android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                if (info.versionName != null) versionName = info.versionName;
            } catch (Exception ignored) { }
            out.append("App: ").append(versionName)
                    .append(" | Android ").append(Build.VERSION.RELEASE)
                    .append(" | ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\n\n");
            out.append("TRANSPORT\n").append(snapshot.transportSummary).append("\n");
            out.append("VPN detected: ").append(snapshot.hasVpn ? "YES" : "NO").append("\n");
            out.append("FGM ZeroTier IP detected: ").append(snapshot.hasFgmAddress ? "YES" : "NO").append("\n");
            if (!snapshot.fgmIpv4.isEmpty()) {
                out.append("Detected FGM ZeroTier IPv4: ").append(snapshot.fgmIpv4).append("\n");
            }
            out.append("Addresses:\n").append(snapshot.addresses).append("\n");
            out.append("Networks:\n").append(snapshot.networks).append("\n");

            out.append("FG LINK SERVICES\n");
            out.append("Controller process: ").append(controllerRunning ? "RUNNING" : "NOT RUNNING").append("\n");
            out.append("Connected MTTL devices: ").append(connectedDevices).append("\n");
            appendProbe(out, localApi);
            appendProbe(out, controllerPort);
            appendProbe(out, gatewayProbe);
            appendProbe(out, controllerZtProbe);
            appendProbe(out, controllerLanProbe);

            String configured = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString(PREF_REMOTE_ENDPOINT, "");
            if (configured != null && !configured.trim().isEmpty()) {
                out.append("\nConfigured remote endpoint: ").append(configured.trim()).append("\n");
            }

            out.append("\nDIAGNOSIS\n").append(recommendation).append("\n");

            bestEndpoint = chosen;
            lastReport = out.toString();

            String finalRecommendation = recommendation;
            runOnUiThread(() -> {
                reportView.setText(lastReport);
                summaryView.setText(finalRecommendation);
                runButton.setEnabled(true);
                copyButton.setEnabled(true);
                applyBestButton.setEnabled(!bestEndpoint.isEmpty());
            });
        });
    }

    private DiagnosticSnapshot collectSnapshot() {
        boolean hasVpn = false;
        boolean hasFgm = false;
        String fgmIpv4 = "";
        StringBuilder networks = new StringBuilder();
        StringBuilder addresses = new StringBuilder();
        String transportSummary = "No active network";

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network active = cm.getActiveNetwork();
            NetworkCapabilities activeCaps = active == null ? null : cm.getNetworkCapabilities(active);
            if (activeCaps != null) transportSummary = describeCapabilities(activeCaps);

            Network[] all = cm.getAllNetworks();
            for (Network network : all) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                LinkProperties props = cm.getLinkProperties(network);
                if (caps == null) continue;
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) hasVpn = true;
                networks.append("- ").append(describeCapabilities(caps));
                if (props != null) {
                    networks.append(" | ").append(props.getInterfaceName());
                    for (LinkAddress address : props.getLinkAddresses()) {
                        String ip = address.getAddress().getHostAddress();
                        if (ip != null && ip.startsWith("10.158.229.")) {
                            hasFgm = true;
                            if (fgmIpv4.isEmpty()) fgmIpv4 = ip;
                        }
                        networks.append(" ").append(ip);
                    }
                }
                networks.append("\n");
            }
        }

        try {
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            if (enumeration != null) {
                List<NetworkInterface> interfaces = Collections.list(enumeration);
                for (NetworkInterface ni : interfaces) {
                    List<String> ips = new ArrayList<>();
                    Enumeration<InetAddress> raw = ni.getInetAddresses();
                    while (raw.hasMoreElements()) {
                        InetAddress address = raw.nextElement();
                        if (address.isLoopbackAddress()) continue;
                        String ip = address.getHostAddress();
                        if (ip == null) continue;
                        if (address instanceof Inet4Address || ip.contains(":")) ips.add(ip);
                        if (ip.startsWith("10.158.229.")) {
                            hasFgm = true;
                            if (fgmIpv4.isEmpty()) fgmIpv4 = ip;
                        }
                    }
                    if (!ips.isEmpty()) {
                        addresses.append("- ").append(ni.getName()).append(": ")
                                .append(android.text.TextUtils.join(", ", ips)).append("\n");
                    }
                }
            }
        } catch (Exception error) {
            addresses.append("- interface scan failed: ").append(safe(error)).append("\n");
        }

        if (addresses.length() == 0) addresses.append("- none detected\n");
        if (networks.length() == 0) networks.append("- none detected\n");
        return new DiagnosticSnapshot(hasVpn, hasFgm, fgmIpv4, transportSummary,
                addresses.toString(), networks.toString());
    }

    private static String describeCapabilities(NetworkCapabilities caps) {
        List<String> parts = new ArrayList<>();
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) parts.add("WIFI");
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) parts.add("CELLULAR");
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) parts.add("VPN");
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) parts.add("ETHERNET");
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) parts.add("INTERNET");
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) parts.add("VALIDATED");
        return parts.isEmpty() ? "UNKNOWN" : android.text.TextUtils.join("+", parts);
    }

    private Probe probeHealth(String label, String host) {
        if (host == null || host.trim().isEmpty()) return Probe.skip(label, "host is empty");
        long start = System.nanoTime();
        try {
            JSONObject json = new RemoteApiClient(endpoint(host), "").health();
            long ms = elapsedMs(start);
            if (!json.optBoolean("ok", false)) return Probe.fail(label, "HTTP answered but health ok=false", ms);
            return Probe.pass(label, "health OK", ms);
        } catch (IOException error) {
            return Probe.fail(label, safe(error), elapsedMs(start));
        }
    }

    private Probe probeTcp(String label, String host, int port) {
        long start = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), 2200);
            return Probe.pass(label, "TCP " + port + " open", elapsedMs(start));
        } catch (IOException error) {
            return Probe.fail(label, "TCP " + port + " " + safe(error), elapsedMs(start));
        }
    }

    private static String endpoint(String rawHost) {
        String host = rawHost == null ? "" : rawHost.trim();
        if (host.startsWith("http://") || host.startsWith("https://")) return host;
        if (host.contains(":") && !host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return "http://[" + host + "]:" + LocalApiServer.PORT;
        }
        return "http://" + host + ":" + LocalApiServer.PORT;
    }

    private static void appendProbe(StringBuilder out, Probe probe) {
        out.append(probe.ok ? "[PASS] " : probe.skipped ? "[SKIP] " : "[FAIL] ")
                .append(probe.label).append(" — ").append(probe.detail);
        if (probe.ms >= 0) out.append(" (").append(probe.ms).append(" ms)");
        out.append("\n");
    }

    private void copyReport() {
        if (lastReport.isEmpty()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("FG Link Network Doctor", lastReport));
            summaryView.setText(R.string.network_doctor_copied);
        }
    }

    private void applyBestEndpoint() {
        if (bestEndpoint.isEmpty()) {
            summaryView.setText(R.string.network_doctor_no_working_path);
            return;
        }
        String host = bestEndpoint;
        try {
            java.net.URL url = new java.net.URL(bestEndpoint);
            host = url.getHost();
        } catch (Exception ignored) { }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_REMOTE_ENDPOINT, bestEndpoint)
                .putString(PREF_FREE_REMOTE_HOST, host)
                .apply();
        summaryView.setText(getString(R.string.network_doctor_applied, bestEndpoint));
    }

    private static long elapsedMs(long start) {
        return Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
    }

    private static String safe(Throwable error) {
        if (error == null) return "unknown error";
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) message = error.getClass().getSimpleName();
        return message.replace('\n', ' ').trim();
    }

    private static String textOf(TextInputEditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private static final class DiagnosticSnapshot {
        final boolean hasVpn;
        final boolean hasFgmAddress;
        final String fgmIpv4;
        final String transportSummary;
        final String addresses;
        final String networks;

        DiagnosticSnapshot(boolean hasVpn, boolean hasFgmAddress, String fgmIpv4,
                           String transportSummary, String addresses, String networks) {
            this.hasVpn = hasVpn;
            this.hasFgmAddress = hasFgmAddress;
            this.fgmIpv4 = fgmIpv4 == null ? "" : fgmIpv4;
            this.transportSummary = transportSummary;
            this.addresses = addresses;
            this.networks = networks;
        }
    }

    private static final class Probe {
        final String label;
        final boolean ok;
        final boolean skipped;
        final String detail;
        final long ms;

        private Probe(String label, boolean ok, boolean skipped, String detail, long ms) {
            this.label = label;
            this.ok = ok;
            this.skipped = skipped;
            this.detail = detail;
            this.ms = ms;
        }

        static Probe pass(String label, String detail, long ms) {
            return new Probe(label, true, false, detail, ms);
        }

        static Probe fail(String label, String detail, long ms) {
            return new Probe(label, false, false, detail, ms);
        }

        static Probe skip(String label, String detail) {
            return new Probe(label, false, true, detail, -1);
        }
    }
}
