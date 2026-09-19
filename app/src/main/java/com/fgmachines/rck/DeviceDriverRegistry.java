package com.fgmachines.rck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of protocol adapters available to the FG Link platform. */
public final class DeviceDriverRegistry {
    private final Map<String, DeviceDriver> drivers = new LinkedHashMap<>();

    public synchronized void register(DeviceDriver driver) {
        if (driver == null || driver.id() == null || driver.id().trim().isEmpty()) {
            throw new IllegalArgumentException("Driver id is required");
        }
        drivers.put(driver.id().trim(), driver);
    }

    public synchronized DeviceDriver byId(String id) {
        return id == null ? null : drivers.get(id.trim());
    }

    public synchronized DeviceDriver forDevice(SmartDevice device) {
        if (device == null) return null;
        DeviceDriver direct = byId(device.driverId);
        if (direct != null && direct.supports(device)) return direct;
        for (DeviceDriver driver : drivers.values()) {
            if (driver.supports(device)) return driver;
        }
        return null;
    }

    public synchronized List<DeviceDriver> all() {
        return Collections.unmodifiableList(new ArrayList<>(drivers.values()));
    }

    public synchronized int size() {
        return drivers.size();
    }
}
