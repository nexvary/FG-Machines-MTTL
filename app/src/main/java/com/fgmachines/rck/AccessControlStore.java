package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Local API users/sharing registry. Access tokens remain on the controller phone. */
public final class AccessControlStore {
    private static final String PREFS = "fg_rck_access";
    private static final String KEY_ENTRIES = "entries";
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum Role {
        VIEW(1), CONTROL(2), ADMIN(3);
        final int level;
        Role(int level) { this.level = level; }
        public boolean allows(Role required) { return level >= required.level; }
    }

    private final SharedPreferences prefs;

    public AccessControlStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized AccessEntry create(String name, Role role) {
        String safeName = name == null || name.trim().isEmpty() ? "Shared user" : name.trim();
        Role safeRole = role == null ? Role.VIEW : role;
        String token = generateToken();
        Set<String> copy = new HashSet<>(prefs.getStringSet(KEY_ENTRIES, Collections.emptySet()));
        copy.add(encode(safeName, safeRole, token));
        prefs.edit().putStringSet(KEY_ENTRIES, copy).apply();
        return new AccessEntry(safeName, safeRole, token);
    }

    public synchronized void revoke(String token) {
        if (token == null) return;
        Set<String> copy = new HashSet<>(prefs.getStringSet(KEY_ENTRIES, Collections.emptySet()));
        copy.removeIf(raw -> {
            AccessEntry entry = decode(raw);
            return entry != null && token.equals(entry.token);
        });
        prefs.edit().putStringSet(KEY_ENTRIES, copy).apply();
    }

    public List<AccessEntry> list() {
        List<AccessEntry> entries = new ArrayList<>();
        for (String raw : prefs.getStringSet(KEY_ENTRIES, Collections.emptySet())) {
            AccessEntry entry = decode(raw);
            if (entry != null) entries.add(entry);
        }
        entries.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return entries;
    }

    public Role roleForToken(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        for (AccessEntry entry : list()) {
            if (constantTimeEquals(entry.token, token.trim())) return entry.role;
        }
        return null;
    }

    public boolean authorized(String token, Role required) {
        Role role = roleForToken(token);
        return role != null && role.allows(required);
    }

    static String encode(String name, Role role, String token) {
        String safeName = Base64.encodeToString(
                name.getBytes(java.nio.charset.StandardCharsets.UTF_8), Base64.NO_WRAP);
        return safeName + "|" + role.name() + "|" + token;
    }

    static AccessEntry decode(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 3) return null;
        try {
            String name = new String(Base64.decode(parts[0], Base64.DEFAULT),
                    java.nio.charset.StandardCharsets.UTF_8);
            Role role = Role.valueOf(parts[1]);
            if (parts[2].length() < 20) return null;
            return new AccessEntry(name, role, parts[2]);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static String generateToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aa = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (aa.length != bb.length) return false;
        int diff = 0;
        for (int i = 0; i < aa.length; i++) diff |= aa[i] ^ bb[i];
        return diff == 0;
    }

    public static final class AccessEntry {
        public final String name;
        public final Role role;
        public final String token;
        AccessEntry(String name, Role role, String token) {
            this.name = name; this.role = role; this.token = token;
        }
    }
}
