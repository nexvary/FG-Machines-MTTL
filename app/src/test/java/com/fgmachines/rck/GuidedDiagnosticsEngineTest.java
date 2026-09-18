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

    @Test public void allProfilesHaveOfficialHttpsSourcesAndQuestions() {
        for (GuidedDiagnosticsEngine.Profile profile : GuidedDiagnosticsEngine.profiles()) {
            assertTrue(profile.sourceUrl.startsWith("https://"));
            assertFalse(profile.questions.isEmpty());
            assertFalse(profile.hypotheses.isEmpty());
        }
    }
}
