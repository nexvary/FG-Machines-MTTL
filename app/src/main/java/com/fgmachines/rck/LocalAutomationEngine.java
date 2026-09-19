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
import java.util.ArrayList;
import java.util.List;
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
    public static final String KEY_STANDBY_ENABLED = "automation_standby_enabled_";
    public static final String KEY_STANDBY_W = "automation_standby_w_";
    public static final String KEY_STANDBY_MINUTES = "automation_standby_minutes_";

    public static final String KEY_AWAY_ENABLED = "automation_away_enabled_";
    public static final String KEY_AWAY_START = "automation_away_start_";
    public static final String KEY_AWAY_END = "automation_away_end_";
    public static final String KEY_AWAY_MIN_MINUTES = "automation_away_min_minutes_";
    public static final String KEY_AWAY_MAX_MINUTES = "automation_away_max_minutes_";
    public static final String KEY_AWAY_OUTLET_MASK = "automation_away_outlet_mask_";

    public static final int DAY_EVERY_DAY = 0;
    public static final int DAY_WEEKDAYS = 1;
    public static final int DAY_WEEKENDS = 2;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.US);

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
            evaluateStandbyCutoff(mac, telemetry);
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
                recordEvent(mac, channel, "automation_power_limit",
                        "cutoff_above_" + limitW + "W");
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
            String deadlineKey = deadlineKey(mac, channel);
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
                recordEvent(mac, channel, "automation_auto_off",
                        "after_" + minutes + "_minutes");
            } catch (IOException ignored) {
                // Keep the deadline so the next telemetry sample retries after reconnect.
            }
        }
    }

    private void evaluateStandbyCutoff(String mac, MttlProtocol.Telemetry telemetry) {
        long now = System.currentTimeMillis();
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            int channel = outlet.channel;
            if (channel < 1 || channel > 4) continue;

            boolean enabled = prefs.getBoolean(deviceKey(KEY_STANDBY_ENABLED, mac, channel), false);
            int thresholdW = Math.max(0, prefs.getInt(deviceKey(KEY_STANDBY_W, mac, channel), 0));
            int minutes = Math.max(1, prefs.getInt(deviceKey(KEY_STANDBY_MINUTES, mac, channel), 5));
            String deadlineKey = standbyDeadlinePreferenceKey(mac, channel);

            boolean belowThreshold = outlet.relayOn && thresholdW > 0
                    && Math.max(0.0, outlet.powerW) <= thresholdW;
            if (!enabled || !belowThreshold) {
                if (prefs.contains(deadlineKey)) prefs.edit().remove(deadlineKey).apply();
                continue;
            }

            long deadline = prefs.getLong(deadlineKey, 0L);
            if (deadline <= 0L) {
                prefs.edit().putLong(deadlineKey,
                        now + TimeUnit.MINUTES.toMillis(minutes)).apply();
                continue;
            }
            if (now < deadline) continue;

            try {
                hub.setOutlet(mac, channel, false);
                prefs.edit().remove(deadlineKey).apply();
                recordEvent(mac, channel, "automation_standby_cutoff",
                        "below_" + thresholdW + "W_for_" + minutes + "_minutes");
            } catch (IOException ignored) {
                // Retry while the standby condition remains true.
            }
        }
    }

    private void runSchedulesSafely() {
        try {
            runSchedules();
            runAwayMode();
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
                if (!prefs.getBoolean(deviceKey(KEY_SCHEDULE_ENABLED, mac, channel), false)) continue;
                int dayMode = prefs.getInt(deviceKey(KEY_SCHEDULE_DAY_MODE, mac, channel), DAY_EVERY_DAY);
                if (!dayMatches(dayMode, now.getDayOfWeek())) continue;

                String onTime = normalizeTime(prefs.getString(
                        deviceKey(KEY_SCHEDULE_ON, mac, channel), ""));
                String offTime = normalizeTime(prefs.getString(
                        deviceKey(KEY_SCHEDULE_OFF, mac, channel), ""));
                if (minute.equals(onTime)) triggerOnce(mac, channel, true, date, minute);
                if (minute.equals(offTime)) triggerOnce(mac, channel, false, date, minute);
            }
        }
    }

    private void runAwayMode() {
        long nowMs = System.currentTimeMillis();
        LocalTime now = LocalTime.now();

        for (ControllerHub.DeviceState device : hub.connectedStates()) {
            String mac = device.mac;
            String nextKey = awayNextPreferenceKey(mac);
            boolean enabled = prefs.getBoolean(deviceKey(KEY_AWAY_ENABLED, mac), false);
            String start = normalizeTime(prefs.getString(deviceKey(KEY_AWAY_START, mac), ""));
            String end = normalizeTime(prefs.getString(deviceKey(KEY_AWAY_END, mac), ""));
            int mask = prefs.getInt(deviceKey(KEY_AWAY_OUTLET_MASK, mac), 0) & 0x0F;

            if (!enabled || start.isEmpty() || end.isEmpty() || mask == 0
                    || !isWithinAwayWindow(start, end, now)) {
                if (prefs.contains(nextKey)) prefs.edit().remove(nextKey).apply();
                continue;
            }

            long due = prefs.getLong(nextKey, 0L);
            int minMinutes = Math.max(1,
                    prefs.getInt(deviceKey(KEY_AWAY_MIN_MINUTES, mac), 15));
            int maxMinutes = Math.max(minMinutes,
                    prefs.getInt(deviceKey(KEY_AWAY_MAX_MINUTES, mac), 45));

            if (due <= 0L) {
                scheduleNextAway(mac, nowMs, minMinutes, maxMinutes);
                continue;
            }
            if (nowMs < due || device.telemetry == null) continue;

            List<MttlProtocol.OutletTelemetry> candidates = new ArrayList<>();
            for (MttlProtocol.OutletTelemetry outlet : device.telemetry.outlets) {
                if (outlet.channel >= 1 && outlet.channel <= 4
                        && (mask & (1 << (outlet.channel - 1))) != 0) {
                    candidates.add(outlet);
                }
            }
            if (candidates.isEmpty()) continue;

            MttlProtocol.OutletTelemetry selected =
                    candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            boolean nextState = !selected.relayOn;
            try {
                hub.setOutlet(mac, selected.channel, nextState);
                recordEvent(mac, selected.channel, "automation_away_toggle",
                        nextState ? "on" : "off");
                scheduleNextAway(mac, nowMs, minMinutes, maxMinutes);
            } catch (IOException ignored) {
                // Leave the deadline in the past so a reconnect can retry.
            }
        }
    }

    private void scheduleNextAway(String mac, long nowMs, int minMinutes, int maxMinutes) {
        int delay = randomIntervalMinutes(minMinutes, maxMinutes);
        prefs.edit().putLong(awayNextPreferenceKey(mac),
                nowMs + TimeUnit.MINUTES.toMillis(delay)).apply();
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
            recordEvent(mac, channel, "automation_schedule", action + "_at_" + minute);
        } catch (IOException ignored) {
            // Do not mark as completed: retry while the matching minute is still active.
        }
    }

    private void recordEvent(String mac, int channel, String kind, String detail) {
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

    public static boolean isWithinAwayWindow(String startValue, String endValue, LocalTime now) {
        String startText = normalizeTime(startValue);
        String endText = normalizeTime(endValue);
        if (startText.isEmpty() || endText.isEmpty() || now == null) return false;
        LocalTime start = LocalTime.parse(startText, TIME_FORMAT);
        LocalTime end = LocalTime.parse(endText, TIME_FORMAT);
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    public static int randomIntervalMinutes(int minMinutes, int maxMinutes) {
        int safeMin = Math.max(1, minMinutes);
        int safeMax = Math.max(safeMin, maxMinutes);
        if (safeMin == safeMax) return safeMin;
        return ThreadLocalRandom.current().nextInt(safeMin, safeMax + 1);
    }

    public static String deviceKey(String base, String mac, int channel) {
        return base + FleetStore.normalizeMac(mac) + "_" + channel;
    }

    public static String deviceKey(String base, String mac) {
        return base + FleetStore.normalizeMac(mac);
    }

    public static String deadlinePreferenceKey(String mac, int channel) {
        if (mac == null) return "automation_deadline_unknown_" + channel;
        return "automation_deadline_" + mac.replace(":", "").replace("-", "") + "_" + channel;
    }

    public static String standbyDeadlinePreferenceKey(String mac, int channel) {
        if (mac == null) return "automation_standby_deadline_unknown_" + channel;
        return "automation_standby_deadline_" + mac.replace(":", "").replace("-", "") + "_" + channel;
    }

    public static String awayNextPreferenceKey(String mac) {
        if (mac == null) return "automation_away_next_unknown";
        return "automation_away_next_" + mac.replace(":", "").replace("-", "");
    }

    private static String deadlineKey(String mac, int channel) {
        return deadlinePreferenceKey(mac, channel);
    }

    @Override public void close() {
        worker.shutdownNow();
    }
}
