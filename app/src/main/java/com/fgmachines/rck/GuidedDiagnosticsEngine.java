package com.fgmachines.rck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Small offline expert system for symptom-first troubleshooting.
 *
 * Scores indicate rule compatibility only; they are not failure probabilities.
 */
public final class GuidedDiagnosticsEngine {
    private GuidedDiagnosticsEngine() {}

    public static final class Hypothesis {
        public final String title;
        public final String safeAction;
        public final String serviceBoundary;

        Hypothesis(String title, String safeAction, String serviceBoundary) {
            this.title = title;
            this.safeAction = safeAction;
            this.serviceBoundary = serviceBoundary;
        }
    }

    public static final class Question {
        public final String prompt;
        final int[] yesDelta;
        final int[] noDelta;

        Question(String prompt, int[] yesDelta, int[] noDelta) {
            this.prompt = prompt;
            this.yesDelta = yesDelta;
            this.noDelta = noDelta;
        }
    }

    public static final class Profile {
        public final String id;
        public final String title;
        public final String brand;
        public final String category;
        public final String modelHint;
        public final String sourceTitle;
        public final String sourceUrl;
        public final List<Question> questions;
        public final List<Hypothesis> hypotheses;
        final int[] baseScores;

        Profile(String id, String title, String brand, String category, String modelHint,
                String sourceTitle, String sourceUrl, List<Question> questions,
                List<Hypothesis> hypotheses, int[] baseScores) {
            this.id = id;
            this.title = title;
            this.brand = brand;
            this.category = category;
            this.modelHint = modelHint;
            this.sourceTitle = sourceTitle;
            this.sourceUrl = sourceUrl;
            this.questions = Collections.unmodifiableList(questions);
            this.hypotheses = Collections.unmodifiableList(hypotheses);
            this.baseScores = baseScores;
        }
    }

    public static final class RankedCause {
        public final Hypothesis hypothesis;
        public final int compatibilityScore;

        RankedCause(Hypothesis hypothesis, int compatibilityScore) {
            this.hypothesis = hypothesis;
            this.compatibilityScore = compatibilityScore;
        }
    }

    private static final Profile SAMSUNG_DRAIN = new Profile(
            "samsung_washer_not_draining",
            "Samsung washer — does not drain",
            "Samsung", "Washer", "",
            "Samsung 5C/5E washing-machine troubleshooting",
            "https://www.samsung.com/sa_en/support/home-appliances/5c5e-error-code-on-samsung-washing-machine-troubleshooting-guide/",
            Arrays.asList(
                    new Question("Is the external drain hose visibly kinked, blocked, frozen, or routed incorrectly?",
                            new int[]{70, 0, 5}, new int[]{0, 8, 12}),
                    new Question("Does the user-accessible drain pump filter contain visible debris?",
                            new int[]{0, 75, 5}, new int[]{4, 0, 15}),
                    new Question("After the safe hose/filter checks, does the washer still fail to drain?",
                            new int[]{0, 0, 85}, new int[]{25, 25, 0})
            ),
            Arrays.asList(
                    new Hypothesis("Drain hose restriction or installation issue",
                            "Power the washer off, inspect the external drain hose, and correct only visible kinks/blockage or routing described in the manual.",
                            "Use service if the hose path is correct but drainage still fails."),
                    new Hypothesis("User-serviceable drain filter restriction",
                            "With the washer switched off and unplugged, clean only the accessible pump filter as described by Samsung.",
                            "Use service if the filter is clear and the symptom persists."),
                    new Hypothesis("Drain pump / internal electrical fault",
                            "Do not open internal mains-voltage compartments or energize exposed parts.",
                            "Qualified service is required if the fault persists after documented external checks.")
            ),
            new int[]{15, 15, 10}
    );

