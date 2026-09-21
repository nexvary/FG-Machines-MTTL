package com.fgmachines.rck;

import org.junit.Test;

import static org.junit.Assert.*;

public class UniversalAcProtocolTest {
    @Test public void sharpProfiles_are38kHzAndChecksummed() {
        for (SharpAcProtocol.Model model : SharpAcProtocol.Model.values()) {
            SharpAcProtocol p = new SharpAcProtocol()
                    .model(model)
                    .mode(SharpAcProtocol.Mode.COOL)
                    .temp(24)
                    .fan(SharpAcProtocol.Fan.AUTO)
                    .power(true, false);
            assertEquals(38000, p.carrierHz());
            assertTrue(SharpAcProtocol.validChecksum(p.raw()));
            assertEquals(212, p.pattern().length);
        }
    }

    @Test public void midea48_is38kHzAndChecksummed() {
        MideaAcProtocol p = new MideaAcProtocol()
                .power(true)
                .temp(24)
                .mode(MideaAcProtocol.Mode.COOL)
                .fan(MideaAcProtocol.Fan.AUTO);
        assertEquals(38000, p.carrierHz());
        assertTrue(MideaAcProtocol.validChecksum(p.raw()));
        assertEquals(201, p.pattern().length);
    }

    @Test public void carrier64_is38kHzAndChecksummed() {
        Carrier64AcProtocol p = new Carrier64AcProtocol()
                .power(true)
                .temp(24)
                .mode(Carrier64AcProtocol.Mode.COOL)
                .fan(Carrier64AcProtocol.Fan.AUTO);
        assertEquals(38000, p.carrierHz());
        assertTrue(Carrier64AcProtocol.validChecksum(p.raw()));
        assertEquals(132, p.pattern().length);
    }

    @Test public void unionAirCandidateSet_canCoverMultipleOemFamilies() {
        DeviceProfile.Protocol[] candidates = {
                DeviceProfile.Protocol.MIDEA_48,
                DeviceProfile.Protocol.CARRIER_64,
                DeviceProfile.Protocol.SHARP_A907,
                DeviceProfile.Protocol.SHARP_A903,
                DeviceProfile.Protocol.SHARP_A705
        };
        assertEquals(5, candidates.length);
    }
}
