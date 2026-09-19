package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Closeable;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Local automation engine that runs inside the foreground controller service.
 * Automations remain local to the controller phone; the hotspot and strip must
 * remain connected for commands to be delivered.
 */
public final class LocalAutomationEngine implements Closeable {
    public static final String PREFS = "fg_rck_settings";
    public static final String KEY_AUTO_OFF_ENABLED = "automation_auto_off_enabled_";
    public static final String KEY_AUTO_OFF_MINUTES = "automation_auto_off_minutes_";
    public static final String KEY_SCHEDULE_ENABLED = "automation_schedule_enabled_";
    public static final String KEY_SCHEDULE_ON = "automation_schedule_on_";
    public static final String KEY_SCHEDULE_OFF = "automation_schedule_off_";
    public static final String KEY_SCHEDULE_DAY_MODE = "automation_schedule_day_mode_";
    public static final String KEY_POWER_LIMIT_ENABLED = "automation_power_limit_enabled_";
    public static final String KEY_POWER_LIMIT_W = "automation_power_limit_w_";

    public static final String KEY_IDLE_OFF_ENABLED = "automation_idle_off_enabled_";
    public static final String KEY_IDLE_OFF_W = "automation_idle_off_w_";
    public static final String KEY_IDLE_OFF_MINUTES = "automation_idle_off_minutes_";
    public static final String KEY_AWAY_ENABLED = "automation_away_enabled_";
    public static final String KEY_AWAY_START = "automation_away_start_";
    public static final String KEY_AWAY_END = "automation_away_end_";

    public static final int DAY_EVERY_DAY = 0;
    public static final int DAY_WEEKDAYS = 1;
    public static final int DAY_WEEKENDS = 2;

    public static final int AWAY_MIN_DELAY_MINUTES = 20;
    public static final int AWAY_MAX_DELAY_MINUTES = 60;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    private final SharedPreferences prefs;
    private final ControllerHub hub;
    private final HistoryStore historyStore;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

    public LocalAutomationEngine(Context context, ControllerHub hub) {
        this(context, hub, null);
    }

