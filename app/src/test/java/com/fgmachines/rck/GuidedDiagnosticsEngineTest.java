package com.fgmachines.rck;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class GuidedDiagnosticsEngineTest {
    @Test public void samsungDrain_hoseAnswerRanksHoseFirst() {
        List<GuidedDiagnosticsEngine.RankedCause> result =
                GuidedDiagnosticsEngine.evaluate("samsung_washer_not_draining",
                        Arrays.asList(true, false, false));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hypothesis.title.toLowerCase().contains("hose"));
    }

    @Test public void samsungDrain_persistentFaultRanksServiceCauseFirst() {
        List<GuidedDiagnosticsEngine.RankedCause> result =
                GuidedDiagnosticsEngine.evaluate("samsung_washer_not_draining",
                        Arrays.asList(false, false, true));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hypothesis.title.toLowerCase().contains("pump"));
    }

    @Test public void lgNoCooling_dirtyFilterRanksAirflowCauseFirst() {
        List<GuidedDiagnosticsEngine.RankedCause> result =
                GuidedDiagnosticsEngine.evaluate("lg_ac_not_cooling",
                        Arrays.asList(false, true, false, false));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hypothesis.title.toLowerCase().contains("filter"));
    }


    @Test public void samsungFill_closedTapRanksSupplyFirst() {
        List<GuidedDiagnosticsEngine.RankedCause> result =
                GuidedDiagnosticsEngine.evaluate("samsung_washer_not_filling",
                        Arrays.asList(true, false, false, false));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hypothesis.title.toLowerCase().contains("supply"));
    }

    @Test public void roborockBrush_visibleDebrisRanksDebrisFirst() {
        List<GuidedDiagnosticsEngine.RankedCause> result =
                GuidedDiagnosticsEngine.evaluate("roborock_s7_main_brush",
                        Arrays.asList(true, false, false));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hypothesis.title.toLowerCase().contains("debris"));
    }

    @Test public void xiaomiBrush_persistentErrorRanksInternalFaultFirst() {
        List<GuidedDiagnosticsEngine.RankedCause> result =
                GuidedDiagnosticsEngine.evaluate("xiaomi_vacuum_mop_main_brush",
                        Arrays.asList(false, false, true));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hypothesis.title.toLowerCase().contains("internal"));
    }

    @Test public void lgFan_persistentCodeRanksFanFaultFirst() {
        List<GuidedDiagnosticsEngine.RankedCause> result =
                GuidedDiagnosticsEngine.evaluate("lg_ac_fan_error",
                        Arrays.asList(false, true, true));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hypothesis.title.toLowerCase().contains("fan motor"));
    }

    @Test public void lowRefrigerantProfileHasServiceBoundary() {
        List<GuidedDiagnosticsEngine.RankedCause> result =
                GuidedDiagnosticsEngine.evaluate("lg_ac_low_refrigerant_codes",
                        Arrays.asList(true, true));
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).hypothesis.serviceBoundary.toLowerCase().contains("hvac"));
    }

    @Test public void allProfilesHaveOfficialHttpsSourcesAndQuestions() {
        assertTrue(GuidedDiagnosticsEngine.profiles().size() >= 10);
        for (GuidedDiagnosticsEngine.Profile profile : GuidedDiagnosticsEngine.profiles()) {
            assertTrue(profile.sourceUrl.startsWith("https://"));
            assertFalse(profile.questions.isEmpty());
            assertFalse(profile.hypotheses.isEmpty());
        }
    }
}
