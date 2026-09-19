package com.fgmachines.rck;

import java.text.Normalizer;
import java.util.Locale;

/** Small deterministic parser for local voice-control phrases. */
public final class VoiceCommandParser {
    private VoiceCommandParser() { }

    public enum Type {
        OUTLET_ON,
        OUTLET_OFF,
        ALL_ON,
        ALL_OFF,
        UNKNOWN
    }

    public static Command parse(String phrase) {
        String value = normalize(phrase);
        if (value.isEmpty()) return new Command(Type.UNKNOWN, 0, phrase);

        boolean wantsOff = containsAny(value,
                "turn off", "switch off", "power off", " off ",
                "اقفل", "اطفئ", "اطفي", "ايقاف", "وقف",
                "kapat", "apaga", "apagar", "desactiva",
                "ausschalten", "schalte aus");
        boolean wantsOn = containsAny(value,
                "turn on", "switch on", "power on", " on ",
                "شغل", "تشغيل", "افتح",
                "ac ", " aç ", "enciende", "encender", "activa",
                "einschalten", "schalte ein");

        if (!wantsOn && !wantsOff) return new Command(Type.UNKNOWN, 0, phrase);

        boolean all = containsAny(value,
                " all ", "everything", "all outlets", "all sockets",
                "الكل", "كل المخارج", "كل الفيش", "كل الفيشات",
                " hepsi ", " tumu ", " tümü ",
                " todos ", " todas ", " alle ");

        if (all) {
            if (wantsOff) return new Command(Type.ALL_OFF, 0, phrase);
            if (wantsOn) return new Command(Type.ALL_ON, 0, phrase);
        }

        int outlet = extractOutlet(value);
        if (outlet < 1 || outlet > 4) return new Command(Type.UNKNOWN, 0, phrase);
        return new Command(wantsOff ? Type.OUTLET_OFF : Type.OUTLET_ON, outlet, phrase);
    }

    static String normalize(String raw) {
        if (raw == null) return "";
        String value = Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .trim();
        value = value
                .replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
                .replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
                .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
                .replace('ة', 'ه').replace('ى', 'ي');
        value = value.replaceAll("[\\u064B-\\u065F\\u0670]", "");
        value = value.replaceAll("[^\\p{L}\\p{N}]+", " ");
        return " " + value.replaceAll("\\s+", " ").trim() + " ";
    }

    private static int extractOutlet(String value) {
        for (int outlet = 1; outlet <= 4; outlet++) {
            if (value.contains(" " + outlet + " ")) return outlet;
        }
        if (containsAny(value, " الاول ", " الاولي ", " اول ", " واحد ",
                " one ", " first ", " bir ", " uno ", " eins ", " erste ")) return 1;
        if (containsAny(value, " الثاني ", " الثانيه ", " ثاني ", " اثنين ", " اتنين ",
                " two ", " second ", " iki ", " dos ", " zwei ", " zweite ")) return 2;
        if (containsAny(value, " الثالث ", " الثالثه ", " ثالث ", " ثلاثه ",
                " three ", " third ", " uc ", " üç ", " tres ", " drei ", " dritte ")) return 3;
        if (containsAny(value, " الرابع ", " الرابعه ", " رابع ", " رابعه ", " اربعه ",
                " four ", " fourth ", " dort ", " dört ", " cuatro ", " vier ", " vierte ")) return 4;
        return 0;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    public static final class Command {
        public final Type type;
        public final int outlet;
        public final String rawPhrase;

        Command(Type type, int outlet, String rawPhrase) {
            this.type = type;
            this.outlet = outlet;
            this.rawPhrase = rawPhrase == null ? "" : rawPhrase;
        }

        public boolean isKnown() {
            return type != Type.UNKNOWN;
        }
    }
}