    private static final Profile SAMSUNG_FILL = new Profile(
            "samsung_washer_not_filling",
            "Samsung washer — does not fill / 4C / 4E / NF / NF1",
            "Samsung", "Washer", "",
            "Samsung 4C / 4E / NF / NF1 water-supply troubleshooting",
            "https://www.samsung.com/eg/support/home-appliances/resolve-4c-4e-nf-or-nf1-error-codes-on-your-samsung-washing-machine/",
            Arrays.asList(
                    new Question("Are the washer water taps closed or only partly open?",
                            new int[]{90, 5, 0, 0}, new int[]{0, 12, 12, 15}),
                    new Question("Is an external inlet hose visibly kinked, frozen, crushed, or blocked?",
                            new int[]{0, 90, 5, 0}, new int[]{8, 0, 12, 18}),
                    new Question("Is the user-serviceable inlet mesh filter visibly restricted by debris?",
                            new int[]{0, 5, 90, 0}, new int[]{5, 8, 0, 20}),
                    new Question("After the documented tap, hose, and mesh-filter checks, does the washer still fail to fill?",
                            new int[]{0, 0, 0, 95}, new int[]{25, 25, 25, 0})
            ),
            Arrays.asList(
                    new Hypothesis("Water supply tap / supply restriction",
                            "Stop the cycle and confirm the washer water taps are fully open.",
                            "Use service if normal supply is present but the washer still does not fill."),
                    new Hypothesis("External inlet-hose restriction",
                            "Switch the washer off and correct only safe, visible hose kinks or routing issues.",
                            "Do not dismantle internal valves or energized components."),
                    new Hypothesis("User-serviceable inlet mesh filter restriction",
                            "With the washer off, clean only the inlet mesh filter using the exact-model procedure.",
                            "Use service if the filter is clear and the symptom remains."),
                    new Hypothesis("Water inlet valve / pressure sensing / internal fault",
                            "Do not open mains-voltage compartments or energize exposed parts.",
                            "Qualified service is required after the documented external water-supply checks have passed.")
            ),
            new int[]{15, 15, 15, 8}
    );

    private static final Profile ROBOROCK_CHARGE = new Profile(
            "roborock_s7_not_charging",
            "Roborock S7 — does not charge / Error 13",
            "Roborock", "Robot Vacuum", "S7",
            "Roborock S7 official manual — Error 13",
            "https://support.roborock.com/hc/en-us/article_attachments/900008254623",
            Arrays.asList(
                    new Question("Are the robot or dock charging contacts visibly dirty?",
                            new int[]{80, 0, 5}, new int[]{0, 10, 12}),
                    new Question("Is the dock unpowered, loosely connected, or dead on a known-good wall outlet?",
                            new int[]{0, 85, 5}, new int[]{8, 0, 18}),
                    new Question("After dry-contact cleaning and dock power checks, does charging still fail?",
                            new int[]{0, 0, 90}, new int[]{25, 20, 0})
            ),
            Arrays.asList(
                    new Hypothesis("Dirty charging contacts",
                            "Power the robot down and wipe robot/dock charging contacts with a dry cloth.",
                            "Use service if contact cleaning does not restore charging."),
                    new Hypothesis("Dock power or cable problem",
                            "Reseat the dock cable and test the dock from a known-good wall outlet.",
                            "Use service if the dock still has no power or appears damaged."),
                    new Hypothesis("Dock / robot charging hardware fault",
                            "Do not open the battery, charger, or mains-powered dock.",
                            "Service support is required if Error 13 persists after basic contact and power checks.")
            ),
            new int[]{15, 15, 10}
    );

