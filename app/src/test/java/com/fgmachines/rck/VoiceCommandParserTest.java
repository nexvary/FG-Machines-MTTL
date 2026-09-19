package com.fgmachines.rck;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VoiceCommandParserTest {
    @Test
    public void parsesArabicOutletCommands() {
        VoiceCommandParser.Command on = VoiceCommandParser.parse("شغل المخرج ٢");
        assertEquals(VoiceCommandParser.Type.OUTLET_ON, on.type);
        assertEquals(2, on.outlet);

        VoiceCommandParser.Command off = VoiceCommandParser.parse("اقفل الفيشة الرابعة");
        assertEquals(VoiceCommandParser.Type.OUTLET_OFF, off.type);
        assertEquals(4, off.outlet);
    }

    @Test
    public void parsesEnglishOutletAndAllOffCommands() {
        VoiceCommandParser.Command one = VoiceCommandParser.parse("turn on outlet 3");
        assertEquals(VoiceCommandParser.Type.OUTLET_ON, one.type);
        assertEquals(3, one.outlet);

        VoiceCommandParser.Command all = VoiceCommandParser.parse("turn off all outlets");
        assertEquals(VoiceCommandParser.Type.ALL_OFF, all.type);
    }

    @Test
    public void parsesSupportedLanguageCommands() {
        assertEquals(VoiceCommandParser.Type.OUTLET_OFF,
                VoiceCommandParser.parse("kapat priz 2").type);
        assertEquals(2, VoiceCommandParser.parse("kapat priz 2").outlet);

        assertEquals(VoiceCommandParser.Type.OUTLET_ON,
                VoiceCommandParser.parse("enciende salida 3").type);
        assertEquals(3, VoiceCommandParser.parse("enciende salida 3").outlet);

        assertEquals(VoiceCommandParser.Type.ALL_OFF,
                VoiceCommandParser.parse("alle ausschalten").type);
    }

    @Test
    public void rejectsAmbiguousSpeech() {
        assertEquals(VoiceCommandParser.Type.UNKNOWN,
                VoiceCommandParser.parse("make the room comfortable").type);
    }
}
