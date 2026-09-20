package com.fgmachines.rck;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Verified local driver for the existing MTTL-W01 family. */
public final class MttlDeviceDriver implements DeviceDriver {
    public static final String DRIVER_ID = "mttl-local";

    private static final Set<SmartDevice.Capability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(
                    SmartDevice.Capability.LOCAL_CONTROL,
                    SmartDevice.Capability.SWITCH,
                    SmartDevice.Capability.POWER_METERING,
                    SmartDevice.Capability.ENERGY_METERING,
                    SmartDevice.Capability.TEMPERATURE,
                    SmartDevice.Capability.SCENES,
                    SmartDevice.Capability.AUTOMATION,
                    SmartDevice.Capability.REMOTE_ACCESS
            ));

    private final ControllerHub hub;

    public MttlDeviceDriver(ControllerHub hub) {
        if (hub == null) throw new IllegalArgumentException("ControllerHub is required");
        this.hub = hub;
    }

    @Override public String id() { return DRIVER_ID; }
    @Override public String displayName() { return "MTTL local TCP"; }
    @Override public SmartDevice.Category category() { return SmartDevice.Category.POWER_STRIP; }
    @Override public SmartDevice.SupportLevel supportLevel() {
        return SmartDevice.SupportLevel.VERIFIED;
    }
    @Override public Set<SmartDevice.Capability> capabilities() { return CAPABILITIES; }

    @Override public boolean supports(SmartDevice device) {
        return device != null && DRIVER_ID.equals(device.driverId);
    }

    @Override public boolean isOnline(String deviceId) {
        return hub.isConnected(deviceId);
    }

    @Override public void setSwitch(String deviceId, int channel, boolean on) throws IOException {
        if (channel < 1 || channel > 4) throw new IOException("MTTL channel must be 1..4");
        hub.setOutlet(deviceId, channel, on);
    }

    @Override public void refresh(String deviceId) throws IOException {
        hub.refresh(deviceId);
    }
}
