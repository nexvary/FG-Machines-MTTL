package com.fgmachines.rck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
    }
}
