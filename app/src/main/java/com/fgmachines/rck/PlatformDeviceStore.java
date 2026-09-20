package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent protocol-neutral registry for devices that are not part of the
 * legacy MTTL fleet store. The old store remains untouched for compatibility.
 */
public final class PlatformDeviceStore {
    private static final String PREFS = "fg_link_platform_devices_v2";
    private static final String KEY_IDS = "device_ids";
    private static final String P_NAME = "name_";
    private static final String P_ROOM = "room_";
    private static final String P_MODEL = "model_";
    private static final String P_DRIVER = "driver_";
    private static final String P_CATEGORY = "category_";
    private static final String P_SUPPORT = "support_";
    private static final String P_CHANNELS = "channels_";
    private static final String P_CAPS = "capabilities_";

    private final SharedPreferences prefs;

    public PlatformDeviceStore(Context context) {
        if (context == null) throw new IllegalArgumentException("Context is required");
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void upsert(SmartDevice device) {
        if (device == null || device.id.isEmpty()) return;
        Set<String> current = prefs.getStringSet(KEY_IDS, Collections.emptySet());
        Set<String> ids = new HashSet<>(current);
        ids.add(device.id);

        prefs.edit()
                .putStringSet(KEY_IDS, ids)
                .putString(P_NAME + device.id, device.name)
                .putString(P_ROOM + device.id, device.room)
                .putString(P_MODEL + device.id, device.model)
                .putString(P_DRIVER + device.id, device.driverId)
                .putString(P_CATEGORY + device.id, device.category.name())
                .putString(P_SUPPORT + device.id, device.supportLevel.name())
                .putInt(P_CHANNELS + device.id, device.channelCount)
                .putString(P_CAPS + device.id, encodeCapabilities(device.capabilities))
                .apply();
    }

    public synchronized void remove(String id) {
        String key = clean(id);
        if (key.isEmpty()) return;
        Set<String> ids = new HashSet<>(prefs.getStringSet(KEY_IDS, Collections.emptySet()));
        ids.remove(key);
        prefs.edit()
                .putStringSet(KEY_IDS, ids)
                .remove(P_NAME + key)
                .remove(P_ROOM + key)
                .remove(P_MODEL + key)
                .remove(P_DRIVER + key)
                .remove(P_CATEGORY + key)
                .remove(P_SUPPORT + key)
                .remove(P_CHANNELS + key)
                .remove(P_CAPS + key)
                .apply();
    }

    public SmartDevice get(String id) {
        String key = clean(id);
        if (key.isEmpty()) return null;
        if (!prefs.getStringSet(KEY_IDS, Collections.emptySet()).contains(key)) return null;
        return decode(key);
    }

    public List<SmartDevice> list() {
        List<SmartDevice> out = new ArrayList<>();
        for (String id : prefs.getStringSet(KEY_IDS, Collections.emptySet())) {
            SmartDevice device = decode(id);
            if (device != null) out.add(device);
        }
        out.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
        return Collections.unmodifiableList(out);
    }

    private SmartDevice decode(String id) {
        try {
            SmartDevice.Category category = SmartDevice.Category.valueOf(
                    prefs.getString(P_CATEGORY + id, SmartDevice.Category.RELAY.name()));
            SmartDevice.SupportLevel support = SmartDevice.SupportLevel.valueOf(
                    prefs.getString(P_SUPPORT + id, SmartDevice.SupportLevel.PLANNED.name()));
            return new SmartDevice(
                    id,
                    prefs.getString(P_NAME + id, ""),
                    prefs.getString(P_ROOM + id, ""),
                    prefs.getString(P_MODEL + id, ""),
                    prefs.getString(P_DRIVER + id, ""),
                    category,
                    support,
                    false,
                    prefs.getInt(P_CHANNELS + id, 0),
                    decodeCapabilities(prefs.getString(P_CAPS + id, ""))
            );
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    static String encodeCapabilities(Set<SmartDevice.Capability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (SmartDevice.Capability capability : capabilities) {
            if (out.length() > 0) out.append(',');
            out.append(capability.name());
        }
        return out.toString();
    }

    static Set<SmartDevice.Capability> decodeCapabilities(String raw) {
        EnumSet<SmartDevice.Capability> out =
                EnumSet.noneOf(SmartDevice.Capability.class);
        if (raw == null || raw.trim().isEmpty()) return out;
        for (String token : raw.split(",")) {
            try {
                out.add(SmartDevice.Capability.valueOf(token.trim()));
            } catch (IllegalArgumentException ignored) { }
        }
        return out;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
