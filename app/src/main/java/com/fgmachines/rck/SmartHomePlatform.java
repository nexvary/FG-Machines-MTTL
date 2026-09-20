package com.fgmachines.rck;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Protocol-neutral facade for FG Link.
 *
 * The existing MTTL stack is the first production driver. Future relay, sensor,
 * IR, energy and gateway protocols plug into DeviceDriverRegistry instead of
 * expanding MainActivity with protocol-specific branches.
 */
public final class SmartHomePlatform {
    public static final String PLATFORM_NAME = "FG Link";

    private final FleetStore fleetStore;
    private final ControllerHub controllerHub;
    private final DeviceDriverRegistry drivers;
    private final PlatformDeviceStore platformStore;

    public SmartHomePlatform(Context context, ControllerHub controllerHub) {
        if (context == null) throw new IllegalArgumentException("Context is required");
        if (controllerHub == null) throw new IllegalArgumentException("ControllerHub is required");
        this.fleetStore = new FleetStore(context.getApplicationContext());
        this.controllerHub = controllerHub;
        this.drivers = new DeviceDriverRegistry();
        this.platformStore = new PlatformDeviceStore(context.getApplicationContext());
        this.drivers.register(new MttlDeviceDriver(controllerHub));
    }

    public DeviceDriverRegistry drivers() {
        return drivers;
    }

    public List<SmartDevice> devices() {
        Map<String, SmartDevice> merged = new LinkedHashMap<>();
        DeviceDriver mttl = drivers.byId(MttlDeviceDriver.DRIVER_ID);
        for (FleetStore.DeviceRecord record : fleetStore.list()) {
            SmartDevice device = new SmartDevice(
                    record.mac,
                    record.displayName(),
                    record.room,
                    "MTTL-W01",
                    MttlDeviceDriver.DRIVER_ID,
                    SmartDevice.Category.POWER_STRIP,
                    SmartDevice.SupportLevel.VERIFIED,
                    controllerHub.isConnected(record.mac),
                    4,
                    mttl == null ? Collections.emptySet() : mttl.capabilities()
            );
            merged.put(device.id, device);
        }

        for (SmartDevice stored : platformStore.list()) {
            DeviceDriver driver = drivers.forDevice(stored);
            SmartDevice resolved = new SmartDevice(
                    stored.id,
                    stored.name,
                    stored.room,
                    stored.model,
                    stored.driverId,
                    stored.category,
                    driver == null ? stored.supportLevel : driver.supportLevel(),
                    driver != null && driver.isOnline(stored.id),
                    stored.channelCount,
                    driver == null ? stored.capabilities : driver.capabilities()
            );
            merged.putIfAbsent(resolved.id, resolved);
        }
        return Collections.unmodifiableList(new ArrayList<>(merged.values()));
    }

    public void registerDevice(SmartDevice device) {
        platformStore.upsert(device);
    }

    public void removeDevice(String deviceId) {
        platformStore.remove(deviceId);
    }

    public SmartDevice device(String id) {
        String raw = id == null ? "" : id.trim();
        if (raw.isEmpty()) return null;
        String macKey = FleetStore.normalizeMac(raw);
        for (SmartDevice device : devices()) {
            if (device.id.equals(raw) || device.id.equalsIgnoreCase(raw)) return device;
            if (MttlDeviceDriver.DRIVER_ID.equals(device.driverId)
                    && device.id.equalsIgnoreCase(macKey)) return device;
        }
        return null;
    }

    public void setSwitch(String deviceId, int channel, boolean on) throws IOException {
        SmartDevice device = device(deviceId);
        if (device == null) throw new IOException("Unknown FG Link device");
        DeviceDriver driver = drivers.forDevice(device);
        if (driver == null || !driver.canSwitch(device)) {
            throw new IOException("Device driver does not expose switch control");
        }
        driver.setSwitch(device.id, channel, on);
    }

    public void refresh(String deviceId) throws IOException {
        SmartDevice device = device(deviceId);
        if (device == null) throw new IOException("Unknown FG Link device");
        DeviceDriver driver = drivers.forDevice(device);
        if (driver == null) throw new IOException("No driver registered for device");
        driver.refresh(device.id);
    }

    public Summary summary() {
        int online = 0;
        int rooms = fleetStore.rooms().size();
        List<SmartDevice> devices = devices();
        for (SmartDevice device : devices) if (device.online) online++;
        return new Summary(devices.size(), online, rooms, drivers.size());
    }

