package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Passive USB protocol-discovery window.
 *
 * This class never sends protocol commands. It only records already-received
 * unknown frames while the user explicitly runs a short discovery session.
 */
public final class UsbDiscoveryStore {
    public static final long DEFAULT_WINDOW_MS = 90_000L;
    private static final String PREFS = "fg_rck_usb_discovery";
    private static final String KEY_MAC = "target_mac";
    private static final String KEY_STARTED = "started_at";
    private static final String KEY_UNTIL = "until";
    private static final String KEY_COUNT = "frame_count";
    private static final String KEY_LAST_FRAME = "last_frame";
    private static final String KEY_LAST_FRAME_TS = "last_frame_ts";
    private static final int MAX_FRAME_CHARS = 512;

    private final SharedPreferences prefs;

    public UsbDiscoveryStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void start(String mac, long now) {
        start(mac, now, DEFAULT_WINDOW_MS);
    }

    public synchronized void start(String mac, long now, long durationMs) {
        String key = FleetStore.normalizeMac(mac);
        long safeDuration = Math.max(10_000L, Math.min(5 * 60_000L, durationMs));
        prefs.edit()
                .putString(KEY_MAC, key)
                .putLong(KEY_STARTED, now)
                .putLong(KEY_UNTIL, now + safeDuration)
                .putInt(KEY_COUNT, 0)
                .putString(KEY_LAST_FRAME, "")
                .putLong(KEY_LAST_FRAME_TS, 0L)
                .apply();
    }

    public synchronized boolean recordUnknownFrame(String mac, String frame, long now) {
        if (!isActiveFor(mac, now)) return false;
        String safe = frame == null ? "" : frame.replace('', ' ').replace('
', ' ').trim();
        if (safe.isEmpty()) return false;
        if (safe.length() > MAX_FRAME_CHARS) safe = safe.substring(0, MAX_FRAME_CHARS);
        int count = prefs.getInt(KEY_COUNT, 0) + 1;
        prefs.edit()
                .putInt(KEY_COUNT, count)
                .putString(KEY_LAST_FRAME, safe)
                .putLong(KEY_LAST_FRAME_TS, now)
                .apply();
        return true;
    }

    public boolean isActiveFor(String mac, long now) {
        String expected = FleetStore.normalizeMac(prefs.getString(KEY_MAC, ""));
        String actual = FleetStore.normalizeMac(mac);
        long until = prefs.getLong(KEY_UNTIL, 0L);
        return !expected.isEmpty() && expected.equals(actual) && now <= until;
    }

    public Snapshot snapshot(String mac, long now) {
        String expected = FleetStore.normalizeMac(prefs.getString(KEY_MAC, ""));
        String actual = FleetStore.normalizeMac(mac);
        boolean sameDevice = !expected.isEmpty() && expected.equals(actual);
        long started = prefs.getLong(KEY_STARTED, 0L);
        long until = prefs.getLong(KEY_UNTIL, 0L);
        boolean active = sameDevice && now <= until;
        return new Snapshot(
                sameDevice,
                active,
                started,
                until,
                sameDevice ? prefs.getInt(KEY_COUNT, 0) : 0,
                sameDevice ? prefs.getString(KEY_LAST_FRAME, "") : "",
                sameDevice ? prefs.getLong(KEY_LAST_FRAME_TS, 0L) : 0L
        );
    }

    public static final class Snapshot {
        public final boolean sameDevice;
        public final boolean active;
        public final long startedAt;
        public final long until;
        public final int frameCount;
        public final String lastFrame;
        public final long lastFrameAt;

        Snapshot(boolean sameDevice, boolean active, long startedAt, long until,
                 int frameCount, String lastFrame, long lastFrameAt) {
            this.sameDevice = sameDevice;
            this.active = active;
            this.startedAt = startedAt;
            this.until = until;
            this.frameCount = frameCount;
            this.lastFrame = lastFrame == null ? "" : lastFrame;
            this.lastFrameAt = lastFrameAt;
        }

        public long remainingMs(long now) {
            return active ? Math.max(0L, until - now) : 0L;
        }
    }
}