    private static final Profile ROBOROCK_BRUSH = new Profile(
            "roborock_s7_main_brush",
            "Roborock S7 — main brush jammed / Error 5",
            "Roborock", "Robot Vacuum", "S7",
            "Roborock S7 official manual — Error 5",
            "https://support.roborock.com/hc/en-us/article_attachments/900008254623",
            Arrays.asList(
                    new Question("Is hair, thread, or visible debris wrapped around the main brush?",
                            new int[]{90, 5, 0}, new int[]{0, 18, 18}),
                    new Question("After removing the user-serviceable brush, is debris trapped around an end cap or bearing?",
                            new int[]{5, 90, 0}, new int[]{8, 0, 25}),
                    new Question("After cleaning and correct reinstallation, does Error 5 return?",
                            new int[]{0, 0, 95}, new int[]{25, 25, 0})
            ),
            Arrays.asList(
                    new Hypothesis("Hair / debris wrapped around the main brush",
                            "Power the robot off, remove the normal user-serviceable main brush, and remove hair/thread/debris.",
                            "Use service if the error returns with a clean, freely moving brush."),
                    new Hypothesis("Brush end-cap / bearing restriction",
                            "Clean accessible debris from the brush ends and reinstall the brush correctly.",
                            "Do not disassemble the internal drive motor or gearbox."),
                    new Hypothesis("Main-brush drive / sensing fault",
                            "Stop repeated operation if the brush is clean but the error persists.",
                            "Roborock service is required for internal motor, drive, or sensing faults.")
            ),
            new int[]{15, 15, 8}
    );

    private static final Profile XIAOMI_CHARGE = new Profile(
            "xiaomi_vacuum_mop_charging",
            "Xiaomi Mi Robot Vacuum-Mop — charging error",
            "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop",
            "Xiaomi Global — Mi Robot Vacuum-Mop error code description",
            "https://www.mi.com/global/support/faq/details/KA-07588/",
            Arrays.asList(
                    new Question("Are the charging contacts visibly dusty or dirty?",
                            new int[]{80, 0, 5}, new int[]{0, 12, 12}),
                    new Question("Is the vacuum-mop not seated correctly on the dock?",
                            new int[]{0, 75, 5}, new int[]{5, 0, 18}),
                    new Question("After cleaning contacts and reseating the robot, does the charging error remain?",
                            new int[]{0, 0, 90}, new int[]{25, 20, 0})
            ),
            Arrays.asList(
                    new Hypothesis("Dirty charging contacts",
                            "Wipe charging contacts on the dock and robot with a dry cloth.",
                            "Use after-sales service if the charging error persists."),
                    new Hypothesis("Poor dock alignment / connection",
                            "Place the robot correctly on the dock and confirm normal contact.",
                            "Use service if correct seating does not restore charging."),
                    new Hypothesis("Charging system fault",
                            "Do not open the battery pack or mains-powered dock.",
                            "Contact Xiaomi after-sales service if the documented checks do not clear the error.")
            ),
            new int[]{15, 15, 10}
    );

    private static final Profile XIAOMI_MAIN_BRUSH = new Profile(
            "xiaomi_vacuum_mop_main_brush",
            "Xiaomi Mi Robot Vacuum-Mop — main brush / Error 4",
            "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop",
            "Xiaomi Global — Mi Robot Vacuum-Mop error code description",
            "https://www.mi.com/global/support/faq/details/KA-07588/",
            Arrays.asList(
                    new Question("Is hair, thread, or visible debris caught in the main brush?",
                            new int[]{90, 5, 0}, new int[]{0, 20, 18}),
                    new Question("Is visible debris trapped around the user-serviceable brush bearing/end cap?",
                            new int[]{5, 90, 0}, new int[]{8, 0, 25}),
                    new Question("After cleaning and correct reinstallation, does Error 4 return?",
                            new int[]{0, 0, 95}, new int[]{25, 25, 0})
            ),
            Arrays.asList(
                    new Hypothesis("Main-brush debris restriction",
                            "Power the robot off, remove the user-serviceable brush, and remove visible hair/debris.",
                            "Use service if the clean brush still triggers Error 4."),
                    new Hypothesis("Brush bearing/end-cap restriction",
                            "Clean accessible debris from the brush bearing/end cap and reinstall correctly.",
                            "Do not dismantle the internal drive mechanism."),
                    new Hypothesis("Brush drive / internal fault",
                            "Stop repeated attempts if the brush is clean and installed correctly.",
                            "Contact Xiaomi after-sales service for internal drive or sensing faults.")
            ),
            new int[]{15, 15, 8}
    );

