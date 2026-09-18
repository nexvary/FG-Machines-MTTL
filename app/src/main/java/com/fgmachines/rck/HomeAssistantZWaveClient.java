package com.fgmachines.rck;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Local/private Home Assistant client used as a Z-Wave gateway transport.
 *
 * The phone does not implement a Z-Wave radio. Instead, it sends authenticated
 * Home Assistant service calls to a local/private gateway that owns the Z-Wave
 * controller. Public plain HTTP is blocked by EndpointSecurity.
 */
public final class HomeAssistantZWaveClient {
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 7000;

    private final String baseUrl;
    private final String token;

    public HomeAssistantZWaveClient(String baseUrl, String token) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        this.baseUrl = normalized;
        this.token = token == null ? "" : token.trim();
    }

    public JSONObject health() throws IOException {
        return request("GET", "/api/", null);
    }

    public List<SwitchEntity> listSwitches() throws IOException {
        Object response = requestAny("GET", "/api/states", null);
        if (!(response instanceof JSONArray)) {
            throw new IOException("Home Assistant states response is not an array");
        }
        JSONArray states = (JSONArray) response;
        List<SwitchEntity> out = new ArrayList<>();
        for (int i = 0; i < states.length(); i++) {
            JSONObject item = states.optJSONObject(i);
            if (item == null) continue;
            String entityId = item.optString("entity_id", "");
            if (!entityId.startsWith("switch.")) continue;
            JSONObject attributes = item.optJSONObject("attributes");
            String friendly = attributes == null ? "" : attributes.optString("friendly_name", "");
            String state = item.optString("state", "");
            out.add(new SwitchEntity(entityId, friendly, state, dawOnScore(entityId, friendly)));
        }
        Collections.sort(out, Comparator
                .comparingInt((SwitchEntity e) -> e.matchScore).reversed()
                .thenComparing(e -> e.displayName().toLowerCase(Locale.US)));
        return out;
    }

    public EntityState getState(String entityId) throws IOException {
        String safe = requireEntityId(entityId);
        JSONObject item = request("GET", "/api/states/" + safe, null);
        JSONObject attributes = item.optJSONObject("attributes");
        return new EntityState(
                item.optString("entity_id", safe),
                item.optString("state", "unknown"),
                attributes == null ? "" : attributes.optString("friendly_name", "")
        );
    }

    public void setSwitch(String entityId, boolean on) throws IOException {
        String safe = requireSwitchEntity(entityId);
        JSONObject body = new JSONObject();
        try {
            body.put("entity_id", safe);
        } catch (Exception error) {
            throw new IOException("Could not create Home Assistant request", error);
        }
        requestAny("POST", "/api/services/switch/" + (on ? "turn_on" : "turn_off"), body);
    }

    public static boolean isValidEntityId(String entityId) {
        if (entityId == null) return false;
        return entityId.trim().matches("[a-z0-9_]+\\.[a-z0-9_]+");
    }

    public static boolean isSwitchEntity(String entityId) {
        return isValidEntityId(entityId) && entityId.trim().startsWith("switch.");
    }

    public static int dawOnScore(String entityId, String friendlyName) {
        String haystack = ((entityId == null ? "" : entityId) + " "
                + (friendlyName == null ? "" : friendlyName)).toLowerCase(Locale.US);
        int score = 0;
        if (haystack.contains("mtd_01") || haystack.contains("mtd-01")) score += 100;
        if (haystack.contains("pm_m130") || haystack.contains("pm-m130")) score += 100;
        if (haystack.contains("dawon")) score += 80;
        if (haystack.contains("power_manager") || haystack.contains("power manager")) score += 40;
        if (haystack.contains("lg_uplus") || haystack.contains("lg uplus")) score += 20;
        return score;
    }

    private String requireEntityId(String entityId) throws IOException {
        if (!isValidEntityId(entityId)) throw new IOException("Invalid Home Assistant entity ID");
        return entityId.trim();
    }

    private String requireSwitchEntity(String entityId) throws IOException {
        if (!isSwitchEntity(entityId)) throw new IOException("Entity must be a switch.* entity");
        return entityId.trim();
    }

    private JSONObject request(String method, String path, JSONObject body) throws IOException {
        Object value = requestAny(method, path, body);
        if (!(value instanceof JSONObject)) {
            throw new IOException("Home Assistant returned unexpected JSON");
        }
        return (JSONObject) value;
    }

    private Object requestAny(String method, String path, JSONObject body) throws IOException {
        EndpointSecurity.validateRemoteEndpoint(baseUrl);
        if (token.isEmpty()) throw new IOException("Home Assistant token is empty");

        URL url = new URL(baseUrl + path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setDoInput(true);

        if (body != null) {
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            connection.getOutputStream().write(bytes);
        }

        int code = connection.getResponseCode();
        java.io.InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder response = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
        }
        connection.disconnect();

        if (code < 200 || code >= 300) {
            throw new IOException("Home Assistant HTTP " + code + ": " + response);
        }

        String text = response.toString().trim();
        if (text.isEmpty()) return new JSONObject();
        try {
            if (text.startsWith("[")) return new JSONArray(text);
            return new JSONObject(text);
        } catch (Exception error) {
            throw new IOException("Home Assistant returned invalid JSON", error);
        }
    }

    public static final class SwitchEntity {
        public final String entityId;
        public final String friendlyName;
        public final String state;
        public final int matchScore;

        SwitchEntity(String entityId, String friendlyName, String state, int matchScore) {
            this.entityId = entityId;
            this.friendlyName = friendlyName == null ? "" : friendlyName;
            this.state = state == null ? "" : state;
            this.matchScore = matchScore;
        }

        public String displayName() {
            String label = friendlyName.isEmpty() ? entityId : friendlyName;
            return label + " · " + state.toUpperCase(Locale.US);
        }

        @Override public String toString() {
            return displayName();
        }
    }

    public static final class EntityState {
        public final String entityId;
        public final String state;
        public final String friendlyName;

        EntityState(String entityId, String state, String friendlyName) {
            this.entityId = entityId;
            this.state = state;
            this.friendlyName = friendlyName == null ? "" : friendlyName;
        }

        public boolean isOn() {
            return "on".equalsIgnoreCase(state);
        }
    }
}
