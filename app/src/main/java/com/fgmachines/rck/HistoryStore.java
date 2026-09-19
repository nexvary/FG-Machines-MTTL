package com.fgmachines.rck;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** Local SQLite telemetry/event history. No cloud account is required. */
public final class HistoryStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "fg_rck_history.db";
    private static final int DB_VERSION = 2;
    private static final long SAMPLE_INTERVAL_MS = 30_000L;
    private static final long RETENTION_MS = 90L * 24L * 60L * 60L * 1000L;
    private static final long PRUNE_INTERVAL_MS = 6L * 60L * 60L * 1000L;
    static final long RUNTIME_MAX_GAP_MS = 120_000L;
    private volatile long lastPruneAt;

    public HistoryStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE telemetry (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ts INTEGER NOT NULL," +
                "mac TEXT NOT NULL," +
                "power_w REAL NOT NULL," +
                "energy_kwh REAL NOT NULL," +
                "max_temp_c INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_telemetry_mac_ts ON telemetry(mac, ts)");
        db.execSQL("CREATE TABLE events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ts INTEGER NOT NULL," +
                "mac TEXT NOT NULL," +
                "outlet INTEGER NOT NULL DEFAULT 0," +
                "kind TEXT NOT NULL," +
                "detail TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX idx_events_mac_ts ON events(mac, ts)");
        createOutletSamplesTable(db);
    }

    private static void createOutletSamplesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS outlet_samples (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ts INTEGER NOT NULL," +
                "mac TEXT NOT NULL," +
                "outlet INTEGER NOT NULL," +
                "relay_on INTEGER NOT NULL," +
                "power_w REAL NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_outlet_samples_mac_outlet_ts " +
                "ON outlet_samples(mac, outlet, ts)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createOutletSamplesTable(db);
    }

    public synchronized void recordTelemetry(String mac, MttlProtocol.Telemetry telemetry, long now) {
        if (mac == null || telemetry == null) return;
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty()) return;

        SQLiteDatabase db = getWritableDatabase();
        pruneIfNeeded(db, now);
        long last = 0L;
        try (Cursor cursor = db.rawQuery(
                "SELECT ts FROM telemetry WHERE mac=? ORDER BY ts DESC LIMIT 1",
                new String[]{key})) {
            if (cursor.moveToFirst()) last = cursor.getLong(0);
        }
        if (now - last < SAMPLE_INTERVAL_MS) return;

        double power = 0.0;
        double energy = 0.0;
        int maxTemp = Integer.MIN_VALUE;
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            power += Math.max(0.0, outlet.powerW);
            energy += Math.max(0.0, outlet.energyKWh);
            maxTemp = Math.max(maxTemp, outlet.temperatureC);
        }
        if (maxTemp == Integer.MIN_VALUE) maxTemp = 0;

        android.content.ContentValues values = new android.content.ContentValues();
        values.put("ts", now);
        values.put("mac", key);
        values.put("power_w", power);
        values.put("energy_kwh", energy);
        values.put("max_temp_c", maxTemp);
        db.insert("telemetry", null, values);

        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            if (outlet.channel < 1 || outlet.channel > 4) continue;
            android.content.ContentValues sample = new android.content.ContentValues();
            sample.put("ts", now);
            sample.put("mac", key);
            sample.put("outlet", outlet.channel);
            sample.put("relay_on", outlet.relayOn ? 1 : 0);
            sample.put("power_w", Math.max(0.0, outlet.powerW));
            db.insert("outlet_samples", null, sample);
        }
    }

    private void pruneIfNeeded(SQLiteDatabase db, long now) {
        if (now - lastPruneAt < PRUNE_INTERVAL_MS) return;
        long cutoff = now - RETENTION_MS;
        db.delete("telemetry", "ts<?", new String[]{String.valueOf(cutoff)});
        db.delete("events", "ts<?", new String[]{String.valueOf(cutoff)});
        db.delete("outlet_samples", "ts<?", new String[]{String.valueOf(cutoff)});
        lastPruneAt = now;
    }

    public synchronized void recordEvent(String mac, int outlet, String kind, String detail, long now) {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty() || kind == null || kind.trim().isEmpty()) return;
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("ts", now);
        values.put("mac", key);
        values.put("outlet", outlet);
        values.put("kind", kind.trim());
        values.put("detail", detail == null ? "" : detail);
        getWritableDatabase().insert("events", null, values);
    }

    public Summary summary(String mac, long since) {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty()) return new Summary(0, 0, 0, 0);
        SQLiteDatabase db = getReadableDatabase();
        double minEnergy = 0;
        double maxEnergy = 0;
        double avgPower = 0;
        double maxPower = 0;
        try (Cursor c = db.rawQuery(
                "SELECT MIN(energy_kwh), MAX(energy_kwh), AVG(power_w), MAX(power_w) " +
                        "FROM telemetry WHERE mac=? AND ts>=?",
                new String[]{key, String.valueOf(since)})) {
            if (c.moveToFirst() && !c.isNull(0)) {
                minEnergy = c.getDouble(0);
                maxEnergy = c.getDouble(1);
                avgPower = c.getDouble(2);
                maxPower = c.getDouble(3);
            }
        }
        return new Summary(Math.max(0.0, maxEnergy - minEnergy), avgPower, maxPower, maxEnergy);
    }

    public List<PowerPoint> recentPower(String mac, int limit) {
        String key = FleetStore.normalizeMac(mac);
        List<PowerPoint> points = new ArrayList<>();
        if (key.isEmpty()) return points;
        int safeLimit = Math.max(2, Math.min(240, limit));
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ts,power_w FROM telemetry WHERE mac=? ORDER BY ts DESC LIMIT " + safeLimit,
                new String[]{key})) {
            while (c.moveToNext()) points.add(0, new PowerPoint(c.getLong(0), c.getDouble(1)));
        }
        return points;
    }

    public RuntimeSummary runtimeSummary(String mac, int outlet, long since, long now) {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty() || outlet < 1 || outlet > 4 || now <= since) {
            return new RuntimeSummary(0L, false, 0);
        }
        List<RuntimePoint> points = new ArrayList<>();
        long queryStart = Math.max(0L, since - RUNTIME_MAX_GAP_MS);
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ts,relay_on FROM outlet_samples " +
                        "WHERE mac=? AND outlet=? AND ts>=? AND ts<=? ORDER BY ts ASC",
                new String[]{key, String.valueOf(outlet), String.valueOf(queryStart), String.valueOf(now)})) {
            while (c.moveToNext()) {
                points.add(new RuntimePoint(c.getLong(0), c.getInt(1) != 0));
            }
        }
        long runtimeMs = calculateRuntimeMs(points, since, now);
        boolean currentOn = !points.isEmpty()
                && points.get(points.size() - 1).relayOn
                && now - points.get(points.size() - 1).ts <= RUNTIME_MAX_GAP_MS;
        return new RuntimeSummary(runtimeMs, currentOn, points.size());
    }

    static long calculateRuntimeMs(List<RuntimePoint> points, long since, long now) {
        if (points == null || points.isEmpty() || now <= since) return 0L;
        long total = 0L;
        RuntimePoint previous = null;
        for (RuntimePoint current : points) {
            if (previous != null) {
                long gap = current.ts - previous.ts;
                if (gap >= 0L && gap <= RUNTIME_MAX_GAP_MS && previous.relayOn) {
                    long start = Math.max(previous.ts, since);
                    long end = Math.min(current.ts, now);
                    if (end > start) total += end - start;
                }
            }
            previous = current;
        }
        if (previous != null && previous.relayOn) {
            long gapToNow = now - previous.ts;
            if (gapToNow >= 0L && gapToNow <= RUNTIME_MAX_GAP_MS) {
                long start = Math.max(previous.ts, since);
                if (now > start) total += now - start;
            }
        }
        return Math.max(0L, total);
    }

    public List<EventRecord> recentEvents(String mac, int limit) {
        String key = FleetStore.normalizeMac(mac);
        List<EventRecord> events = new ArrayList<>();
        if (key.isEmpty()) return events;
        int safeLimit = Math.max(1, Math.min(100, limit));
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ts,outlet,kind,detail FROM events WHERE mac=? ORDER BY ts DESC LIMIT " + safeLimit,
                new String[]{key})) {
            while (c.moveToNext()) {
                events.add(new EventRecord(c.getLong(0), c.getInt(1), c.getString(2), c.getString(3)));
            }
        }
        return events;
    }

    public synchronized void writeCsv(String mac, Writer writer) throws IOException {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty()) throw new IOException("No device selected");

        writer.write("section,timestamp_ms,mac,outlet,kind,detail,power_w,energy_kwh,max_temp_c\n");
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ts,power_w,energy_kwh,max_temp_c FROM telemetry WHERE mac=? ORDER BY ts ASC",
                new String[]{key})) {
            while (c.moveToNext()) {
                writer.write("telemetry,");
                writer.write(String.valueOf(c.getLong(0)));
                writer.write(",");
                writer.write(csv(key));
                writer.write(",,,,");
                writer.write(String.valueOf(c.getDouble(1)));
                writer.write(",");
                writer.write(String.valueOf(c.getDouble(2)));
                writer.write(",");
                writer.write(String.valueOf(c.getInt(3)));
                writer.write("\n");
            }
        }
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ts,outlet,kind,detail FROM events WHERE mac=? ORDER BY ts ASC",
                new String[]{key})) {
            while (c.moveToNext()) {
                writer.write("event,");
                writer.write(String.valueOf(c.getLong(0)));
                writer.write(",");
                writer.write(csv(key));
                writer.write(",");
                writer.write(String.valueOf(c.getInt(1)));
                writer.write(",");
                writer.write(csv(c.getString(2)));
                writer.write(",");
                writer.write(csv(c.getString(3)));
                writer.write(",,,\n");
            }
        }
        writer.flush();
    }

    static String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public static final class Summary {
        public final double energyDeltaKWh;
        public final double averagePowerW;
        public final double maxPowerW;
        public final double latestEnergyKWh;

        Summary(double energyDeltaKWh, double averagePowerW, double maxPowerW, double latestEnergyKWh) {
            this.energyDeltaKWh = energyDeltaKWh;
            this.averagePowerW = averagePowerW;
            this.maxPowerW = maxPowerW;
            this.latestEnergyKWh = latestEnergyKWh;
        }
    }

    public static final class PowerPoint {
        public final long ts;
        public final double powerW;
        PowerPoint(long ts, double powerW) { this.ts = ts; this.powerW = powerW; }
    }

    public static final class RuntimeSummary {
        public final long runtimeMs;
        public final boolean currentOn;
        public final int sampleCount;

        RuntimeSummary(long runtimeMs, boolean currentOn, int sampleCount) {
            this.runtimeMs = runtimeMs;
            this.currentOn = currentOn;
            this.sampleCount = sampleCount;
        }
    }

    static final class RuntimePoint {
        final long ts;
        final boolean relayOn;

        RuntimePoint(long ts, boolean relayOn) {
            this.ts = ts;
            this.relayOn = relayOn;
        }
    }

    public static final class EventRecord {
        public final long ts;
        public final int outlet;
        public final String kind;
        public final String detail;
        EventRecord(long ts, int outlet, String kind, String detail) {
            this.ts = ts; this.outlet = outlet; this.kind = kind; this.detail = detail;
        }
    }
}
