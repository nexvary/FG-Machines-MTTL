package com.fgmachines.rck;

import java.io.IOException;
import java.util.Set;

/**
 * Adapter contract between FG Link and a concrete device protocol.
 * New hardware should implement this interface instead of adding device-specific
 * branches throughout the Activity or automation UI.
 */
public interface DeviceDriver {
    String id();
    String displayName();
    SmartDevice.Category category();
    SmartDevice.SupportLevel supportLevel();
    Set<SmartDevice.Capability> capabilities();

    boolean supports(SmartDevice device);

    default boolean canSwitch(SmartDevice device) {
        return supports(device) && capabilities().contains(SmartDevice.Capability.SWITCH);
    }

    void setSwitch(String deviceId, int channel, boolean on) throws IOException;
    void refresh(String deviceId) throws IOException;
}
