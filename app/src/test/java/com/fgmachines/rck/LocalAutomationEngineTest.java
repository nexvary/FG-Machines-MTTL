package com.fgmachines.rck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalTime;

public class LocalAutomationEngineTest {
    @Test
    public void validatesTwentyFourHourTimes() {
        assertTrue(LocalAutomationEngine.isValidTime(""));
        assertTrue(LocalAutomationEngine.isValidTime("00:00"));
        assertTrue(LocalAutomationEngine.isValidTime("23:59"));
        assertFalse(LocalAutomationEngine.isValidTime("24:00"));
        assertFalse(LocalAutomationEngine.isValidTime("9:30"));
        assertFalse(LocalAutomationEngine.isValidTime("12:60"));
    }

    @Test
    public void normalizesValidTimesAndRejectsInvalidTimes() {
        assertEquals("07:05", LocalAutomationEngine.normalizeTime("07:05"));
        assertEquals("", LocalAutomationEngine.normalizeTime("7:05"));
        assertEquals("", LocalAutomationEngine.normalizeTime("invalid"));
    }

    @Test
    public void createsStableDeadlinePreferenceKey() {
        assertEquals("automation_deadline_AABBCCDDEEFF_3",
                LocalAutomationEngine.deadlinePreferenceKey("AA:BB:CC:DD:EE:FF", 3));
        assertEquals("automation_standby_deadline_AABBCCDDEEFF_2",
                LocalAutomationEngine.standbyDeadlinePreferenceKey("AA:BB:CC:DD:EE:FF", 2));
        assertEquals("automation_away_next_AABBCCDDEEFF",
                LocalAutomationEngine.awayNextPreferenceKey("AA:BB:CC:DD:EE:FF"));
    }

    @Test
    public void supportsNormalAndOvernightAwayWindows() {
        assertTrue(LocalAutomationEngine.isWithinAwayWindow(
                "18:00", "23:00", LocalTime.of(20, 0)));
        assertFalse(LocalAutomationEngine.isWithinAwayWindow(
                "18:00", "23:00", LocalTime.of(12, 0)));
        assertTrue(LocalAutomationEngine.isWithinAwayWindow(
                "22:00", "06:00", LocalTime.of(23, 30)));
        assertTrue(LocalAutomationEngine.isWithinAwayWindow(
                "22:00", "06:00", LocalTime.of(5, 30)));
        assertFalse(LocalAutomationEngine.isWithinAwayWindow(
                "22:00", "06:00", LocalTime.of(12, 0)));
    }

    @Test
    public void keepsAwayIntervalsInsideConfiguredBounds() {
        for (int i = 0; i < 50; i++) {
            int value = LocalAutomationEngine.randomIntervalMinutes(10, 15);
            assertTrue(value >= 10);
            assertTrue(value <= 15);
        }
        assertEquals(7, LocalAutomationEngine.randomIntervalMinutes(7, 7));
    }

    @Test
    public void formatsRuntimeDurations() {
        assertEquals("0h 00m", OutletRuntimeStore.formatDuration(0));
        assertEquals("2h 05m", OutletRuntimeStore.formatDuration(125L * 60L * 1000L));
    }
}