    /**
     * Product families the platform is being structured to host. PLANNED means
     * the app architecture reserves the capability; it is not a support claim.
     */
    public static List<ProductProfile> roadmap() {
        List<ProductProfile> out = new ArrayList<>();
        out.add(new ProductProfile("MTTL-W01", SmartDevice.Category.POWER_STRIP,
                SmartDevice.SupportLevel.VERIFIED, 4,
                EnumSet.of(SmartDevice.Capability.SWITCH,
                        SmartDevice.Capability.POWER_METERING,
                        SmartDevice.Capability.ENERGY_METERING,
                        SmartDevice.Capability.TEMPERATURE)));
        out.add(new ProductProfile("FG Smart Relay", SmartDevice.Category.RELAY,
                SmartDevice.SupportLevel.PLANNED, 4,
                EnumSet.of(SmartDevice.Capability.SWITCH,
                        SmartDevice.Capability.LOCAL_CONTROL)));
        out.add(new ProductProfile("FG IR Link", SmartDevice.Category.IR_CONTROLLER,
                SmartDevice.SupportLevel.PLANNED, 0,
                EnumSet.of(SmartDevice.Capability.IR_TRANSMIT,
                        SmartDevice.Capability.IR_LEARN,
                        SmartDevice.Capability.LOCAL_CONTROL)));
        out.add(new ProductProfile("FG Sensor Hub", SmartDevice.Category.SENSOR,
                SmartDevice.SupportLevel.PLANNED, 0,
                EnumSet.of(SmartDevice.Capability.TEMPERATURE,
                        SmartDevice.Capability.HUMIDITY,
                        SmartDevice.Capability.MOTION,
                        SmartDevice.Capability.CONTACT,
                        SmartDevice.Capability.LEAK,
                        SmartDevice.Capability.SMOKE)));
        out.add(new ProductProfile("FG Energy Monitor", SmartDevice.Category.ENERGY_MONITOR,
                SmartDevice.SupportLevel.PLANNED, 0,
                EnumSet.of(SmartDevice.Capability.POWER_METERING,
                        SmartDevice.Capability.ENERGY_METERING)));
        out.add(new ProductProfile("FG Room Controller", SmartDevice.Category.ROOM_CONTROLLER,
                SmartDevice.SupportLevel.PLANNED, 4,
                EnumSet.of(SmartDevice.Capability.SWITCH,
                        SmartDevice.Capability.IR_TRANSMIT,
                        SmartDevice.Capability.TEMPERATURE,
                        SmartDevice.Capability.MOTION)));
        out.add(new ProductProfile("FG Gateway", SmartDevice.Category.GATEWAY,
                SmartDevice.SupportLevel.PLANNED, 0,
                EnumSet.of(SmartDevice.Capability.LOCAL_CONTROL,
                        SmartDevice.Capability.REMOTE_ACCESS,
                        SmartDevice.Capability.AUTOMATION)));
        out.add(new ProductProfile("FG Smart Panel", SmartDevice.Category.SMART_PANEL,
                SmartDevice.SupportLevel.PLANNED, 8,
                EnumSet.of(SmartDevice.Capability.SWITCH,
                        SmartDevice.Capability.POWER_METERING,
                        SmartDevice.Capability.ENERGY_METERING,
                        SmartDevice.Capability.AUTOMATION)));
        return Collections.unmodifiableList(out);
    }

    public static final class Summary {
        public final int devices;
        public final int online;
        public final int rooms;
        public final int drivers;

        Summary(int devices, int online, int rooms, int drivers) {
            this.devices = devices;
            this.online = online;
            this.rooms = rooms;
            this.drivers = drivers;
        }
    }

    public static final class ProductProfile {
        public final String name;
        public final SmartDevice.Category category;
        public final SmartDevice.SupportLevel supportLevel;
        public final int channels;
        public final java.util.Set<SmartDevice.Capability> capabilities;

        ProductProfile(String name, SmartDevice.Category category,
                       SmartDevice.SupportLevel supportLevel, int channels,
                       java.util.Set<SmartDevice.Capability> capabilities) {
            this.name = name;
            this.category = category;
            this.supportLevel = supportLevel;
            this.channels = Math.max(0, channels);
            this.capabilities = Collections.unmodifiableSet(EnumSet.copyOf(capabilities));
        }
    }
}
