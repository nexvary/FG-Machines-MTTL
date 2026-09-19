package com.fgmachines.rck;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalTime;
import java.util.Arrays;

public class SmartAutomationEngineTest {
    @Test
    public void awayWindowSupportsNormalAndOvernightRanges() {
        assertTrue(SmartAutomationEngine.isWithinWindow(
                LocalTime.of(20, 0), "18:00", "23:00"));
        assertFalse(SmartAutomationEngine.isWithinWindow(
                LocalTime.of(12, 0), "18:00", "23:00"));

        assertTrue(SmartAutomationEngine.isWithinWindow(
                LocalTime.of(23, 30), "22:00", "06:00"));
        assertTrue(SmartAutomationEngine.isWithinWindow(
                LocalTime.of(2, 0), "22:00", "06:00"));
        assertFalse(SmartAutomationEngine.isWithinWindow(
                LocalTime.of(12, 0), "22:00", "06:00"));
    }

    @Test
    public void linkedOutletRejectsSelfAndClampsDelay() {
        assertTrue(SmartAutomationEngine.isValidFollowTarget(1, 2));
        assertFalse(SmartAutomationEngine.isValidFollowTarget(1, 1));
        assertFalse(SmartAutomationEngine.isValidFollowTarget(1, 0));
        assertEquals(0, SmartAutomationEngine.clampFollowDelaySeconds(-5));
        assertEquals(20, SmartAutomationEngine.clampFollowDelaySeconds(20));
        assertEquals(3600, SmartAutomationEngine.clampFollowDelaySeconds(9999));
    }

    @Test
    public void runtimeUsesObservedContinuousSamplesOnly() {
        long runtime = HistoryStore.calculateRuntimeMs(Arrays.asList(
                new HistoryStore.RuntimePoint(0L, true),
                new HistoryStore.RuntimePoint(30_000L, true),
                new HistoryStore.RuntimePoint(60_000L, false)
        ), 0L, 90_000L);
        assertEquals(60_000L, runtime);

        long offlineGap = HistoryStore.calculateRuntimeMs(Arrays.asList(
                new HistoryStore.RuntimePoint(0L, true),
                new HistoryStore.RuntimePoint(300_000L, false)
        ), 0L, 300_000L);
        assertEquals(0L, offlineGap);
    }
}
