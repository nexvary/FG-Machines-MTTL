package com.fgmachines.rck;

import org.junit.Test;

import static org.junit.Assert.*;

public class UsbHardwareProfileTest {
    @Test public void mttlW01DoesNotExposeInventedUsbSwitchChannels() {
        assertEquals(2, UsbHardwareProfile.USB_PORT_COUNT);
        assertEquals(4, UsbHardwareProfile.VERIFIED_RELAY_CHANNEL_COUNT);
        assertEquals(
                UsbHardwareProfile.ControlMode.SHARED_CHARGER_NO_VERIFIED_SWITCH,
                UsbHardwareProfile.modeForModel("MTTL-W01"));
        assertFalse(UsbHardwareProfile.canSendIndependentUsbCommand("MTTL-W01", 1));
        assertFalse(UsbHardwareProfile.canSendIndependentUsbCommand("MTTL-W01", 2));
    }

    @Test public void invalidUsbPortIsNeverControllable() {
        assertFalse(UsbHardwareProfile.canSendIndependentUsbCommand("MTTL-W01", 0));
        assertFalse(UsbHardwareProfile.canSendIndependentUsbCommand("MTTL-W01", 3));
    }
}
