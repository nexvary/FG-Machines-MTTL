package com.fgmachines.rck;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ApplianceDiagnosticsCatalogTest {
    @Test public void samsung4c_isBrandScopedAndSourceAware() {
        List<ApplianceDiagnosticsCatalog.Match> matches =
                ApplianceDiagnosticsCatalog.search("Samsung", "Washer", "", "4C", "");
        assertFalse(matches.isEmpty());
        assertEquals("Samsung", matches.get(0).entry.brand);
        assertTrue(matches.get(0).entry.codes.contains("4C"));
        assertTrue(matches.get(0).entry.sourceUrl.startsWith("https://www.samsung.com/"));
    }

    @Test public void sameCodeDoesNotLeakAcrossWrongBrand() {
        List<ApplianceDiagnosticsCatalog.Match> matches =
                ApplianceDiagnosticsCatalog.search("Roborock", "Robot Vacuum", "S7", "4C", "");
        assertTrue(matches.isEmpty());
    }

    @Test public void roborockS7_modelRestrictionIsEnforced() {
        assertFalse(ApplianceDiagnosticsCatalog.search(
                "Roborock", "Robot Vacuum", "S8", "13", "").size() > 0);
        List<ApplianceDiagnosticsCatalog.Match> s7 =
                ApplianceDiagnosticsCatalog.search("Roborock", "Robot Vacuum", "S7", "13", "");
        assertFalse(s7.isEmpty());
        assertTrue(s7.get(0).modelVerified);
    }

    @Test public void symptomSearchSupportsArabic() {
        List<ApplianceDiagnosticsCatalog.Match> matches =
                ApplianceDiagnosticsCatalog.search("Samsung", "Washer", "", "", "لا تصرف المياه");
        assertFalse(matches.isEmpty());
        assertTrue(matches.get(0).entry.codes.contains("5C"));
    }

    @Test public void xiaomiVacuumCodeIsModelScoped() {
        List<ApplianceDiagnosticsCatalog.Match> matches =
                ApplianceDiagnosticsCatalog.search(
                        "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum Mop", "10", "");
        assertFalse(matches.isEmpty());
        assertEquals("Xiaomi", matches.get(0).entry.brand);
        assertTrue(matches.get(0).modelVerified);

        assertTrue(ApplianceDiagnosticsCatalog.search(
                "Xiaomi", "Robot Vacuum", "Robot Vacuum H50", "10", "").isEmpty());
    }

    @Test public void lgNoCoolingSymptomFindsOfficialGuidance() {
        List<ApplianceDiagnosticsCatalog.Match> matches =
                ApplianceDiagnosticsCatalog.search(
                        "LG", "Air Conditioner", "", "", "لا يبرد");
        assertFalse(matches.isEmpty());
        assertTrue(matches.get(0).entry.sourceUrl.contains("lg.com"));
    }
}
