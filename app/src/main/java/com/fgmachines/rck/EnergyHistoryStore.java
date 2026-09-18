package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;

/**
 * Stores lightweight local energy deltas derived from the cumulative energy
 * meter reported by the strip. No cloud account or external database is used.
 */
public final class EnergyHistoryStore {
    private static final String PREFS = "fg_rck_settings";
    private static final String KEY_LAST_METER = "energy_history_last_meter_";
    private static final String KEY_DAY_TOTAL = "energy_history_day_";

    private final SharedPreferences prefs;

    public EnergyHistoryStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void record(String mac, MttlProtocol.Telemetry telemetry) {
        if (mac == null || telemetry == null) return;
        double meter = totalEnergyKWh(telemetry);
        String deviceKey = sanitize(mac);
        String lastKey = KEY_LAST_METER + deviceKey;

        double last = Double.longBitsToDouble(
                prefs.getLong(lastKey, Double.doubleToRawLongBits(Double.NaN)));
        double delta = 0.0;
        if (Double.isFinite(last) && meter >= last) {
            delta = meter - last;
        }

        SharedPreferences.Editor editor = prefs.edit()
                .putLong(lastKey, Double.doubleToRawLongBits(meter));

        if (delta > 0.0 && delta < 1000.0) {
            String dayKey = dayKey(LocalDate.now());
            double current = Double.longBitsToDouble(
                    prefs.getLong(dayKey, Double.doubleToRawLongBits(0.0)));
            editor.putLong(dayKey, Double.doubleToRawLongBits(current + delta));
        }
        editor.apply();
    }

    public synchronized double todayKWh() {
        return valueFor(LocalDate.now());
    }

    public synchronized double yesterdayKWh() {
        return valueFor(LocalDate.now().minusDays(1));
    }

    public synchronized double lastSevenDaysKWh() {
        double total = 0.0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) total += valueFor(today.minusDays(i));
        return total;
    }

    public synchronized double lastThirtyDaysKWh() {
        double total = 0.0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 30; i++) total += valueFor(today.minusDays(i));
        return total;
    }

    public static double totalEnergyKWh(MttlProtocol.Telemetry telemetry) {
        if (telemetry == null) return 0.0;
        double total = 0.0;
        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            total += Math.max(0.0, outlet.energyKWh);
        }
        return total;
    }

    static String sanitize(String mac) {
        return mac.replace(":", "").replace("-", "").toUpperCase();
    }

    private double valueFor(LocalDate date) {
        return Double.longBitsToDouble(
                prefs.getLong(dayKey(date), Double.doubleToRawLongBits(0.0)));
    }

    private static String dayKey(LocalDate date) {
        return KEY_DAY_TOTAL + date;
    }
}
