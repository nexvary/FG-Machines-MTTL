package com.fgmachines.rck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class SmartHomePlatformTest {
    @Test
    public void driverRegistryResolvesProtocolByDriverId() {
        DeviceDriverRegistry registry = new DeviceDriverRegistry();
        FakeSwitchDriver driver = new FakeSwitchDriver();
        registry.register(driver);

        SmartDevice device = new SmartDevice(
                "relay-1", "Desk relay", "Office", "FG-R4", driver.id(),
                SmartDevice.Category.RELAY, SmartDevice.SupportLevel.EXPERIMENTAL,
                true, 4, driver.capabilities());

        assertEquals(1, registry.size());
        assertEquals(driver, registry.forDevice(device));
        assertTrue(registry.forDevice(device).canSwitch(device));
    }

    @Test
    public void automationRulesValidateCapabilitiesAndChannelBounds() {
        SmartDevice switchDevice = new SmartDevice(
                "relay-1", "Desk relay", "Office", "FG-R4", "fake",
                SmartDevice.Category.RELAY, SmartDevice.SupportLevel.EXPERIMENTAL,
                true, 4, EnumSet.of(SmartDevice.Capability.SWITCH));

        PlatformAutomationRule valid = new PlatformAutomationRule(
                "r1", "Night lamp", PlatformAutomationRule.TriggerType.TIME,
                PlatformAutomationRule.ActionType.SWITCH_CHANNEL, "relay-1", 3);
        PlatformAutomationRule invalidChannel = new PlatformAutomationRule(
                "r2", "Bad channel", PlatformAutomationRule.TriggerType.TIME,
                PlatformAutomationRule.ActionType.SWITCH_CHANNEL, "relay-1", 5);

        assertTrue(valid.isCompatible(switchDevice));
        assertFalse(invalidChannel.isCompatible(switchDevice));
    }

    @Test
    public void roadmapSeparatesVerifiedHardwareFromPlannedFamilies() {
        java.util.List<SmartHomePlatform.ProductProfile> roadmap =
                SmartHomePlatform.roadmap();
        assertTrue(roadmap.size() >= 8);
        assertEquals("MTTL-W01", roadmap.get(0).name);
        assertEquals(SmartDevice.SupportLevel.VERIFIED, roadmap.get(0).supportLevel);

        SmartHomePlatform.ProductProfile relay = null;
        SmartHomePlatform.ProductProfile ir = null;
        for (SmartHomePlatform.ProductProfile profile : roadmap) {
            if ("FG Smart Relay".equals(profile.name)) relay = profile;
            if ("FG IR Link".equals(profile.name)) ir = profile;
        }
        assertNotNull(relay);
        assertNotNull(ir);
        assertEquals(SmartDevice.SupportLevel.PLANNED, relay.supportLevel);
        assertTrue(ir.capabilities.contains(SmartDevice.Capability.IR_TRANSMIT));
    }

    private static final class FakeSwitchDriver implements DeviceDriver {
        @Override public String id() { return "fake"; }
        @Override public String displayName() { return "Fake"; }
        @Override public SmartDevice.Category category() { return SmartDevice.Category.RELAY; }
        @Override public SmartDevice.SupportLevel supportLevel() {
            return SmartDevice.SupportLevel.EXPERIMENTAL;
        }
        @Override public Set<SmartDevice.Capability> capabilities() {
            return Collections.unmodifiableSet(EnumSet.of(SmartDevice.Capability.SWITCH));
        }
        @Override public boolean supports(SmartDevice device) {
            return device != null && id().equals(device.driverId);
        }
        @Override public void setSwitch(String deviceId, int channel, boolean on) throws IOException { }
        @Override public void refresh(String deviceId) throws IOException { }
    }
}