    private static final Profile LG_NO_COOL = new Profile(
            "lg_ac_not_cooling",
            "LG air conditioner — runs but does not cool",
            "LG", "Air Conditioner", "",
            "LG Support — Cooling operation fails / No cold air",
            "https://www.lg.com/us/support/help-library/lg-air-conditioner-for-cooling-cooling-operation-fails-no-cold-air--20154629491668",
            Arrays.asList(
                    new Question("Is the unit in a mode other than normal Cooling, or is the target temperature set too high?",
                            new int[]{80, 0, 0, 5}, new int[]{0, 8, 8, 12}),
                    new Question("Is the user-accessible air filter visibly dusty or overdue for cleaning?",
                            new int[]{0, 80, 0, 5}, new int[]{5, 0, 8, 12}),
                    new Question("Is airflow around the outdoor unit visibly obstructed or poorly ventilated?",
                            new int[]{0, 0, 80, 5}, new int[]{4, 4, 0, 18}),
                    new Question("After correct Cooling mode, filter maintenance, and clear outdoor airflow, is cooling still weak or absent?",
                            new int[]{0, 0, 0, 90}, new int[]{20, 20, 20, 0})
            ),
            Arrays.asList(
                    new Hypothesis("Operating mode / target temperature",
                            "Select normal Cooling mode, set an appropriately low target temperature, and allow the unit time to cool as described by LG.",
                            "Request service if correct settings do not restore cooling."),
                    new Hypothesis("Restricted indoor airflow / dirty filter",
                            "Switch the unit off and clean only the user-serviceable filter according to the exact model manual.",
                            "Request service if airflow/cooling remains poor after filter maintenance."),
                    new Hypothesis("Outdoor-unit ventilation restriction",
                            "Remove safe, visible obstacles around the outdoor unit and ensure the ventilation area is open.",
                            "Do not open the outdoor unit; request service for internal fan/electrical issues."),
                    new Hypothesis("Refrigerant, compressor, sensor, or other internal fault",
                            "Do not open refrigerant lines or mains-voltage compartments.",
                            "Qualified HVAC service is required when cooling remains absent after the documented user checks.")
            ),
            new int[]{12, 12, 12, 8}
    );

    private static final Profile LG_FAN_ERROR = new Profile(
            "lg_ac_fan_error",
            "LG air conditioner — fan code CH10 / CH67 / E6 / EF",
            "LG", "Air Conditioner", "",
            "LG Support — CH10 / CH67 / E6 / EF",
            "https://www.lg.com/us/support/help-library/how-to-troubleshoot-error-codes-ch10-ch67-e6-and-ef-on-your-lg-air-conditioner-CT10000014-20155396792105",
            Arrays.asList(
                    new Question("Is there safe, visible external debris such as leaves obstructing an accessible fan/air path?",
                            new int[]{85, 0, 5}, new int[]{0, 15, 18}),
                    new Question("After switching the unit off, removing only safe external debris, and restoring normal power, does the code return?",
                            new int[]{0, 20, 90}, new int[]{30, 0, 0}),
                    new Question("Is the fan still not operating normally with no visible external obstruction?",
                            new int[]{0, 5, 95}, new int[]{15, 20, 0})
            ),
            Arrays.asList(
                    new Hypothesis("External fan / airflow obstruction",
                            "Switch the unit off and remove only safe, visible external debris without disassembly.",
                            "Do not reach into moving fans or open the outdoor/indoor unit."),
                    new Hypothesis("Temporary condition cleared after safe reset",
                            "Observe the unit after normal restart and stop if the code returns.",
                            "Repeated fan codes require service even if a restart temporarily clears them."),
                    new Hypothesis("Fan motor / fan electronics fault",
                            "Do not open fan-motor or mains-voltage compartments.",
                            "LG service is required if CH10/CH67/E6/EF persists without a safe external obstruction.")
            ),
            new int[]{12, 10, 10}
    );

