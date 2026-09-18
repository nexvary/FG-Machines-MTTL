package com.fgmachines.rck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Offline, source-aware appliance diagnostic knowledge.
 *
 * Entries are intentionally scoped to an official source. A code is never treated as
 * universal across brands or product families.
 */
public final class ApplianceDiagnosticsCatalog {
    private ApplianceDiagnosticsCatalog() {}

    public enum Severity {
        INFO,
        WARNING,
        SERVICE
    }

    public static final class Entry {
        public final String brand;
        public final String category;
        public final String modelSeries;
        public final String modelPattern;
        public final List<String> codes;
        public final String meaning;
        public final List<String> causes;
        public final List<String> safeChecks;
        public final List<String> likelyParts;
        public final List<String> symptomTags;
        public final Severity severity;
        public final String serviceRequiredWhen;
        public final String sourceTitle;
        public final String sourceUrl;
        public final String sourceScope;

        Entry(String brand, String category, String modelSeries, String modelPattern,
              List<String> codes, String meaning, List<String> causes,
              List<String> safeChecks, List<String> likelyParts, List<String> symptomTags,
              Severity severity, String serviceRequiredWhen, String sourceTitle,
              String sourceUrl, String sourceScope) {
            this.brand = brand;
            this.category = category;
            this.modelSeries = modelSeries;
            this.modelPattern = modelPattern;
            this.codes = Collections.unmodifiableList(codes);
            this.meaning = meaning;
            this.causes = Collections.unmodifiableList(causes);
            this.safeChecks = Collections.unmodifiableList(safeChecks);
            this.likelyParts = Collections.unmodifiableList(likelyParts);
            this.symptomTags = Collections.unmodifiableList(symptomTags);
            this.severity = severity;
            this.serviceRequiredWhen = serviceRequiredWhen;
            this.sourceTitle = sourceTitle;
            this.sourceUrl = sourceUrl;
            this.sourceScope = sourceScope;
        }

        public String primaryCode() {
            return codes.isEmpty() ? "" : codes.get(0);
        }

        public boolean hasModelRestriction() {
            return modelPattern != null && !modelPattern.trim().isEmpty() && !"*".equals(modelPattern);
        }

        public boolean modelMatches(String model) {
            if (!hasModelRestriction()) return true;
            String query = normalize(model);
            return !query.isEmpty() && query.contains(normalize(modelPattern));
        }
    }

    public static final class Match {
        public final Entry entry;
        public final int score;
        public final boolean modelVerified;

        Match(Entry entry, int score, boolean modelVerified) {
            this.entry = entry;
            this.score = score;
            this.modelVerified = modelVerified;
        }
    }

    private static Entry entry(String brand, String category, String modelSeries, String modelPattern,
                               String[] codes, String meaning, String[] causes,
                               String[] safeChecks, String[] likelyParts, String[] symptomTags,
                               Severity severity, String serviceRequiredWhen, String sourceTitle,
                               String sourceUrl, String sourceScope) {
        return new Entry(brand, category, modelSeries, modelPattern,
                Arrays.asList(codes), meaning, Arrays.asList(causes), Arrays.asList(safeChecks),
                Arrays.asList(likelyParts), Arrays.asList(symptomTags), severity,
                serviceRequiredWhen, sourceTitle, sourceUrl, sourceScope);
    }

