package com.fgmachines.rck;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MttlProtocolTest {

    @Test
    public void recognizesKnownSetupFamilies() {
        assertTrue(ModelCatalog.isSetupSsid("TONLY_TAP_1C0C50"));
        assertTrue(ModelCatalog.isSetupSsid("only_tap_ABC1234"));
        assertFalse(ModelCatalog.isSetupSsid("HOME_WIFI"));
        assertEquals("LGU_1C0C50", ModelCatalog.setupPassword("TONLY_TAP_1C0C50"));
        assertNull(ModelCatalog.setupPassword("HOME_WIFI"));
    }

    @Test
    public void catalogContainsObservedHardwareRevisions() {
        assertTrue(ModelCatalog.isKnownCertificateRevision("HU04139-17002A"));
        assertTrue(ModelCatalog.isKnownCertificateRevision("HU04139-17002B"));
        assertTrue(ModelCatalog.isKnownCertificateRevision("HU04139-17002E"));
        assertTrue(ModelCatalog.knownFirmwares().contains("1.0.66"));
        assertTrue(ModelCatalog.knownFirmwares().contains("1.0.110"));
    }

    @Test
    public void parsesValidBootInfoAndRejectsClientMismatch() {
        MttlProtocol.BootInfo boot = MttlProtocol.parseBootInfo(
                "up:bootinfo:lgutap;88D0391C0C50;88D0391C0C50;1.0.66;connect"
        );
        assertNotNull(boot);
        assertEquals("lgutap", boot.model);
        assertEquals("88D0391C0C50", boot.mac);
        assertEquals("1.0.66", boot.firmwareVersion);
        assertTrue(ModelCatalog.isCompatibleBootModel(boot.model));

        assertNull(MttlProtocol.parseBootInfo(
                "up:bootinfo:lgutap;88D0391C0C50;000000000000;1.0.66;connect"
        ));
    }

    @Test
    public void buildsAndParsesOutletCommands() {
        assertEquals("up:onoff:1:on", MttlProtocol.setOutlet(1, true));
        assertEquals("up:onoff:4:off", MttlProtocol.setOutlet(4, false));

        MttlProtocol.OutletState state = MttlProtocol.parseOnOff("up:event:onoff:3:on");
        assertNotNull(state);
        assertEquals(3, state.outlet);
        assertTrue(state.on);
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesOutOfRangeOutlet() {
        MttlProtocol.setOutlet(5, true);
    }

    @Test
    public void parsesFourChannelTelemetry() {
        String response = "up:getinfo:"
                + "1:0;on;0;off;off;1234;00000064;00000050;00000000;on;00;25:"
                + "2:0;off;0;off;off;0;00000032;00000030;00000000;on;00;24:"
                + "3:0;on;0;on;off;2500;0000000A;00000009;00000001;on;01;30:"
                + "4:0;off;0;off;on;50;00000001;00000001;00000002;off;02;-5";

        MttlProtocol.Telemetry telemetry = MttlProtocol.parseGetInfo(response);
        assertNotNull(telemetry);
        assertEquals(4, telemetry.outlets.size());
        assertTrue(telemetry.outlets.get(0).relayOn);
        assertEquals(1.234, telemetry.outlets.get(0).powerW, 0.0001);
        assertEquals(100L, telemetry.outlets.get(0).energyWh);
        assertTrue(telemetry.outlets.get(2).overloadProtection);
        assertTrue(telemetry.outlets.get(3).overheatProtection);
        assertEquals(-5, telemetry.outlets.get(3).temperatureC);
    }
}
