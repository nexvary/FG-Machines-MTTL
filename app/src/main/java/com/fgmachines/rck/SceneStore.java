package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Local scene presets for the four outlet states of each fleet device. */
public final class SceneStore {
    private static final String PREFS = "fg_rck_scenes";
    private static final String KEY_SCENES = "scenes";

    private final SharedPreferences prefs;

    public SceneStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized Scene save(String mac, String name, int outletMask) {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty()) throw new IllegalArgumentException("Device MAC is required");
        String safeName = cleanName(name);
        if (safeName.isEmpty()) throw new IllegalArgumentException("Scene name is required");
        if ((outletMask & ~0x0F) != 0) throw new IllegalArgumentException("Invalid outlet mask");

        Scene scene = new Scene(UUID.randomUUID().toString(), key, safeName, outletMask & 0x0F);
        Set<String> copy = new HashSet<>(prefs.getStringSet(KEY_SCENES, Collections.emptySet()));
        copy.add(encode(scene));
        prefs.edit().putStringSet(KEY_SCENES, copy).apply();
        return scene;
    }

    public synchronized void delete(String id) {
        if (id == null || id.isEmpty()) return;
        Set<String> copy = new HashSet<>(prefs.getStringSet(KEY_SCENES, Collections.emptySet()));
        copy.removeIf(raw -> {
            Scene scene = decode(raw);
            return scene != null && id.equals(scene.id);
        });
        prefs.edit().putStringSet(KEY_SCENES, copy).apply();
    }

    public List<Scene> list(String mac) {
        String key = FleetStore.normalizeMac(mac);
        List<Scene> out = new ArrayList<>();
        if (key.isEmpty()) return out;
        for (String raw : prefs.getStringSet(KEY_SCENES, Collections.emptySet())) {
            Scene scene = decode(raw);
            if (scene != null && key.equals(scene.mac)) out.add(scene);
        }
        out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }

    static String encode(Scene scene) {
        String encodedName = Base64.encodeToString(
                scene.name.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        return scene.id + "|" + scene.mac + "|" + encodedName + "|" + scene.outletMask;
    }

    static Scene decode(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 4) return null;
        try {
            String id = parts[0];
            String mac = FleetStore.normalizeMac(parts[1]);
            String name = new String(Base64.decode(parts[2], Base64.DEFAULT), StandardCharsets.UTF_8);
            int mask = Integer.parseInt(parts[3]);
            if (id.isEmpty() || mac.isEmpty() || cleanName(name).isEmpty() || (mask & ~0x0F) != 0) {
                return null;
            }
            return new Scene(id, mac, cleanName(name), mask);
        } catch (RuntimeException error) {
            return null;
        }
    }

    private static String cleanName(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
    }

    public static final class Scene {
        public final String id;
        public final String mac;
        public final String name;
        public final int outletMask;

        Scene(String id, String mac, String name, int outletMask) {
            this.id = id;
            this.mac = mac;
            this.name = name;
            this.outletMask = outletMask;
        }

        public boolean outletOn(int outlet) {
            if (outlet < 1 || outlet > 4) return false;
            return (outletMask & (1 << (outlet - 1))) != 0;
        }

        public int onCount() {
            return Integer.bitCount(outletMask);
        }

        @Override public String toString() {
            return name + " · " + onCount() + "/4 ON";
        }
    }
}
