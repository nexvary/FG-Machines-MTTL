package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight local runtime statistics for each AC outlet.
 * Values are stored on-device only and survive controller restarts.
 */
public final class OutletRuntimeStore {
    private static final String PREFS = "fg_rck_runtime";
    private static final String KEY_KNOWN = "known_";
    private static final String KEY_ON = "on_";
    private static final String KEY_ON_SINCE = "on_since_";
    private static final String KEY_TOTAL_MS = "total_ms_";
    private static final String KEY_SWITCHES = "switches_";
    private static final String KEY_LAST_CHANGE = "last_change_";

    private final SharedPreferences prefs;

    public OutletRuntimeStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void recordState(String mac, int outlet, boolean on, long now) {
        if (outlet < 1 || outlet > 4) return;
        String suffix = suffix(mac, outlet);
        if (suffix.isEmpty()) return;

        boolean known = prefs.getBoolean(KEY_KNOWN + suffix, false);
        boolean previous = prefs.getBoolean(KEY_ON + suffix, false);
        SharedPreferences.Editor editor = prefs.edit();

        if (!known) {
            editor.putBoolean(KEY_KNOWN + suffix, true)
                    .putBoolean(KEY_ON + suffix, on)
                    .putLong(KEY_LAST_CHANGE + suffix, now);
            if (on) editor.putLong(KEY_ON_SINCE + suffix, now);
            editor.apply();
            return;
        }

        if (previous == on) {
            if (on && prefs.getLong(KEY_ON_SINCE + suffix, 0L) <= 0L) {
                editor.putLong(KEY_ON_SINCE + suffix, now).apply();
            }
            return;
        }

        long total = prefs.getLong(KEY_TOTAL_MS + suffix, 0L);
        if (previous) {
            long onSince = prefs.getLong(KEY_ON_SINCE + suffix, now);
            if (onSince > 0L && now > onSince) total += now - onSince;
        }

        editor.putBoolean(KEY_ON + suffix, on)
                .putLong(KEY_TOTAL_MS + suffix, Math.max(0L, total))
                .putInt(KEY_SWITCHES + suffix, prefs.getInt(KEY_SWITCHES + suffix, 0) + 1)
                .putLong(KEY_LAST_CHANGE + suffix, now);

        if (on) editor.putLong(KEY_ON_SINCE + suffix, now);
        else editor.remove(KEY_ON_SINCE + suffix);
        editor.apply();
    }

    public synchronized Snapshot snapshot(String mac, int outlet, long now) {
        if (outlet < 1 || outlet > 4) return Snapshot.empty(outlet);
        String suffix = suffix(mac, outlet);
        if (suffix.isEmpty()) return Snapshot.empty(outlet);

        boolean known = prefs.getBoolean(KEY_KNOWN + suffix, false);
        boolean on = prefs.getBoolean(KEY_ON + suffix, false);
        long total = prefs.getLong(KEY_TOTAL_MS + suffix, 0L);
        if (known && on) {
            long onSince = prefs.getLong(KEY_ON_SINCE + suffix, 0L);
            if (onSince > 0L && now > onSince) total += now - onSince;
        }
        return new Snapshot(
                outlet,
                known,
                on,
                Math.max(0L, total),
                Math.max(0, prefs.getInt(KEY_SWITCHES + suffix, 0)),
                prefs.getLong(KEY_LAST_CHANGE + suffix, 0L)
        );
    }

    public static String formatDuration(long millis) {
        long safe = Math.max(0L, millis);
        long hours = TimeUnit.MILLISECONDS.toHours(safe);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(safe) % 60L;
        if (hours >= 100L) return String.format(Locale.US, "%dh", hours);
        return String.format(Locale.US, "%dh %02dm", hours, minutes);
    }

    private static String suffix(String mac, int outlet) {
        String key = FleetStore.normalizeMac(mac);
        return key.isEmpty() ? "" : key + "_" + outlet;
    }

    public static final class Snapshot {
        public final int outlet;
        public final boolean known;
        public final boolean on;
        public final long runtimeMs;
        public final int switchCount;
        public final long lastChangeAt;

        Snapshot(int outlet, boolean known, boolean on, long runtimeMs,
                 int switchCount, long lastChangeAt) {
            this.outlet = outlet;
            this.known = known;
            this.on = on;
            this.runtimeMs = runtimeMs;
            this.switchCount = switchCount;
            this.lastChangeAt = lastChangeAt;
        }

        static Snapshot empty(int outlet) {
            return new Snapshot(outlet, false, false, 0L, 0, 0L);
        }
    }
}