    private static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            entry(
                    "Samsung", "Washer",
                    "Samsung washers covered by official 4C/4E/NF/NF1 support guidance", "*",
                    new String[]{"4C", "4E", "NF", "NF1"},
                    "The washer is not receiving enough water to run the cycle.",
                    new String[]{
                            "Water tap closed or supply restricted",
                            "Inlet hose kinked, blocked, frozen, or incorrectly connected",
                            "Inlet mesh filter restricted",
                            "Low supply pressure or an internal inlet-valve fault"
                    },
                    new String[]{
                            "Stop the cycle and switch the washer off before handling hoses.",
                            "Confirm the water taps are fully open.",
                            "Inspect accessible inlet hoses for kinks or obvious blockage.",
                            "Clean only the user-serviceable inlet mesh filter described by the manufacturer."
                    },
                    new String[]{"Inlet hose", "Inlet mesh filter", "Water inlet valve (service part)"},
                    new String[]{"4c", "4e", "nf", "nf1", "not filling", "no water",
                            "لا تسحب المياه", "لا يدخل الماء", "لا تمتلئ بالماء"},
                    Severity.WARNING,
                    "Use qualified service if the code returns after the documented external checks, or if an internal valve/electrical fault is suspected.",
                    "Samsung: Resolve 4C / 4E / NF / NF1 error codes",
                    "https://www.samsung.com/eg/support/home-appliances/resolve-4c-4e-nf-or-nf1-error-codes-on-your-samsung-washing-machine/",
                    "Official Samsung washer support guidance. Confirm the exact model manual before ordering or replacing internal parts."
            ),
            entry(
                    "Samsung", "Washer",
                    "Samsung washers covered by official 5C/5E drainage guidance", "*",
                    new String[]{"5C", "5E"},
                    "The washer has detected a drainage problem.",
                    new String[]{
                            "Drain pump filter blocked by debris",
                            "Drain hose kinked, blocked, frozen, or incorrectly installed",
                            "Drain pump or related electrical fault"
                    },
                    new String[]{
                            "Switch the washer off and unplug it before accessible drain maintenance.",
                            "Inspect the external drain hose for kinks or blockage.",
                            "Clean the user-accessible drain pump filter only as described by Samsung.",
                            "Check that the drain hose installation height and routing match the manual."
                    },
                    new String[]{"Drain filter", "Drain hose", "Drain pump (service part)"},
                    new String[]{"5c", "5e", "not draining", "water remains", "drain problem",
                            "لا تصرف المياه", "المياه لا تخرج", "مياه داخل الغسالة"},
                    Severity.WARNING,
                    "Use qualified service if the machine still cannot drain after the manufacturer-approved external checks.",
                    "Samsung: 5C/5E washing-machine troubleshooting",
                    "https://www.samsung.com/sa_en/support/home-appliances/5c5e-error-code-on-samsung-washing-machine-troubleshooting-guide/",
                    "Official Samsung washing-machine support guidance. Model-specific hose/filter layout must be confirmed in the exact model manual."
            ),
            entry(
                    "Roborock", "Robot Vacuum", "S7", "S7",
                    new String[]{"13"},
                    "Charging error.",
                    new String[]{
                            "Dirty or oxidized charging contacts",
                            "Dock power/cable connection problem",
                            "Dock or charging hardware fault"
                    },
                    new String[]{
                            "Clean the robot and dock charging contacts with a dry cloth.",
                            "Confirm the dock cable is fully seated and the dock has power.",
                            "Try a known-good wall outlet.",
                            "Unplug the dock briefly, reconnect it, then retry charging."
                    },
                    new String[]{"Charging contacts", "Charging dock", "Dock power cable"},
                    new String[]{"13", "charging error", "not charging", "won't charge",
                            "لا تشحن", "لا يشحن", "خطأ الشحن"},
                    Severity.WARNING,
                    "Use service support if Error 13 persists after contact cleaning and basic dock power checks.",
                    "Roborock S7 manual / official support: Error 13",
                    "https://support.roborock.com/hc/en-us/article_attachments/900008254623",
                    "Roborock S7 family. Do not apply this entry to another Roborock model unless its official manual confirms the same code."
            ),
            entry(
                    "Roborock", "Robot Vacuum", "S7", "S7",
                    new String[]{"5"},
                    "Main brush jammed.",
                    new String[]{
                            "Hair, thread, or debris wrapped around the main brush",
                            "Debris trapped around the brush bearings"
                    },
                    new String[]{
                            "Power the robot off.",
                            "Remove the main brush using the normal user-serviceable cover.",
                            "Remove hair and debris from the brush and bearings, then reinstall it."
                    },
                    new String[]{"Main brush", "Main-brush bearing/end cap"},
                    new String[]{"5", "main brush", "brush jammed", "brush stuck",
                            "الفرشاة الرئيسية", "الفرشاة عالقة"},
                    Severity.INFO,
                    "Use service support if the brush rotates freely by hand after cleaning but the error repeatedly returns.",
                    "Roborock S7 manual: Error 5",
                    "https://support.roborock.com/hc/en-us/article_attachments/900008254623",
                    "Roborock S7 family only."
            )
    ));

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static List<Match> search(String brand, String category, String model,
                                     String code, String symptom) {
        String qBrand = normalize(brand);
        String qCategory = normalize(category);
        String qModel = normalize(model);
        String qCode = normalizeCode(code);
        String qSymptom = normalize(symptom);

        List<Match> matches = new ArrayList<>();
        for (Entry item : ENTRIES) {
            int score = 0;

            if (!qBrand.isEmpty()) {
                if (!normalize(item.brand).contains(qBrand) && !qBrand.contains(normalize(item.brand))) continue;
                score += 10;
            }
            if (!qCategory.isEmpty()) {
                String itemCategory = normalize(item.category);
                if (!itemCategory.contains(qCategory) && !qCategory.contains(itemCategory)) continue;
                score += 6;
            }

            boolean codeMatched = qCode.isEmpty();
            if (!qCode.isEmpty()) {
                for (String alias : item.codes) {
                    if (normalizeCode(alias).equals(qCode)) {
                        codeMatched = true;
                        score += 30;
                        break;
                    }
                }
                if (!codeMatched) continue;
            }

            boolean modelVerified = !item.hasModelRestriction();
            if (!qModel.isEmpty()) {
                if (item.modelMatches(qModel)) {
                    modelVerified = true;
                    score += 18;
                } else if (item.hasModelRestriction()) {
                    continue;
                } else {
                    score += 2;
                }
            }

            if (!qSymptom.isEmpty()) {
                boolean symptomMatched = containsNormalized(item.meaning, qSymptom);
                for (String tag : item.symptomTags) {
                    if (containsEither(tag, qSymptom)) {
                        symptomMatched = true;
                        score += 8;
                    }
                }
                if (!symptomMatched && qCode.isEmpty()) continue;
            }

            if (qBrand.isEmpty() && qCategory.isEmpty() && qCode.isEmpty()
                    && qModel.isEmpty() && qSymptom.isEmpty()) {
                continue;
            }
            matches.add(new Match(item, score, modelVerified));
        }
        matches.sort((left, right) -> Integer.compare(right.score, left.score));
        return matches;
    }

    static String normalizeCode(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "");
    }

    static String normalize(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsNormalized(String source, String query) {
        return !query.isEmpty() && normalize(source).contains(query);
    }

    private static boolean containsEither(String source, String query) {
        String a = normalize(source);
        return !a.isEmpty() && !query.isEmpty() && (a.contains(query) || query.contains(a));
    }
}
