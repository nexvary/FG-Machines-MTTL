package com.fgmachines.rck;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ExpandedRemoteProtocolTest {
    @Test public void commonAcProtocols_emit38kHzPatterns() {
        IrSignal[] signals = new IrSignal[] {
                new GreeAcProtocol().model(GreeAcProtocol.Model.YAW1F)
                        .power(true).temp(24).mode(GreeAcProtocol.Mode.COOL),
                new LgAcProtocol().model(LgAcProtocol.Model.LG)
                        .power(true).temp(24).mode(LgAcProtocol.Mode.COOL),
                new SamsungAcProtocol().power(true).temp(24)
                        .mode(SamsungAcProtocol.Mode.COOL),
                new HaierAcProtocol().power(true).temp(24)
                        .mode(HaierAcProtocol.Mode.COOL),
                new ToshibaAcProtocol().model(ToshibaAcProtocol.Model.GENERIC)
                        .power(true).temp(24).mode(ToshibaAcProtocol.Mode.COOL),
                new TclAcProtocol().power(true).temp(24)
                        .mode(TclAcProtocol.Mode.COOL),
                new Kelon168AcProtocol().power(true).temp(24)
                        .mode(Kelon168AcProtocol.Mode.COOL)
        };
        for (IrSignal signal : signals) {
            assertEquals(signal.protocolName(), 38000, signal.carrierHz());
            assertNotNull(signal.pattern());
            assertTrue(signal.protocolName(), signal.pattern().length > 50);
            for (int timing : signal.pattern()) {
                assertTrue(signal.protocolName(), timing > 0);
            }
        }
    }

    @Test public void protocolPayloadLengths_areStable() {
        assertEquals(8, new GreeAcProtocol().raw().length);
        assertEquals(14, new TclAcProtocol().raw().length);
        assertEquals(9, new ToshibaAcProtocol().raw().length);
        assertEquals(14, new HaierAcProtocol().raw().length);
        assertEquals(21, new Kelon168AcProtocol().raw().length);
        assertEquals(14, new SamsungAcProtocol().raw14().length);
    }

    @Test public void lgAndSamsung_haveExpectedFrameFamilies() {
        assertTrue(new LgAcProtocol().protocolName().startsWith("LG"));
        assertTrue(new SamsungAcProtocol().protocolName().startsWith("SAMSUNG"));
        assertTrue(new SamsungAcProtocol().pattern().length >
                new SamsungAcProtocol().normalFrame().pattern().length);
    }

    @Test public void verifiedFanProfiles_emitNec38kHz() {
        List<FanRemoteProfile> profiles = FanRemoteProfile.verifiedProfiles();
        assertTrue(profiles.size() >= 2);
        for (FanRemoteProfile profile : profiles) {
            assertNotNull(profile.powerOn);
            IrSignal signal = profile.signal(profile.powerOn);
            assertEquals(38000, signal.carrierHz());
            assertEquals("NEC32", signal.protocolName());
            assertTrue(signal.pattern().length >= 68);
        }
    }

    @Test public void freshTargets_areCataloguedWithoutInventingCodes() {
        boolean foundFresh = false;
        for (String target : FanRemoteProfile.targets()) {
            if (target.startsWith("Fresh")) foundFresh = true;
        }
        assertTrue(foundFresh);
        for (FanRemoteProfile profile : FanRemoteProfile.verifiedProfiles()) {
            assertFalse(profile.manufacturer.equalsIgnoreCase("Fresh"));
        }
    }
}
