package com.fgmachines.rck;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional client for another FG Machines RCK controller exposed through a
 * trusted HTTPS endpoint or private VPN. Local control remains the default.
 */
public final class RemoteApiClient {
    private final String baseUrl;
    private final String token;

    public RemoteApiClient(String baseUrl, String token) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        this.baseUrl = normalized;
        this.token = token == null ? "" : token.trim();
    }

    public List<RemoteDevice> listDevices() throws IOException {
        JSONObject response = request("GET", "/api/v1/devices");
        JSONArray devices = response.optJSONArray("devices");
        List<RemoteDevice> out = new ArrayList<>();
        if (devices == null) return out;
        for (int i = 0; i < devices.length(); i++) {
            JSONObject item = devices.optJSONObject(i);
            if (item == null) continue;
            out.add(new RemoteDevice(
                    item.optString("mac"),
                    item.optString("name"),
                    item.optString("room"),
                    item.optBoolean("connected"),
                    item.optLong("last_seen")
            ));
        }
        return out;
    }

    public void setOutlet(String mac, int outlet, boolean on) throws IOException {
        if (outlet < 1 || outlet > 4) throw new IOException("Outlet must be 1..4");
        String safeMac = FleetStore.normalizeMac(mac);
        request("POST", "/api/v1/devices/" + safeMac + "/outlets/" + outlet
                + "?state=" + (on ? "on" : "off"));
    }

    public JSONObject history(String mac, int hours) throws IOException {
        return request("GET", "/api/v1/history/" + FleetStore.normalizeMac(mac)
                + "?hours=" + Math.max(1, hours));
    }

    public JSONObject health() throws IOException {
        return request("GET", "/api/v1/health");
    }

    private JSONObject request(String method, String path) throws IOException {
        if (baseUrl.isEmpty()) throw new IOException("Remote endpoint is empty");
        URL url = new URL(baseUrl + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("Accept", "application/json");
        if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setDoInput(true);
        if ("POST".equals(method)) {
            connection.setDoOutput(true);
            connection.getOutputStream().write(new byte[0]);
        }
        int code = connection.getResponseCode();
        java.io.InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder body = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
        }
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("Remote API HTTP " + code + ": " + body);
        }
        try {
            return new JSONObject(body.toString());
        } catch (Exception error) {
            throw new IOException("Remote API returned invalid JSON", error);
        }
    }

    public static final class RemoteDevice {
        public final String mac;
        public final String name;
        public final String room;
        public final boolean connected;
        public final long lastSeen;

        RemoteDevice(String mac, String name, String room, boolean connected, long lastSeen) {
            this.mac = mac;
            this.name = name;
            this.room = room;
            this.connected = connected;
            this.lastSeen = lastSeen;
        }

        @Override public String toString() {
            String label = name == null || name.isEmpty() ? mac : name;
            if (room != null && !room.isEmpty()) label += " · " + room;
            return label + (connected ? " · ONLINE" : " · OFFLINE");
        }
    }
}
