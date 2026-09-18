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
            String query = normalizeModel(model);
            String expected = normalizeModel(modelPattern);
            return !query.isEmpty() && !expected.isEmpty() &&
                    (query.contains(expected) || expected.contains(query));
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
            ),
            entry(
                    "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop", "Mi Robot Vacuum-Mop",
                    new String[]{"1"},
                    "Bumper / collision buffer needs attention.",
                    new String[]{"Collision buffer stuck", "Foreign object around the bumper"},
                    new String[]{
                            "Power or pause the robot before inspection.",
                            "Gently tap the bumper and remove visible foreign objects.",
                            "Move the vacuum-mop to a clear area and reactivate it."
                    },
                    new String[]{"Bumper assembly (service part if mechanically faulty)"},
                    new String[]{"error 1", "bumper", "collision buffer", "stuck bumper",
                            "المصد", "الصدام", "عالق"},
                    Severity.INFO,
                    "Use after-sales service if the bumper moves freely but the error repeatedly returns.",
                    "Xiaomi Global: Mi Robot Vacuum-Mop error code description",
                    "https://www.mi.com/global/support/faq/details/KA-07588/",
                    "Official Xiaomi guidance for Mi Robot Vacuum-Mop. Do not apply this code to other Xiaomi vacuum families without their own model documentation."
            ),
            entry(
                    "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop", "Mi Robot Vacuum-Mop",
                    new String[]{"4"},
                    "Main brush is obstructed.",
                    new String[]{"Hair or foreign object caught in the main brush", "Debris around brush bearings"},
                    new String[]{
                            "Power the robot off.",
                            "Remove the user-serviceable main brush.",
                            "Clean the bristles and bearings and reinstall the brush."
                    },
                    new String[]{"Main brush", "Brush bearing/end cap"},
                    new String[]{"error 4", "main brush", "brush stuck", "brush jammed",
                            "الفرشاة الرئيسية", "الفرشاة عالقة"},
                    Severity.INFO,
                    "Use service if the brush is clean and free-moving but the error continues.",
                    "Xiaomi Global: Mi Robot Vacuum-Mop error code description",
                    "https://www.mi.com/global/support/faq/details/KA-07588/",
                    "Official Xiaomi guidance for Mi Robot Vacuum-Mop only."
            ),
            entry(
                    "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop", "Mi Robot Vacuum-Mop",
                    new String[]{"5"},
                    "Side brush is obstructed.",
                    new String[]{"Hair or debris caught in the side brush"},
                    new String[]{
                            "Power the robot off.",
                            "Remove and clean the user-serviceable side brush."
                    },
                    new String[]{"Side brush"},
                    new String[]{"error 5", "side brush", "brush stuck", "الفرشاة الجانبية"},
                    Severity.INFO,
                    "Use service if the side brush is clean and turns freely but the error continues.",
                    "Xiaomi Global: Mi Robot Vacuum-Mop error code description",
                    "https://www.mi.com/global/support/faq/details/KA-07588/",
                    "Official Xiaomi guidance for Mi Robot Vacuum-Mop only."
            ),
            entry(
                    "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop", "Mi Robot Vacuum-Mop",
                    new String[]{"8"},
                    "Dust compartment or filter is not detected correctly.",
                    new String[]{"Dust compartment not seated", "Filter not installed correctly", "Filter fault"},
                    new String[]{
                            "Power or pause the robot.",
                            "Remove and reinstall the dust compartment and filter.",
                            "Confirm both parts are correctly seated."
                    },
                    new String[]{"Dust compartment", "Filter"},
                    new String[]{"error 8", "dust compartment", "filter", "bin error",
                            "حاوية الغبار", "الفلتر"},
                    Severity.INFO,
                    "Use after-sales service if correct installation and a known-good filter do not clear the error.",
                    "Xiaomi Global: Mi Robot Vacuum-Mop error code description",
                    "https://www.mi.com/global/support/faq/details/KA-07588/",
                    "Official Xiaomi guidance for Mi Robot Vacuum-Mop only."
            ),
            entry(
                    "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop", "Mi Robot Vacuum-Mop",
                    new String[]{"10"},
                    "Charging error.",
                    new String[]{"Dirty charging contacts", "Poor dock contact or seating", "Charging hardware fault"},
                    new String[]{
                            "Power or pause the robot.",
                            "Wipe charging contacts on the dock and robot with a dry cloth.",
                            "Place the vacuum-mop correctly on the charging dock and retry."
                    },
                    new String[]{"Charging contacts", "Charging dock"},
                    new String[]{"error 10", "charging error", "not charging", "لا تشحن", "لا يشحن"},
                    Severity.WARNING,
                    "Use after-sales service if cleaning the contacts and reseating the robot does not clear the error.",
                    "Xiaomi Global: Mi Robot Vacuum-Mop error code description",
                    "https://www.mi.com/global/support/faq/details/KA-07588/",
                    "Official Xiaomi guidance for Mi Robot Vacuum-Mop only."
            ),
            entry(
                    "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop", "Mi Robot Vacuum-Mop",
                    new String[]{"11"},
                    "Battery temperature is outside the normal operating range.",
                    new String[]{"Battery too hot", "Battery too cold"},
                    new String[]{
                            "Stop use and allow the robot to return to normal room temperature.",
                            "Retry only after the battery temperature has normalized."
                    },
                    new String[]{"Battery pack (service part)"},
                    new String[]{"error 11", "battery temperature", "too hot", "too cold",
                            "حرارة البطارية", "البطارية ساخنة"},
                    Severity.WARNING,
                    "Do not open the battery pack. Use after-sales service if the warning persists at normal room temperature.",
                    "Xiaomi Global: Mi Robot Vacuum-Mop error code description",
                    "https://www.mi.com/global/support/faq/details/KA-07588/",
                    "Official Xiaomi guidance for Mi Robot Vacuum-Mop only."
            ),
            entry(
                    "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop", "Mi Robot Vacuum-Mop",
                    new String[]{"13"},
                    "Visual navigation sensor requires cleaning.",
                    new String[]{"Visual navigation sensor dirty or obstructed"},
                    new String[]{
                            "Power or pause the robot.",
                            "Wipe the visual navigation sensor carefully, then reactivate the vacuum-mop."
                    },
                    new String[]{"Visual navigation sensor (service part if faulty)"},
                    new String[]{"error 13", "navigation sensor", "visual sensor", "navigation error",
                            "حساس الملاحة", "مستشعر الملاحة"},
                    Severity.INFO,
                    "Use after-sales service if the sensor is clean but the error returns.",
                    "Xiaomi Global: Mi Robot Vacuum-Mop error code description",
                    "https://www.mi.com/global/support/faq/details/KA-07588/",
                    "Official Xiaomi guidance for Mi Robot Vacuum-Mop only."
            ),
            entry(
                    "Xiaomi", "Robot Vacuum", "Mi Robot Vacuum-Mop", "Mi Robot Vacuum-Mop",
                    new String[]{"14"},
                    "Internal error; Xiaomi advises a system reset.",
                    new String[]{"Internal controller or sensor fault"},
                    new String[]{
                            "Use the documented system-reset procedure for the exact model.",
                            "Retry normal operation after the reset."
                    },
                    new String[]{"Internal electronics (service only)"},
                    new String[]{"error 14", "internal error", "system reset", "خطأ داخلي"},
                    Severity.SERVICE,
                    "If the error remains after the documented reset, contact Xiaomi after-sales service.",
                    "Xiaomi Global: Mi Robot Vacuum-Mop error code description",
                    "https://www.mi.com/global/support/faq/details/KA-07588/",
                    "Official Xiaomi guidance for Mi Robot Vacuum-Mop only."
            ),
            entry(
                    "LG", "Air Conditioner", "LG air conditioners covered by CH10/CH67/E6/EF support guidance", "*",
                    new String[]{"CH10", "CH67", "E6", "EF"},
                    "LG identifies these codes with an indoor or outdoor fan motor problem.",
                    new String[]{"Visible debris obstructing an exposed fan", "Fan motor or related electrical fault"},
                    new String[]{
                            "Switch the air conditioner off before any visual inspection.",
                            "Remove only safe, visible external debris such as leaves if accessible without disassembly.",
                            "Restore power and check whether the code returns."
                    },
                    new String[]{"Fan motor", "Fan assembly", "Control electronics"},
                    new String[]{"fan error", "fan motor", "fan not running", "مروحة", "المروحة لا تعمل"},
                    Severity.SERVICE,
                    "If the code persists, stop using the unit and contact LG support. Do not disassemble or replace internal parts yourself.",
                    "LG Support: CH10 / CH67 / E6 / EF",
                    "https://www.lg.com/us/support/help-library/how-to-troubleshoot-error-codes-ch10-ch67-e6-and-ef-on-your-lg-air-conditioner-CT10000014-20155396792105",
                    "Official LG air-conditioner guidance. Error applicability varies by product type/model; confirm the exact model documentation."
            ),
            entry(
                    "LG", "Air Conditioner", "LG air conditioners covered by CH05/CH53/E0 guidance", "*",
                    new String[]{"CH05", "CH53", "E0"},
                    "Communication problem between the indoor and outdoor units.",
                    new String[]{"Temporary electrical/power instability", "Indoor/outdoor communication wiring or electronics fault"},
                    new String[]{
                            "Switch the air conditioner off using its normal control.",
                            "Perform only the manufacturer-documented power reset.",
                            "If the code appeared just after installation, contact the installer."
                    },
                    new String[]{"Communication wiring", "Indoor/outdoor control electronics"},
                    new String[]{"communication error", "indoor outdoor communication", "اتصال الوحدة الداخلية", "اتصال الوحدة الخارجية"},
                    Severity.SERVICE,
                    "If the code returns after a documented reset, service or installer inspection is required.",
                    "LG Support: CH05 / CH53 / E0",
                    "https://www.lg.com/levant_en/support/product-help/CT20158041-20155402843500",
                    "Official LG air-conditioner guidance; confirm exact model/type because code applicability can differ."
            ),
            entry(
                    "LG", "Air Conditioner", "LG air conditioners covered by CH32/CH33/CH36/CH38/F4 guidance", "*",
                    new String[]{"CH32", "CH33", "CH36", "CH38", "F4"},
                    "LG associates these codes with a low-refrigerant condition.",
                    new String[]{"Low refrigerant charge", "Possible sealed-system leak or installation issue"},
                    new String[]{
                            "Stop repeated resets if the code continues.",
                            "Do not open refrigerant lines or valves.",
                            "Contact the installer or qualified HVAC service."
                    },
                    new String[]{"Refrigerant circuit", "Sealed-system components"},
                    new String[]{"low refrigerant", "not cooling", "weak cooling", "نقص فريون", "لا يبرد", "تبريد ضعيف"},
                    Severity.SERVICE,
                    "Qualified HVAC service is required; refrigerant circuits are sealed/pressurized systems.",
                    "LG Support: CH32 / CH33 / CH36 / CH38 / F4",
                    "https://www.lg.com/levant_en/support/product-help/CT20158041-20155402843500",
                    "Official LG air-conditioner guidance; refrigerant diagnosis and service must be performed by qualified personnel."
            ),
            entry(
                    "LG", "Air Conditioner", "General LG cooling troubleshooting", "*",
                    new String[]{},
                    "Cooling is weak or no cold air is produced.",
                    new String[]{
                            "Incorrect operating mode or target temperature",
                            "Dirty user-serviceable filter restricting airflow",
                            "Outdoor-unit area poorly ventilated or obstructed",
                            "Internal or sealed-system fault if safe checks do not resolve the symptom"
                    },
                    new String[]{
                            "Select normal Cooling mode and verify the target temperature.",
                            "Clean only the user-serviceable filter according to the exact model manual.",
                            "Remove safe, visible obstacles around the outdoor unit and ensure its ventilation area is open."
                    },
                    new String[]{"Air filter", "Internal HVAC parts require service diagnosis"},
                    new String[]{"not cooling", "no cold air", "weak cooling", "runs but not cold",
                            "لا يبرد", "التكييف لا يبرد", "تبريد ضعيف"},
                    Severity.WARNING,
                    "If cooling remains weak after the documented user checks, request qualified service; do not open refrigerant or mains-voltage sections.",
                    "LG Support: Cooling operation fails / No cold air",
                    "https://www.lg.com/us/support/help-library/lg-air-conditioner-for-cooling-cooling-operation-fails-no-cold-air--20154629491668",
                    "General LG troubleshooting across models; exact filter access and service diagnosis depend on the specific model."
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

    static String normalizeModel(String value) {
        return normalize(value).replaceAll("[^a-z0-9]+", "");
    }

    private static boolean containsNormalized(String source, String query) {
        return !query.isEmpty() && normalize(source).contains(query);
    }

    private static boolean containsEither(String source, String query) {
        String a = normalize(source);
        return !a.isEmpty() && !query.isEmpty() && (a.contains(query) || query.contains(a));
    }
}
