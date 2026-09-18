package com.fgmachines.rck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative parsing of OCR/barcode capture results. */
public final class DiagnosticVisionParser {
    private DiagnosticVisionParser() {}

    private static final Pattern MODEL_PATTERN = Pattern.compile(
            "(?i)\\b(?:model|model\\s*no|model\\s*number|mod\\.?)\\s*[:#-]?\\s*([A-Z0-9][A-Z0-9._/-]{2,})");
    private static final Pattern EXPLICIT_CODE_PATTERN = Pattern.compile(
            "(?i)(?:error|code|fault|خطأ|كود|رمز)\\s*[:#-]?\\s*(CH\\d{1,3}|[A-Z]{1,2}\\d{1,3}(?:\\.\\d)?|\\d{1,2}[A-Z])");
    private static final Pattern STANDALONE_CODE_PATTERN = Pattern.compile(
            "(?i)^(CH\\d{1,3}|[A-Z]{1,2}\\d{1,3}(?:\\.\\d)?|\\d{1,2}[A-Z])$");

    public static final class Result {
        public final String brand;
        public final String model;
        public final String errorCode;
        public final String recognizedText;
        public final List<String> barcodes;

        Result(String brand, String model, String errorCode,
               String recognizedText, List<String> barcodes) {
            this.brand = brand;
            this.model = model;
            this.errorCode = errorCode;
            this.recognizedText = recognizedText == null ? "" : recognizedText;
            this.barcodes = Collections.unmodifiableList(new ArrayList<>(barcodes));
        }
    }

    public static Result parse(String text, List<String> barcodeValues) {
        String safeText = text == null ? "" : text.trim();
        List<String> safeBarcodes = barcodeValues == null
                ? Collections.emptyList() : barcodeValues;

        String brand = detectBrand(safeText);
        String model = "";
        Matcher modelMatcher = MODEL_PATTERN.matcher(safeText);
        if (modelMatcher.find()) model = modelMatcher.group(1).trim();

        String errorCode = "";
        Matcher explicit = EXPLICIT_CODE_PATTERN.matcher(safeText);
        if (explicit.find()) {
            errorCode = normalizeCode(explicit.group(1));
        } else {
            String oneLine = safeText.replace("\n", " ").trim();
            if (oneLine.length() <= 12) {
                Matcher standalone = STANDALONE_CODE_PATTERN.matcher(oneLine);
                if (standalone.matches()) errorCode = normalizeCode(standalone.group(1));
            }
        }

        return new Result(brand, model, errorCode, safeText, safeBarcodes);
    }

    private static String detectBrand(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (value.contains("samsung")) return "Samsung";
        if (value.contains("roborock")) return "Roborock";
        if (value.contains("xiaomi") || value.contains("mijia") || value.contains("mi robot")) return "Xiaomi";
        if (value.contains("dreame")) return "Dreame";
        if (value.contains("lg electronics") || Pattern.compile("(?i)(^|\\W)lg($|\\W)").matcher(text).find()) return "LG";
        if (value.contains("bosch")) return "Bosch";
        if (value.contains("whirlpool")) return "Whirlpool";
        if (value.contains("electrolux")) return "Electrolux";
        return "";
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(" ", "");
    }
}