    public LocalAutomationEngine(Context context, ControllerHub hub, HistoryStore historyStore) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.hub = hub;
        this.historyStore = historyStore;
    }

    public void start() {
        worker.scheduleAtFixedRate(this::runSchedulesSafely, 5, 15, TimeUnit.SECONDS);
    }

    public void onTelemetry(String mac, MttlProtocol.Telemetry telemetry) {
        if (mac == null || telemetry == null) return;
        worker.execute(() -> {
            evaluatePowerLimits(mac, telemetry);
            evaluateAutoOff(mac, telemetry);
            evaluateIdleAutoOff(mac, telemetry);
        });
    }

    private void evaluatePowerLimits(String mac, MttlProtocol.Telemetry telemetry) {
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            int channel = outlet.channel;
            if (channel < 1 || channel > 4) continue;
            boolean enabled = prefs.getBoolean(deviceKey(KEY_POWER_LIMIT_ENABLED, mac, channel), false);
            int limitW = prefs.getInt(deviceKey(KEY_POWER_LIMIT_W, mac, channel), 0);
            String latchKey = "automation_power_latched_" + FleetStore.normalizeMac(mac) + "_" + channel;

            if (!enabled || limitW <= 0 || !outlet.relayOn || outlet.powerW < limitW) {
                if (prefs.getBoolean(latchKey, false)) prefs.edit().remove(latchKey).apply();
                continue;
            }
            if (prefs.getBoolean(latchKey, false)) continue;
            try {
                hub.setOutlet(mac, channel, false);
                prefs.edit().putBoolean(latchKey, true).apply();
                recordAutomationEvent(mac, channel, "power_cutoff",
                        String.format(Locale.US, "%.1fW >= %dW", outlet.powerW, limitW));
            } catch (IOException ignored) {
                // Retry on the next telemetry sample.
            }
        }
    }

    private void evaluateAutoOff(String mac, MttlProtocol.Telemetry telemetry) {
        long now = System.currentTimeMillis();
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            int channel = outlet.channel;
            if (channel < 1 || channel > 4) continue;
            String deadlineKey = deadlinePreferenceKey(mac, channel);
            boolean enabled = prefs.getBoolean(deviceKey(KEY_AUTO_OFF_ENABLED, mac, channel), false);
            int minutes = Math.max(1, prefs.getInt(deviceKey(KEY_AUTO_OFF_MINUTES, mac, channel), 30));

            if (!enabled || !outlet.relayOn) {
                if (prefs.contains(deadlineKey)) prefs.edit().remove(deadlineKey).apply();
                continue;
            }

            long deadline = prefs.getLong(deadlineKey, 0L);
            if (deadline <= 0L) {
                prefs.edit().putLong(deadlineKey, now + TimeUnit.MINUTES.toMillis(minutes)).apply();
                continue;
            }
            if (now < deadline) continue;

            try {
                hub.setOutlet(mac, channel, false);
                prefs.edit().remove(deadlineKey).apply();
                recordAutomationEvent(mac, channel, "auto_off", minutes + " min");
            } catch (IOException ignored) {
                // Keep the deadline so the next telemetry sample retries after reconnect.
            }
        }
    }

    private void evaluateIdleAutoOff(String mac, MttlProtocol.Telemetry telemetry) {
        long now = System.currentTimeMillis();
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            int channel = outlet.channel;
            if (channel < 1 || channel > 4) continue;

            String deadlineKey = idleDeadlinePreferenceKey(mac, channel);
            boolean enabled = prefs.getBoolean(deviceKey(KEY_IDLE_OFF_ENABLED, mac, channel), false);
            int thresholdW = Math.max(0, prefs.getInt(deviceKey(KEY_IDLE_OFF_W, mac, channel), 0));
            int minutes = Math.max(1, prefs.getInt(deviceKey(KEY_IDLE_OFF_MINUTES, mac, channel), 10));

            boolean idle = enabled && thresholdW > 0 && outlet.relayOn
                    && outlet.powerW >= 0.0 && outlet.powerW <= thresholdW;
            if (!idle) {
                if (prefs.contains(deadlineKey)) prefs.edit().remove(deadlineKey).apply();
                continue;
            }

            long deadline = prefs.getLong(deadlineKey, 0L);
            if (deadline <= 0L) {
                prefs.edit().putLong(deadlineKey, now + TimeUnit.MINUTES.toMillis(minutes)).apply();
                continue;
            }
            if (now < deadline) continue;

            try {
                hub.setOutlet(mac, channel, false);
                prefs.edit().remove(deadlineKey).apply();
                recordAutomationEvent(mac, channel, "idle_auto_off",
                        String.format(Locale.US, "<= %dW for %d min", thresholdW, minutes));
            } catch (IOException ignored) {
                // Keep the deadline for a later retry.
            }
        }
    }

    private void runSchedulesSafely() {
        try {
            runSchedules();
        } catch (RuntimeException ignored) {
            // A malformed local preference must never terminate the controller service.
        }
    }

    private void runSchedules() {
        LocalDateTime now = LocalDateTime.now();
        String minute = now.toLocalTime().format(TIME_FORMAT);
        LocalDate date = now.toLocalDate();

        for (ControllerHub.DeviceState device : hub.connectedStates()) {
            String mac = device.mac;
            for (int channel = 1; channel <= 4; channel++) {
                boolean scheduleEnabled = prefs.getBoolean(
                        deviceKey(KEY_SCHEDULE_ENABLED, mac, channel), false);
                if (scheduleEnabled) {
                    int dayMode = prefs.getInt(deviceKey(KEY_SCHEDULE_DAY_MODE, mac, channel), DAY_EVERY_DAY);
                    if (dayMatches(dayMode, now.getDayOfWeek())) {
                        String onTime = normalizeTime(prefs.getString(
                                deviceKey(KEY_SCHEDULE_ON, mac, channel), ""));
                        String offTime = normalizeTime(prefs.getString(
                                deviceKey(KEY_SCHEDULE_OFF, mac, channel), ""));
                        if (minute.equals(onTime)) triggerOnce(mac, channel, true, date, minute);
                        if (minute.equals(offTime)) triggerOnce(mac, channel, false, date, minute);
                    }
                    // Fixed schedules take precedence over Away Mode on the same outlet.
                    clearAwayRuntimeState(mac, channel);
                    continue;
                }
                runAwayMode(device, channel, minute);
            }
        }
    }

    private void runAwayMode(ControllerHub.DeviceState device, int channel, String minute) {
        String mac = device.mac;
        boolean enabled = prefs.getBoolean(deviceKey(KEY_AWAY_ENABLED, mac, channel), false);
        String start = normalizeTime(prefs.getString(awayWindowKey(KEY_AWAY_START, mac), ""));
        String end = normalizeTime(prefs.getString(awayWindowKey(KEY_AWAY_END, mac), ""));

        if (!enabled || !isValidAwayWindow(start, end)) {
            clearAwayRuntimeState(mac, channel);
            return;
        }
        if (!isWithinWindow(minute, start, end)) {
            finishAwayWindow(device, channel);
            return;
        }

        Boolean current = relayState(device, channel);
        if (current == null) return;

        long now = System.currentTimeMillis();
        String nextKey = awayNextPreferenceKey(mac, channel);
        long nextAt = prefs.getLong(nextKey, 0L);
        if (nextAt <= 0L) {
            prefs.edit().putLong(nextKey, now + randomAwayDelayMillis()).apply();
            return;
        }
        if (now < nextAt) return;

        boolean target = !current;
        try {
            hub.setOutlet(mac, channel, target);
            prefs.edit()
                    .putLong(nextKey, now + randomAwayDelayMillis())
                    .putBoolean(awayManagedPreferenceKey(mac, channel), true)
                    .apply();
            recordAutomationEvent(mac, channel, "away_toggle", target ? "on" : "off");
        } catch (IOException ignored) {
            // Retry on the next scheduler pass without moving the due time.
        }
    }

    private void finishAwayWindow(ControllerHub.DeviceState device, int channel) {
        String mac = device.mac;
        String managedKey = awayManagedPreferenceKey(mac, channel);
        if (!prefs.getBoolean(managedKey, false)) {
            clearAwayRuntimeState(mac, channel);
            return;
        }

        Boolean current = relayState(device, channel);
        if (Boolean.TRUE.equals(current)) {
            try {
                hub.setOutlet(mac, channel, false);
                recordAutomationEvent(mac, channel, "away_window_end", "off");
            } catch (IOException ignored) {
                return;
            }
        }
        clearAwayRuntimeState(mac, channel);
    }

    private void clearAwayRuntimeState(String mac, int channel) {
        prefs.edit()
                .remove(awayNextPreferenceKey(mac, channel))
                .remove(awayManagedPreferenceKey(mac, channel))
                .apply();
    }

    private static Boolean relayState(ControllerHub.DeviceState device, int channel) {
        if (device == null || device.telemetry == null) return null;
        for (MttlProtocol.OutletTelemetry outlet : device.telemetry.outlets) {
            if (outlet.channel == channel) return outlet.relayOn;
        }
        return null;
    }

    private void triggerOnce(String mac, int channel, boolean on, LocalDate date, String minute) {
        String action = on ? "on" : "off";
        String key = "automation_last_" + FleetStore.normalizeMac(mac)
                + "_" + channel + "_" + action;
        String stamp = date + "T" + minute;
        if (stamp.equals(prefs.getString(key, ""))) return;
        try {
            hub.setOutlet(mac, channel, on);
            prefs.edit().putString(key, stamp).apply();
            recordAutomationEvent(mac, channel, "schedule", action + "@" + minute);
        } catch (IOException ignored) {
            // Do not mark as completed: retry while the matching minute is still active.
        }
    }

    private void recordAutomationEvent(String mac, int channel, String kind, String detail) {
        if (historyStore != null) {
            historyStore.recordEvent(mac, channel, kind, detail, System.currentTimeMillis());
        }
    }

    private static boolean dayMatches(int mode, DayOfWeek day) {
        boolean weekend = day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
        if (mode == DAY_WEEKDAYS) return !weekend;
        if (mode == DAY_WEEKENDS) return weekend;
        return true;
    }

    public static boolean isValidTime(String value) {
        if (value == null || value.trim().isEmpty()) return true;
        return value.trim().matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$");
    }

    public static String normalizeTime(String value) {
        if (value == null || value.trim().isEmpty()) return "";
        String trimmed = value.trim();
        return isValidTime(trimmed) ? trimmed : "";
    }

    public static boolean isValidAwayWindow(String start, String end) {
        String safeStart = normalizeTime(start);
        String safeEnd = normalizeTime(end);
        return !safeStart.isEmpty() && !safeEnd.isEmpty() && !safeStart.equals(safeEnd);
    }

    public static boolean isWithinWindow(String now, String start, String end) {
        if (!isValidAwayWindow(start, end) || !isValidTime(now) || now == null || now.trim().isEmpty()) {
            return false;
        }
        try {
            LocalTime n = LocalTime.parse(now.trim(), TIME_FORMAT);
            LocalTime s = LocalTime.parse(start.trim(), TIME_FORMAT);
            LocalTime e = LocalTime.parse(end.trim(), TIME_FORMAT);
            if (s.isBefore(e)) return !n.isBefore(s) && n.isBefore(e);
            return !n.isBefore(s) || n.isBefore(e);
        } catch (RuntimeException error) {
            return false;
        }
    }

    static long randomAwayDelayMillis() {
        long min = TimeUnit.MINUTES.toMillis(AWAY_MIN_DELAY_MINUTES);
        long maxExclusive = TimeUnit.MINUTES.toMillis(AWAY_MAX_DELAY_MINUTES) + 1L;
        return ThreadLocalRandom.current().nextLong(min, maxExclusive);
    }

    public static String deviceKey(String base, String mac, int channel) {
        return base + FleetStore.normalizeMac(mac) + "_" + channel;
    }

    public static String awayWindowKey(String base, String mac) {
        return base + FleetStore.normalizeMac(mac);
    }

    public static String deadlinePreferenceKey(String mac, int channel) {
        if (mac == null) return "automation_deadline_unknown_" + channel;
        return "automation_deadline_" + mac.replace(":", "").replace("-", "") + "_" + channel;
    }

    public static String idleDeadlinePreferenceKey(String mac, int channel) {
        if (mac == null) return "automation_idle_deadline_unknown_" + channel;
        return "automation_idle_deadline_" + mac.replace(":", "").replace("-", "") + "_" + channel;
    }

    public static String awayNextPreferenceKey(String mac, int channel) {
        if (mac == null) return "automation_away_next_unknown_" + channel;
        return "automation_away_next_" + mac.replace(":", "").replace("-", "") + "_" + channel;
    }

    public static String awayManagedPreferenceKey(String mac, int channel) {
        if (mac == null) return "automation_away_managed_unknown_" + channel;
        return "automation_away_managed_" + mac.replace(":", "").replace("-", "") + "_" + channel;
    }

    @Override public void close() {
        worker.shutdownNow();
    }
}
