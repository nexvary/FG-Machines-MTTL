package com.fgmachines.rck;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class DiagnosticVisionParserTest {
    @Test public void parsesSamsungModelAndExplicitCode() {
        DiagnosticVisionParser.Result result = DiagnosticVisionParser.parse(
                "SAMSUNG\nMODEL: WW90T554DAW\nError 4C",
                Arrays.asList("8806090600000"));
        assertEquals("Samsung", result.brand);
        assertEquals("WW90T554DAW", result.model);
        assertEquals("4C", result.errorCode);
        assertEquals(1, result.barcodes.size());
    }

    @Test public void parsesStandaloneDisplayCodeWithoutInventingModel() {
        DiagnosticVisionParser.Result result =
                DiagnosticVisionParser.parse("CH67", null);
        assertEquals("CH67", result.errorCode);
        assertEquals("", result.model);
    }

    @Test public void doesNotTreatArbitrarySerialAsErrorCode() {
        DiagnosticVisionParser.Result result =
                DiagnosticVisionParser.parse("Serial 123456789\nS/N ABCD123456", null);
        assertEquals("", result.errorCode);
    }
}