    private static final Profile LG_COMMUNICATION_ERROR = new Profile(
            "lg_ac_communication_error",
            "LG air conditioner — communication code CH05 / CH53 / E0",
            "LG", "Air Conditioner", "",
            "LG Support — CH05 / CH53 / E0",
            "https://www.lg.com/levant_en/support/product-help/CT20158041-20155402843500",
            Arrays.asList(
                    new Question("Did the code appear immediately after installation, relocation, or electrical work?",
                            new int[]{0, 90, 20}, new int[]{15, 0, 18}),
                    new Question("After only the manufacturer-documented normal power reset, does the communication code return?",
                            new int[]{0, 25, 95}, new int[]{75, 0, 0}),
                    new Question("Are both indoor and outdoor units supplied normally but communication still fails?",
                            new int[]{0, 20, 90}, new int[]{20, 15, 10})
            ),
            Arrays.asList(
                    new Hypothesis("Transient power / communication condition",
                            "Use only the manufacturer-documented normal power reset and observe whether the code clears.",
                            "Do not repeatedly cycle power if the code continues."),
                    new Hypothesis("Installation / communication wiring issue",
                            "If the fault followed installation or relocation, stop troubleshooting at user level.",
                            "The installer or qualified HVAC technician should inspect communication wiring and installation."),
                    new Hypothesis("Indoor/outdoor control electronics or communication fault",
                            "Do not open mains-voltage control boards or probe energized wiring.",
                            "Qualified service is required when CH05/CH53/E0 returns after the documented reset.")
            ),
            new int[]{12, 10, 10}
    );

    private static final Profile LG_LOW_REFRIGERANT = new Profile(
            "lg_ac_low_refrigerant_codes",
            "LG air conditioner — CH32 / CH33 / CH36 / CH38 / F4",
            "LG", "Air Conditioner", "",
            "LG Support — low-refrigerant error codes",
            "https://www.lg.com/levant_en/support/product-help/CT20158041-20155402843500",
            Arrays.asList(
                    new Question("Is one of CH32, CH33, CH36, CH38, or F4 currently displayed?",
                            new int[]{90, 30}, new int[]{0, 15}),
                    new Question("Does the code return after a normal documented restart?",
                            new int[]{20, 95}, new int[]{20, 20})
            ),
            Arrays.asList(
                    new Hypothesis("Low-refrigerant / sealed-system condition indicated",
                            "Stop repeated resets and keep the unit available for service inspection.",
                            "Do not open refrigerant lines, valves, or attempt charging; qualified HVAC service is required."),
                    new Hypothesis("Code requires exact-model confirmation / HVAC service",
                            "Record the exact model and displayed code for the technician.",
                            "Sealed-system diagnosis and refrigerant work require qualified HVAC service.")
            ),
            new int[]{20, 15}
    );

    private static final List<Profile> PROFILES = Collections.unmodifiableList(Arrays.asList(
            SAMSUNG_DRAIN, SAMSUNG_FILL,
            ROBOROCK_CHARGE, ROBOROCK_BRUSH,
            XIAOMI_CHARGE, XIAOMI_MAIN_BRUSH,
            LG_NO_COOL, LG_FAN_ERROR, LG_COMMUNICATION_ERROR, LG_LOW_REFRIGERANT
    ));

    public static List<Profile> profiles() {
        return PROFILES;
    }

    public static Profile profile(String id) {
        if (id == null) return null;
        for (Profile profile : PROFILES) if (profile.id.equals(id)) return profile;
        return null;
    }

    public static List<RankedCause> evaluate(String profileId, List<Boolean> answers) {
        Profile profile = profile(profileId);
        if (profile == null) return Collections.emptyList();
        int[] scores = Arrays.copyOf(profile.baseScores, profile.baseScores.length);
        int count = Math.min(answers == null ? 0 : answers.size(), profile.questions.size());
        for (int i = 0; i < count; i++) {
            Question question = profile.questions.get(i);
            int[] delta = Boolean.TRUE.equals(answers.get(i)) ? question.yesDelta : question.noDelta;
            for (int h = 0; h < scores.length; h++) scores[h] += delta[h];
        }

        List<RankedCause> result = new ArrayList<>();
        for (int i = 0; i < profile.hypotheses.size(); i++) {
            result.add(new RankedCause(profile.hypotheses.get(i), scores[i]));
        }
        result.sort(Comparator.comparingInt((RankedCause x) -> x.compatibilityScore).reversed());
        return result;
    }
}
