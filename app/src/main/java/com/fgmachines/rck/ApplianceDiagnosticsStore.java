package com.fgmachines.rck;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Local-only appliance bindings and diagnostic incident history. */
public final class ApplianceDiagnosticsStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "fg_rck_appliance_diagnostics.db";
    private static final int DB_VERSION = 2;

    public ApplianceDiagnosticsStore(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE appliance_binding (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "mac TEXT NOT NULL," +
                "outlet INTEGER NOT NULL," +
                "brand TEXT NOT NULL DEFAULT ''," +
                "category TEXT NOT NULL DEFAULT ''," +
                "model TEXT NOT NULL DEFAULT ''," +
                "nickname TEXT NOT NULL DEFAULT ''," +
                "updated_ts INTEGER NOT NULL," +
                "UNIQUE(mac,outlet))");
        db.execSQL("CREATE INDEX idx_appliance_binding_mac_outlet ON appliance_binding(mac,outlet)");

        db.execSQL("CREATE TABLE diagnostic_incident (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ts INTEGER NOT NULL," +
                "mac TEXT NOT NULL," +
                "outlet INTEGER NOT NULL," +
                "code TEXT NOT NULL DEFAULT ''," +
                "symptom TEXT NOT NULL DEFAULT ''," +
                "diagnosis TEXT NOT NULL DEFAULT ''," +
                "source_url TEXT NOT NULL DEFAULT ''," +
                "power_w REAL NOT NULL DEFAULT 0," +
                "energy_kwh REAL NOT NULL DEFAULT 0," +
                "rck_context TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX idx_diag_incident_mac_outlet_ts " +
                "ON diagnostic_incident(mac,outlet,ts)");
        createIdentityTable(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createIdentityTable(db);
    }

    private static void createIdentityTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS model_identity (" +
                "identifier TEXT PRIMARY KEY," +
                "brand TEXT NOT NULL DEFAULT ''," +
                "category TEXT NOT NULL DEFAULT ''," +
                "model TEXT NOT NULL DEFAULT ''," +
                "source TEXT NOT NULL DEFAULT ''," +
                "updated_ts INTEGER NOT NULL)");
    }

    public synchronized void bind(String mac, int outlet, String brand, String category,
                                  String model, String nickname, long now) {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty() || outlet < 1 || outlet > 4) return;
        ContentValues values = new ContentValues();
        values.put("mac", key);
        values.put("outlet", outlet);
        values.put("brand", clean(brand));
        values.put("category", clean(category));
        values.put("model", clean(model));
        values.put("nickname", clean(nickname));
        values.put("updated_ts", now);
        getWritableDatabase().insertWithOnConflict(
                "appliance_binding", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public BindingRecord binding(String mac, int outlet) {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty() || outlet < 1 || outlet > 4) return null;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT brand,category,model,nickname,updated_ts FROM appliance_binding " +
                        "WHERE mac=? AND outlet=? LIMIT 1",
                new String[]{key, String.valueOf(outlet)})) {
            if (!c.moveToFirst()) return null;
            return new BindingRecord(key, outlet, c.getString(0), c.getString(1),
                    c.getString(2), c.getString(3), c.getLong(4));
        }
    }

    public synchronized boolean saveIdentity(String identifier, String brand,
                                             String category, String model,
                                             String source, long now) {
        String key = normalizeIdentifier(identifier);
        String cleanModel = clean(model);
        if (key.isEmpty() || cleanModel.isEmpty()) return false;
        ContentValues values = new ContentValues();
        values.put("identifier", key);
        values.put("brand", clean(brand));
        values.put("category", clean(category));
        values.put("model", cleanModel);
        values.put("source", clean(source));
        values.put("updated_ts", now);
        return getWritableDatabase().insertWithOnConflict(
                "model_identity", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public IdentityRecord resolveIdentity(String identifier) {
        String key = normalizeIdentifier(identifier);
        if (key.isEmpty()) return null;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT brand,category,model,source,updated_ts FROM model_identity " +
                        "WHERE identifier=? LIMIT 1",
                new String[]{key})) {
            if (!c.moveToFirst()) return null;
            return new IdentityRecord(
                    key, c.getString(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getLong(4));
        }
    }

    public synchronized void recordIncident(String mac, int outlet, String code,
                                            String symptom, String diagnosis, String sourceUrl,
                                            double powerW, double energyKWh,
                                            String rckContext, long now) {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty() || outlet < 1 || outlet > 4) return;
        ContentValues values = new ContentValues();
        values.put("ts", now);
        values.put("mac", key);
        values.put("outlet", outlet);
        values.put("code", clean(code));
        values.put("symptom", clean(symptom));
        values.put("diagnosis", clean(diagnosis));
        values.put("source_url", clean(sourceUrl));
        values.put("power_w", powerW);
        values.put("energy_kwh", energyKWh);
        values.put("rck_context", clean(rckContext));
        getWritableDatabase().insert("diagnostic_incident", null, values);
    }

    public List<IncidentRecord> recent(String mac, int outlet, int limit) {
        String key = FleetStore.normalizeMac(mac);
        List<IncidentRecord> out = new ArrayList<>();
        if (key.isEmpty() || outlet < 1 || outlet > 4) return out;
        int safeLimit = Math.max(1, Math.min(50, limit));
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ts,code,symptom,diagnosis,source_url,power_w,energy_kwh,rck_context " +
                        "FROM diagnostic_incident WHERE mac=? AND outlet=? " +
                        "ORDER BY ts DESC LIMIT " + safeLimit,
                new String[]{key, String.valueOf(outlet)})) {
            while (c.moveToNext()) {
                out.add(new IncidentRecord(
                        c.getLong(0), key, outlet, c.getString(1), c.getString(2),
                        c.getString(3), c.getString(4), c.getDouble(5),
                        c.getDouble(6), c.getString(7)));
            }
        }
        return out;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeIdentifier(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static final class IdentityRecord {
        public final String identifier;
        public final String brand;
        public final String category;
        public final String model;
        public final String source;
        public final long updatedAt;

        IdentityRecord(String identifier, String brand, String category,
                       String model, String source, long updatedAt) {
            this.identifier = identifier == null ? "" : identifier;
            this.brand = brand == null ? "" : brand;
            this.category = category == null ? "" : category;
            this.model = model == null ? "" : model;
            this.source = source == null ? "" : source;
            this.updatedAt = updatedAt;
        }
    }

    public static final class BindingRecord {
        public final String mac;
        public final int outlet;
        public final String brand;
        public final String category;
        public final String model;
        public final String nickname;
        public final long updatedAt;

        BindingRecord(String mac, int outlet, String brand, String category,
                      String model, String nickname, long updatedAt) {
            this.mac = mac;
            this.outlet = outlet;
            this.brand = brand == null ? "" : brand;
            this.category = category == null ? "" : category;
            this.model = model == null ? "" : model;
            this.nickname = nickname == null ? "" : nickname;
            this.updatedAt = updatedAt;
        }

        public String displayName() {
            StringBuilder value = new StringBuilder();
            if (!brand.isEmpty()) value.append(brand);
            if (!model.isEmpty()) {
                if (value.length() > 0) value.append(' ');
                value.append(model);
            }
            if (value.length() == 0 && !category.isEmpty()) value.append(category);
            return value.toString();
        }
    }

    public static final class IncidentRecord {
        public final long ts;
        public final String mac;
        public final int outlet;
        public final String code;
        public final String symptom;
        public final String diagnosis;
        public final String sourceUrl;
        public final double powerW;
        public final double energyKWh;
        public final String rckContext;

        IncidentRecord(long ts, String mac, int outlet, String code, String symptom,
                       String diagnosis, String sourceUrl, double powerW,
                       double energyKWh, String rckContext) {
            this.ts = ts;
            this.mac = mac;
            this.outlet = outlet;
            this.code = code == null ? "" : code;
            this.symptom = symptom == null ? "" : symptom;
            this.diagnosis = diagnosis == null ? "" : diagnosis;
            this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
            this.powerW = powerW;
            this.energyKWh = energyKWh;
            this.rckContext = rckContext == null ? "" : rckContext;
        }
    }
}
