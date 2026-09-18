package com.fgmachines.rck;

import org.junit.Test;

import static org.junit.Assert.*;

public class HardwareCatalogTest {
    @Test public void identifiesDawonFromStickerModel() {
        HardwareCatalog.Profile p = HardwareCatalog.identifyFromLabel("Model MTD-01");
        assertNotNull(p);
        assertEquals("dawon_mtd_01", p.id);
        assertEquals(HardwareCatalog.Transport.Z_WAVE_GATEWAY, p.transport);
        assertTrue(p.requiresGateway());
    }

    @Test public void identifiesDawonFromCertificationModel() {
        HardwareCatalog.Profile p = HardwareCatalog.identifyFromLabel(
                "MSIP-CMM-DaW-PM-M130-ZW JH04151-17006");
        assertNotNull(p);
        assertEquals("MTD-01 / PM-M130-ZW", p.displayModel);
        assertEquals(2, p.usbPortCount);
        assertEquals(3500, p.maxPowerW);
        assertEquals(0x018C, p.zwaveManufacturerId);
        assertEquals(0x0042, p.zwaveProductTypeId);
        assertEquals(0x0007, p.zwaveProductId);
        assertTrue(p.zwaveCommandClasses.contains("Switch Binary"));
        assertTrue(p.zwaveCommandClasses.contains("Meter v3"));
    }

    @Test public void doesNotTreatDawonAsMttl() {
        HardwareCatalog.Profile p = HardwareCatalog.identifyFromLabel("PM-M130-ZW");
        assertNotNull(p);
        assertNotEquals(HardwareCatalog.Transport.MTTL_LOCAL_TCP, p.transport);
    }

    @Test public void keepsMttlFamilyRecognition() {
        HardwareCatalog.Profile p = HardwareCatalog.identifyFromLabel("MTTL-W01 HU04139-17002C");
        assertNotNull(p);
        assertEquals("mttl_w01", p.id);
        assertEquals(HardwareCatalog.Transport.MTTL_LOCAL_TCP, p.transport);
    }

    @Test public void unknownLabelDoesNotGuess() {
        assertNull(HardwareCatalog.identifyFromLabel("generic power strip 1234"));
    }
}
