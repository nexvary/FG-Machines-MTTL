package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Closeable;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Advanced local-only automations layered on top of the verified MTTL outlet commands.
 * No cloud service is required. The controller phone must remain connected to the strip.
 */
public final class SmartAutomationEngine implements Closeable {
    public static final String PREFS = LocalAutomationEngine.PREFS;
    public static final String KEY_AWAY_ENABLED = "smart_away_enabled_";
    public static final String KEY_AWAY_START = "smart_away_start_";
    public static final String KEY_AWAY_END = "smart_away_end_";
    public static final String KEY_FOLLOW_TARGET = "smart_follow_target_";
    public static final String KEY_FOLLOW_DELAY_SECONDS = "smart_follow_delay_seconds_";

    private static final long AWAY_MIN_MINUTES = 10L;
    private static final long AWAY_MAX_MINUTES = 45L;
    private static final int MAX_FOLLOW_DELAY_SECONDS = 3600;

    private final SharedPreferences prefs;
    private final ControllerHub hub;
    private final HistoryStore historyStore;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Boolean> lastRelayState = new ConcurrentHashMap<>();

    public SmartAutomationEngine(Context context, ControllerHub hub, HistoryStore historyStore) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.hub = hub;
        this.historyStore = historyStore;
    }

    public void start() {
        worker.scheduleAtFixedRate(this::runAwayModesSafely, 7, 15, TimeUnit.SECONDS);
    }

    public void onTelemetry(String mac, MttlProtocol.Telemetry telemetry) {
        if (mac == null || telemetry == null) return;
        worker.execute(() -> evaluateFollowRules(mac, telemetry));
    }

    private void evaluateFollowRules(String mac, MttlProtocol.Telemetry telemetry) {
        String normalized = FleetStore.normalizeMac(mac);
        if (normalized.isEmpty()) return;

        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            int source = outlet.channel;
            if (source < 1 || source > 4) continue;

            String relayKey = normalized + "_" + source;
            Boolean previous = lastRelayState.put(relayKey, outlet.relayOn);
            if (previous == null || previous == outlet.relayOn) continue;

            int target = prefs.getInt(LocalAutomationEngine.deviceKey(
                    KEY_FOLLOW_TARGET, normalized, source), 0);
            if (!isValidFollowTarget(source, target)) continue;

            int delaySeconds = clampFollowDelaySeconds(prefs.getInt(
                    LocalAutomationEngine.deviceKey(KEY_FOLLOW_DELAY_SECONDS, normalized, source), 0));
            boolean desiredState = outlet.relayOn;
            worker.schedule(() -> {
                try {
                    hub.setOutlet(normalized, target, desiredState);
                    if (historyStore != null) {
                        historyStore.recordEvent(normalized, target, "linked_outlet",
                                "source=" + source + ";state=" + (desiredState ? "on" : "off")
                                        + ";delay_s=" + delaySeconds,
                                System.currentTimeMillis());
                    }
                } catch (IOException ignored) {
                    if (historyStore != null) {
                        historyStore.recordEvent(normalized, target, "linked_outlet_failed",
                                "source=" + source + ";state=" + (desiredState ? "on" : "off"),
                                System.currentTimeMillis());
                    }
                }
            }, delaySeconds, TimeUnit.SECONDS);
        }
    }

    private void runAwayModesSafely() {
        try {
            runAwayModes();
        } catch (RuntimeException ignored) {
            // Malformed local preferences must not terminate the foreground controller.
        }
    }

    private void runAwayModes() {
        long nowMs = System.currentTimeMillis();
        LocalTime now = LocalTime.now();

        for (ControllerHub.DeviceState device : hub.connectedStates()) {
            if (device.telemetry == null) continue;
            String mac = device.mac;

            for (MttlProtocol.OutletTelemetry outlet : device.telemetry.outlets) {
                int channel = outlet.channel;
                if (channel < 1 || channel > 4) continue;

                boolean enabled = prefs.getBoolean(LocalAutomationEngine.deviceKey(
                        KEY_AWAY_ENABLED, mac, channel), false);
                String start = LocalAutomationEngine.normalizeTime(prefs.getString(
                        LocalAutomationEngine.deviceKey(KEY_AWAY_START, mac, channel), ""));
                String end = LocalAutomationEngine.normalizeTime(prefs.getString(
                        LocalAutomationEngine.deviceKey(KEY_AWAY_END, mac, channel), ""));
                String nextKey = awayNextPreferenceKey(mac, channel);

                if (!enabled || start.isEmpty() || end.isEmpty() || !isWithinWindow(now, start, end)) {
                    if (prefs.contains(nextKey)) prefs.edit().remove(nextKey).apply();
                    continue;
                }

                long nextAt = prefs.getLong(nextKey, 0L);
                if (nextAt <= 0L) {
                    prefs.edit().putLong(nextKey, nowMs + randomAwayDelayMs()).apply();
                    continue;
                }
                if (nowMs < nextAt) continue;

                boolean desired = !outlet.relayOn;
                try {
                    hub.setOutlet(mac, channel, desired);
                    prefs.edit().putLong(nextKey, nowMs + randomAwayDelayMs()).apply();
                    if (historyStore != null) {
                        historyStore.recordEvent(mac, channel, "away_mode",
                                desired ? "on" : "off", nowMs);
                    }
                } catch (IOException ignored) {
                    // Keep the due timestamp so the next pass retries after reconnect.
                }
            }
        }
    }

    private static long randomAwayDelayMs() {
        long minutes = ThreadLocalRandom.current().nextLong(
                AWAY_MIN_MINUTES, AWAY_MAX_MINUTES + 1L);
        return TimeUnit.MINUTES.toMillis(minutes);
    }

    public static boolean isWithinWindow(LocalTime now, String startValue, String endValue) {
        if (now == null) return false;
        String startText = LocalAutomationEngine.normalizeTime(startValue);
        String endText = LocalAutomationEngine.normalizeTime(endValue);
        if (startText.isEmpty() || endText.isEmpty()) return false;

        LocalTime start = LocalTime.parse(startText);
        LocalTime end = LocalTime.parse(endText);
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    public static boolean isValidFollowTarget(int source, int target) {
        return source >= 1 && source <= 4 && target >= 1 && target <= 4 && source != target;
    }

    public static int clampFollowDelaySeconds(int seconds) {
        return Math.max(0, Math.min(MAX_FOLLOW_DELAY_SECONDS, seconds));
    }

    public static String awayNextPreferenceKey(String mac, int channel) {
        return "smart_away_next_" + FleetStore.normalizeMac(mac) + "_" + channel;
    }

    @Override public void close() {
        worker.shutdownNow();
    }
}
