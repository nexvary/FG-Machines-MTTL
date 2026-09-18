package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Closeable;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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

    public static final int DAY_EVERY_DAY = 0;
    public static final int DAY_WEEKDAYS = 1;
    public static final int DAY_WEEKENDS = 2;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    private final SharedPreferences prefs;
    private final ControllerHub hub;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();

    public LocalAutomationEngine(Context context, ControllerHub hub) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.hub = hub;
    }

    public void start() {
        worker.scheduleAtFixedRate(this::runSchedulesSafely, 5, 15, TimeUnit.SECONDS);
    }

    public void onTelemetry(String mac, MttlProtocol.Telemetry telemetry) {
        if (mac == null || telemetry == null) return;
        worker.execute(() -> {
            evaluatePowerLimits(mac, telemetry);
            evaluateAutoOff(mac, telemetry);
        });
    }

    private void evaluatePowerLimits(String mac, MttlProtocol.Telemetry telemetry) {
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            int channel = outlet.channel;
            if (channel < 1 || channel > 4) continue;
            boolean enabled = prefs.getBoolean(KEY_POWER_LIMIT_ENABLED + channel, false);
            int limitW = Math.max(1, prefs.getInt(KEY_POWER_LIMIT_W + channel, 0));
            String latchKey = "automation_power_latched_" + FleetStore.normalizeMac(mac) + "_" + channel;

            if (!enabled || !outlet.relayOn || outlet.powerW < limitW) {
                if (prefs.getBoolean(latchKey, false)) prefs.edit().remove(latchKey).apply();
                continue;
            }
            if (prefs.getBoolean(latchKey, false)) continue;
            try {
                hub.setOutlet(mac, channel, false);
                prefs.edit().putBoolean(latchKey, true).apply();
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
            boolean enabled = prefs.getBoolean(KEY_AUTO_OFF_ENABLED + channel, false);
            int minutes = Math.max(1, prefs.getInt(KEY_AUTO_OFF_MINUTES + channel, 30));

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
            } catch (IOException ignored) {
                // Keep the deadline so the next telemetry sample retries after reconnect.
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
        String mac = hub.activeMac();
        if (mac == null) return;

        LocalDateTime now = LocalDateTime.now();
        String minute = now.toLocalTime().format(TIME_FORMAT);
        LocalDate date = now.toLocalDate();

        for (int channel = 1; channel <= 4; channel++) {
            if (!prefs.getBoolean(KEY_SCHEDULE_ENABLED + channel, false)) continue;
            int dayMode = prefs.getInt(KEY_SCHEDULE_DAY_MODE + channel, DAY_EVERY_DAY);
            if (!dayMatches(dayMode, now.getDayOfWeek())) continue;

            String onTime = normalizeTime(prefs.getString(KEY_SCHEDULE_ON + channel, ""));
            String offTime = normalizeTime(prefs.getString(KEY_SCHEDULE_OFF + channel, ""));
            if (minute.equals(onTime)) triggerOnce(mac, channel, true, date, minute);
            if (minute.equals(offTime)) triggerOnce(mac, channel, false, date, minute);
        }
    }

    private void triggerOnce(String mac, int channel, boolean on, LocalDate date, String minute) {
        String action = on ? "on" : "off";
        String key = "automation_last_" + channel + "_" + action;
        String stamp = date + "T" + minute;
        if (stamp.equals(prefs.getString(key, ""))) return;
        try {
            hub.setOutlet(mac, channel, on);
            prefs.edit().putString(key, stamp).apply();
        } catch (IOException ignored) {
            // Do not mark as completed: retry while the matching minute is still active.
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

    public static String deadlinePreferenceKey(String mac, int channel) {
        if (mac == null) return "automation_deadline_unknown_" + channel;
        return "automation_deadline_" + mac.replace(":", "").replace("-", "") + "_" + channel;
    }

    private static String deadlineKey(String mac, int channel) {
        return deadlinePreferenceKey(mac, channel);
    }

    @Override public void close() {
        worker.shutdownNow();
    }
}
