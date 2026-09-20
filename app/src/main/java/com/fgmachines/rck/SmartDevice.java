package com.fgmachines.rck;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Protocol-neutral smart-home device model. This is intentionally independent
 * from MTTL so FG Link can host additional drivers without duplicating the app.
 */
public final class SmartDevice {
    public enum Category {
        POWER_STRIP,
        RELAY,
        LIGHTING,
        SENSOR,
        IR_CONTROLLER,
        ENERGY_MONITOR,
        ROOM_CONTROLLER,
        GATEWAY,
        SMART_PANEL
    }

    public enum Capability {
        LOCAL_CONTROL,
        SWITCH,
        POWER_METERING,
        ENERGY_METERING,
        TEMPERATURE,
        HUMIDITY,
        MOTION,
        CONTACT,
        LEAK,
        SMOKE,
        IR_TRANSMIT,
        IR_LEARN,
        SCENES,
        AUTOMATION,
        REMOTE_ACCESS
    }

    public enum SupportLevel {
        VERIFIED,
        EXPERIMENTAL,
        PLANNED
    }

    public final String id;
    public final String name;
    public final String room;
    public final String model;
    public final String driverId;
    public final Category category;
    public final SupportLevel supportLevel;
    public final boolean online;
    public final int channelCount;
    public final Set<Capability> capabilities;

    public SmartDevice(String id, String name, String room, String model, String driverId,
                       Category category, SupportLevel supportLevel, boolean online,
                       int channelCount, Set<Capability> capabilities) {
        this.id = clean(id);
        this.name = clean(name);
        this.room = clean(room);
        this.model = clean(model);
        this.driverId = clean(driverId);
        this.category = category;
        this.supportLevel = supportLevel;
        this.online = online;
        this.channelCount = Math.max(0, channelCount);
        this.capabilities = capabilities == null || capabilities.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
    }

    public boolean has(Capability capability) {
        return capability != null && capabilities.contains(capability);
    }

    public String displayName() {
        if (!name.isEmpty()) return name;
        if (!model.isEmpty()) return model;
        return id;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
