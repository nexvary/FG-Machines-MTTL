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

    private static final List<Profile> PROFILES = Collections.unmodifiableList(Arrays.asList(
            SAMSUNG_DRAIN, ROBOROCK_CHARGE, XIAOMI_CHARGE, LG_NO_COOL
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
