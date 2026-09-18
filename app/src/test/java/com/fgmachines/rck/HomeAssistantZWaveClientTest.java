package com.fgmachines.rck;

import org.junit.Test;

import static org.junit.Assert.*;

public class HomeAssistantZWaveClientTest {
    @Test public void validatesSwitchEntityIds() {
        assertTrue(HomeAssistantZWaveClient.isSwitchEntity("switch.mtd_01"));
        assertFalse(HomeAssistantZWaveClient.isSwitchEntity("sensor.mtd_01_power"));
        assertFalse(HomeAssistantZWaveClient.isSwitchEntity("switch.MTD-01"));
        assertFalse(HomeAssistantZWaveClient.isSwitchEntity("../switch.mtd"));
    }

    @Test public void ranksDawonCandidatesAheadOfGenericSwitches() {
        int exact = HomeAssistantZWaveClient.dawOnScore(
                "switch.mtd_01", "Dawon MTD-01 Power Manager");
        int dawOn = HomeAssistantZWaveClient.dawOnScore(
                "switch.living_room", "Dawon Power Manager");
        int generic = HomeAssistantZWaveClient.dawOnScore(
                "switch.living_room", "Living room lamp");
        assertTrue(exact > dawOn);
        assertTrue(dawOn > generic);
    }
}
