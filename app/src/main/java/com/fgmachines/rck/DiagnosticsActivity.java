package com.fgmachines.rck;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class DiagnosticsActivity extends AppCompatActivity {
    public static final String EXTRA_MAC = "diagnostics_mac";

    private static final String PREFS = "fg_rck_settings";
    private static final String PREF_LANGUAGE = "language";
    private static final String[] LANGUAGE_TAGS = {"ar", "en", "tr", "es", "de"};

    private Spinner deviceSpinner;
    private Spinner outletSpinner;
    private TextInputEditText brandInput;
    private TextInputEditText categoryInput;
    private TextInputEditText modelInput;
    private TextInputEditText codeInput;
    private TextInputEditText symptomInput;
    private TextView bindingStatus;
    private TextView liveElectricalText;
    private MaterialButton refreshElectricalButton;
    private TextView resultText;
    private TextView historyText;
    private MaterialButton sourceButton;
    private Spinner guidedProfileSpinner;
    private TextView guidedProgressText;
    private TextView guidedQuestionText;
    private TextView guidedResultText;
    private MaterialButton guidedYesButton;
    private MaterialButton guidedNoButton;
    private MaterialButton guidedSourceButton;
    private MaterialButton captureDisplayButton;
    private MaterialButton captureLabelButton;
    private TextView captureResultText;

    private FleetStore fleetStore;
    private ApplianceDiagnosticsStore diagnosticsStore;
    private HistoryStore historyStore;
    private ControllerHub controllerHub;

    private final List<FleetStore.DeviceRecord> devices = new ArrayList<>();
    private final List<GuidedDiagnosticsEngine.Profile> guidedProfiles = new ArrayList<>();
    private final List<Boolean> guidedAnswers = new ArrayList<>();
    private String activeMac = "";
    private ApplianceDiagnosticsCatalog.Match lastMatch;
    private GuidedDiagnosticsEngine.Profile activeGuidedProfile;
    private int guidedQuestionIndex;
    private static final int CAPTURE_MODE_DISPLAY = 1;
    private static final int CAPTURE_MODE_LABEL = 2;
    private int captureMode;
    private int capturePending;
    private String capturedText = "";
    private final List<String> capturedBarcodes = new ArrayList<>();

    private final ActivityResultLauncher<Void> diagnosticCameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap == null) {
                    if (captureResultText != null) captureResultText.setText(R.string.camera_capture_cancelled);
                    return;
                }
                processDiagnosticImage(bitmap);
            });

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String language = prefs.getString(PREF_LANGUAGE, "");
        if (language == null || language.isEmpty()) {
            language = newBase.getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        if (!isSupportedLanguage(language)) language = "en";
        Locale locale = Locale.forLanguageTag(language);
        Configuration configuration = new Configuration(newBase.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        super.attachBaseContext(newBase.createConfigurationContext(configuration));
    }

    private static boolean isSupportedLanguage(String language) {
        for (String tag : LANGUAGE_TAGS) if (tag.equals(language)) return true;
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics);
        applySystemBarInsets();
        bindViews();

        fleetStore = new FleetStore(this);
        diagnosticsStore = new ApplianceDiagnosticsStore(this);
        historyStore = new HistoryStore(this);
        controllerHub = ControllerHub.get(this);

        configureDevices();
        configureOutlets();
        configureGuidedDiagnostics();

        findViewById(R.id.diagnosticsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.diagnosticsSearchButton).setOnClickListener(v -> runSearch());
        findViewById(R.id.diagnosticsBindButton).setOnClickListener(v -> saveBinding());
        findViewById(R.id.diagnosticsRecordButton).setOnClickListener(v -> recordIncident());
        sourceButton.setOnClickListener(v -> openCurrentSource());
        captureDisplayButton.setOnClickListener(v -> startDiagnosticCapture(CAPTURE_MODE_DISPLAY));
        captureLabelButton.setOnClickListener(v -> startDiagnosticCapture(CAPTURE_MODE_LABEL));
        refreshElectricalButton.setOnClickListener(v -> refreshLiveElectricalContext());
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.diagnosticsRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        deviceSpinner = findViewById(R.id.diagnosticsDeviceSpinner);
        outletSpinner = findViewById(R.id.diagnosticsOutletSpinner);
        brandInput = findViewById(R.id.diagnosticsBrandInput);
        categoryInput = findViewById(R.id.diagnosticsCategoryInput);
        modelInput = findViewById(R.id.diagnosticsModelInput);
        codeInput = findViewById(R.id.diagnosticsCodeInput);
        symptomInput = findViewById(R.id.diagnosticsSymptomInput);
        bindingStatus = findViewById(R.id.diagnosticsBindingStatus);
        liveElectricalText = findViewById(R.id.diagnosticsLiveElectrical);
        refreshElectricalButton = findViewById(R.id.diagnosticsRefreshElectricalButton);
        resultText = findViewById(R.id.diagnosticsResult);
        historyText = findViewById(R.id.diagnosticsHistory);
        sourceButton = findViewById(R.id.diagnosticsSourceButton);
        guidedProfileSpinner = findViewById(R.id.guidedProfileSpinner);
        guidedProgressText = findViewById(R.id.guidedProgressText);
        guidedQuestionText = findViewById(R.id.guidedQuestionText);
        guidedResultText = findViewById(R.id.guidedResultText);
        guidedYesButton = findViewById(R.id.guidedYesButton);
        guidedNoButton = findViewById(R.id.guidedNoButton);
        guidedSourceButton = findViewById(R.id.guidedSourceButton);
        captureDisplayButton = findViewById(R.id.captureDisplayButton);
        captureLabelButton = findViewById(R.id.captureLabelButton);
        captureResultText = findViewById(R.id.captureResultText);
        sourceButton.setEnabled(false);
        sourceButton.setAlpha(0.55f);
    }

    private void startDiagnosticCapture(int mode) {
        captureMode = mode;
        captureResultText.setText(R.string.camera_processing);
        diagnosticCameraLauncher.launch(null);
    }

    private void processDiagnosticImage(Bitmap bitmap) {
        capturedText = "";
        capturedBarcodes.clear();
        capturePending = 2;
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    capturedText = text == null ? "" : text.getText();
                    recognizer.close();
                    onCapturePartFinished();
                })
                .addOnFailureListener(error -> {
                    recognizer.close();
                    onCapturePartFinished();
                });

        BarcodeScanner scanner = BarcodeScanning.getClient();
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes != null) {
                        for (Barcode barcode : barcodes) {
                            String raw = barcode.getRawValue();
                            if (raw != null && !raw.trim().isEmpty()) capturedBarcodes.add(raw.trim());
                        }
                    }
                    scanner.close();
                    onCapturePartFinished();
                })
                .addOnFailureListener(error -> {
                    scanner.close();
                    onCapturePartFinished();
                });
    }

    private void onCapturePartFinished() {
        capturePending--;
        if (capturePending > 0) return;
        applyDiagnosticCapture();
    }

    private void applyDiagnosticCapture() {
        DiagnosticVisionParser.Result parsed =
                DiagnosticVisionParser.parse(capturedText, capturedBarcodes);

        boolean applied = false;
        ApplianceDiagnosticsStore.IdentityRecord localIdentity = null;

        if (captureMode == CAPTURE_MODE_LABEL) {
            for (String barcode : parsed.barcodes) {
                ApplianceDiagnosticsStore.IdentityRecord candidate =
                        diagnosticsStore.resolveIdentity(barcode);
                if (candidate != null) {
                    localIdentity = candidate;
                    break;
                }
            }
            if (localIdentity != null) {
                if (!localIdentity.brand.isEmpty()) brandInput.setText(localIdentity.brand);
                if (!localIdentity.category.isEmpty()) categoryInput.setText(localIdentity.category);
                modelInput.setText(localIdentity.model);
                applied = true;
            }
        }

        if (captureMode == CAPTURE_MODE_DISPLAY && !parsed.errorCode.isEmpty()) {
            codeInput.setText(parsed.errorCode);
            applied = true;
        }
        if (captureMode == CAPTURE_MODE_LABEL && localIdentity == null) {
            if (!parsed.brand.isEmpty()) {
                brandInput.setText(parsed.brand);
                applied = true;
            }
            if (!parsed.model.isEmpty()) {
                modelInput.setText(parsed.model);
                applied = true;
            }
        }

        StringBuilder summary = new StringBuilder();
        if (localIdentity != null) {
            summary.append(getString(R.string.camera_local_identity))
                    .append(": ")
                    .append(localIdentity.brand);
            if (!localIdentity.model.isEmpty()) {
                if (!localIdentity.brand.isEmpty()) summary.append(" ");
                summary.append(localIdentity.model);
            }
            summary.append("\n");
        }
        if (!parsed.brand.isEmpty()) {
            summary.append(getString(R.string.camera_detected_brand))
                    .append(": ").append(parsed.brand).append("\n");
        }
        if (!parsed.model.isEmpty()) {
            summary.append(getString(R.string.camera_detected_model))
                    .append(": ").append(parsed.model).append("\n");
        }
        if (!parsed.errorCode.isEmpty()) {
            summary.append(getString(R.string.camera_detected_code))
                    .append(": ").append(parsed.errorCode).append("\n");
        }
        if (!parsed.barcodes.isEmpty()) {
            summary.append(getString(R.string.camera_detected_barcodes))
                    .append(": ").append(android.text.TextUtils.join(", ", parsed.barcodes))
                    .append("\n")
                    .append(getString(R.string.camera_barcode_note))
                    .append("\n");
        }
        if (!parsed.recognizedText.isEmpty()) {
            String preview = parsed.recognizedText.replace("\n", " · ");
            if (preview.length() > 220) preview = preview.substring(0, 220) + "…";
            summary.append(getString(R.string.camera_ocr_text))
                    .append(": ").append(preview);
        }

        if (summary.length() == 0) {
            captureResultText.setText(R.string.camera_no_result);
        } else {
            captureResultText.setText(summary.toString().trim());
        }

        if (!applied) {
            Snackbar.make(captureResultText, R.string.camera_no_autofill, Snackbar.LENGTH_LONG).show();
        }
    }

    private void configureGuidedDiagnostics() {
        guidedProfiles.clear();
        guidedProfiles.addAll(GuidedDiagnosticsEngine.profiles());

        List<String> labels = new ArrayList<>();
        for (GuidedDiagnosticsEngine.Profile profile : guidedProfiles) labels.add(profile.title);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        guidedProfileSpinner.setAdapter(adapter);

        findViewById(R.id.guidedStartButton).setOnClickListener(v -> startGuidedDiagnosis());
        guidedYesButton.setOnClickListener(v -> answerGuidedQuestion(true));
        guidedNoButton.setOnClickListener(v -> answerGuidedQuestion(false));
        guidedSourceButton.setOnClickListener(v -> openGuidedSource());
        resetGuidedUi();
    }

    private void resetGuidedUi() {
        activeGuidedProfile = null;
        guidedQuestionIndex = 0;
        guidedAnswers.clear();
        guidedProgressText.setText(R.string.guided_not_started);
        guidedQuestionText.setText(R.string.guided_question_waiting);
        guidedResultText.setText(R.string.guided_result_waiting);
        guidedYesButton.setEnabled(false);
        guidedNoButton.setEnabled(false);
        guidedSourceButton.setEnabled(false);
        guidedSourceButton.setAlpha(0.55f);
    }

    private void startGuidedDiagnosis() {
        int position = guidedProfileSpinner.getSelectedItemPosition();
        if (position < 0 || position >= guidedProfiles.size()) {
            Snackbar.make(guidedQuestionText, R.string.guided_select_profile, Snackbar.LENGTH_LONG).show();
            return;
        }

        activeGuidedProfile = guidedProfiles.get(position);
        guidedQuestionIndex = 0;
        guidedAnswers.clear();

        brandInput.setText(activeGuidedProfile.brand);
        categoryInput.setText(activeGuidedProfile.category);
        if (!activeGuidedProfile.modelHint.isEmpty()) modelInput.setText(activeGuidedProfile.modelHint);
        symptomInput.setText(activeGuidedProfile.title);

        guidedResultText.setText(R.string.guided_result_waiting);
        guidedYesButton.setEnabled(true);
        guidedNoButton.setEnabled(true);
        guidedSourceButton.setEnabled(true);
        guidedSourceButton.setAlpha(1f);
        showGuidedQuestion();
    }

    private void showGuidedQuestion() {
        if (activeGuidedProfile == null) return;
        if (guidedQuestionIndex >= activeGuidedProfile.questions.size()) {
            finishGuidedDiagnosis();
            return;
        }
        guidedProgressText.setText(getString(
                R.string.guided_progress_format,
                guidedQuestionIndex + 1,
                activeGuidedProfile.questions.size()));
        guidedQuestionText.setText(activeGuidedProfile.questions.get(guidedQuestionIndex).prompt);
    }

    private void answerGuidedQuestion(boolean yes) {
        if (activeGuidedProfile == null
                || guidedQuestionIndex < 0
                || guidedQuestionIndex >= activeGuidedProfile.questions.size()) {
            return;
        }
        guidedAnswers.add(yes);
        guidedQuestionIndex++;
        showGuidedQuestion();
    }

    private void finishGuidedDiagnosis() {
        if (activeGuidedProfile == null) return;

        List<GuidedDiagnosticsEngine.RankedCause> causes =
                GuidedDiagnosticsEngine.evaluate(activeGuidedProfile.id, guidedAnswers);
        StringBuilder result = new StringBuilder();
        result.append(getString(R.string.guided_complete)).append("\n");
        result.append(getString(R.string.guided_score_note));

        int count = Math.min(3, causes.size());
        for (int i = 0; i < count; i++) {
            GuidedDiagnosticsEngine.RankedCause cause = causes.get(i);
            result.append("\n\n")
                    .append(i + 1).append(". ")
                    .append(cause.hypothesis.title)
                    .append("\n")
                    .append(getString(R.string.guided_safe_action_label))
                    .append(": ").append(cause.hypothesis.safeAction)
                    .append("\n")
                    .append(getString(R.string.guided_service_boundary_label))
                    .append(": ").append(cause.hypothesis.serviceBoundary)
                    .append("\n")
                    .append(getString(R.string.guided_compatibility_label))
                    .append(": ").append(cause.compatibilityScore);
        }

        guidedProgressText.setText(getString(
                R.string.guided_progress_done,
                activeGuidedProfile.questions.size()));
        guidedQuestionText.setText(R.string.guided_question_complete);
        guidedResultText.setText(result.toString());
        guidedYesButton.setEnabled(false);
        guidedNoButton.setEnabled(false);
    }

    private void openGuidedSource() {
        if (activeGuidedProfile == null || activeGuidedProfile.sourceUrl.isEmpty()) return;
        openExternal(activeGuidedProfile.sourceUrl, guidedSourceButton);
    }

    private void openExternal(String url, View anchor) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException error) {
            Snackbar.make(anchor, R.string.open_link_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    private void configureDevices() {
        devices.clear();
        devices.addAll(fleetStore.list());

        List<String> labels = new ArrayList<>();
        for (FleetStore.DeviceRecord record : devices) {
            String label = record.displayName();
            if (!record.room.isEmpty()) label += " · " + record.room;
            labels.add(label);
        }
        if (labels.isEmpty()) labels.add(getString(R.string.diagnostics_no_devices));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(adapter);

        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= devices.size()) {
                    activeMac = "";
                    refreshBindingAndHistory();
                    return;
                }
                activeMac = devices.get(position).mac;
                refreshBindingAndHistory();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
                activeMac = "";
                refreshBindingAndHistory();
            }
        });

        String requested = FleetStore.normalizeMac(getIntent().getStringExtra(EXTRA_MAC));
        int selected = 0;
        if (!requested.isEmpty()) {
            for (int i = 0; i < devices.size(); i++) {
                if (devices.get(i).mac.equalsIgnoreCase(requested)) {
                    selected = i;
                    break;
                }
            }
        }
        if (!devices.isEmpty()) deviceSpinner.setSelection(selected, false);
    }

    private void configureOutlets() {
        List<String> outlets = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            outlets.add(getString(R.string.diagnostics_outlet_format, i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, outlets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        outletSpinner.setAdapter(adapter);
        outletSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshBindingAndHistory();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private int selectedOutlet() {
        return Math.max(1, Math.min(4, outletSpinner.getSelectedItemPosition() + 1));
    }

    private void runSearch() {
        List<ApplianceDiagnosticsCatalog.Match> matches = ApplianceDiagnosticsCatalog.search(
                textOf(brandInput), textOf(categoryInput), textOf(modelInput),
                textOf(codeInput), textOf(symptomInput));

        if (matches.isEmpty()) {
            lastMatch = null;
            resultText.setText(R.string.diagnostics_no_match);
            sourceButton.setEnabled(false);
            sourceButton.setAlpha(0.55f);
            return;
        }

        lastMatch = matches.get(0);
        StringBuilder result = new StringBuilder();
        int count = Math.min(3, matches.size());
        for (int i = 0; i < count; i++) {
            ApplianceDiagnosticsCatalog.Match match = matches.get(i);
            ApplianceDiagnosticsCatalog.Entry entry = match.entry;
            if (i > 0) result.append("\n\n────────────────\n\n");
            result.append(entry.brand).append(" · ").append(entry.category);
            if (!entry.modelSeries.isEmpty()) result.append(" · ").append(entry.modelSeries);
            result.append("\n").append(getString(R.string.diagnostics_code_label))
                    .append(": ").append(entry.primaryCode());
            result.append("\n").append(getString(R.string.diagnostics_meaning_label))
                    .append(": ").append(entry.meaning);

            result.append("\n\n").append(getString(R.string.diagnostics_causes_label)).append(":");
            appendBullets(result, entry.causes);
            result.append("\n").append(getString(R.string.diagnostics_safe_checks_label)).append(":");
            appendBullets(result, entry.safeChecks);
            result.append("\n").append(getString(R.string.diagnostics_parts_label)).append(":");
            appendBullets(result, entry.likelyParts);

            if (!match.modelVerified) {
                result.append("\n\n⚠ ").append(getString(R.string.diagnostics_model_verify));
            }
            result.append("\n\n").append(getString(R.string.diagnostics_service_label))
                    .append(": ").append(entry.serviceRequiredWhen);
            result.append("\n").append(getString(R.string.diagnostics_scope_label))
                    .append(": ").append(entry.sourceScope);
        }
        resultText.setText(result.toString());
        sourceButton.setEnabled(true);
        sourceButton.setAlpha(1f);
    }

    private static void appendBullets(StringBuilder out, List<String> values) {
        for (String value : values) out.append("\n• ").append(value);
    }

    private void saveBinding() {
        if (activeMac.isEmpty()) {
            Snackbar.make(bindingStatus, R.string.diagnostics_select_device, Snackbar.LENGTH_LONG).show();
            return;
        }
        String brand = textOf(brandInput);
        String category = textOf(categoryInput);
        String model = textOf(modelInput);
        if (brand.isEmpty() && category.isEmpty() && model.isEmpty()) {
            Snackbar.make(bindingStatus, R.string.diagnostics_binding_requires_identity, Snackbar.LENGTH_LONG).show();
            return;
        }
        int outlet = selectedOutlet();
        String nickname = fleetStore.outletName(activeMac, outlet);
        long now = System.currentTimeMillis();
        diagnosticsStore.bind(activeMac, outlet, brand, category, model, nickname, now);

        int identifiersSaved = 0;
        if (captureMode == CAPTURE_MODE_LABEL && !model.isEmpty()) {
            for (String barcode : capturedBarcodes) {
                if (diagnosticsStore.saveIdentity(
                        barcode, brand, category, model, "confirmed_by_user", now)) {
                    identifiersSaved++;
                }
            }
        }

        refreshBindingAndHistory();
        if (identifiersSaved > 0) {
            Snackbar.make(bindingStatus,
                    getString(R.string.diagnostics_bound_with_identifiers, identifiersSaved),
                    Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(bindingStatus, R.string.diagnostics_bound, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void recordIncident() {
        if (activeMac.isEmpty()) {
            Snackbar.make(historyText, R.string.diagnostics_select_device, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (lastMatch == null) {
            Snackbar.make(historyText, R.string.diagnostics_search_before_record, Snackbar.LENGTH_LONG).show();
            return;
        }

        int outlet = selectedOutlet();
        double powerW = 0.0;
        double energyKWh = 0.0;
        String rckContext = getString(R.string.diagnostics_rck_unavailable);

        ControllerHub.DeviceState state = controllerHub.state(activeMac);
        if (state != null && state.telemetry != null) {
            for (MttlProtocol.OutletTelemetry telemetry : state.telemetry.outlets) {
                if (telemetry.channel == outlet) {
                    powerW = telemetry.powerW;
                    energyKWh = telemetry.energyKWh;
                    rckContext = String.format(Locale.US,
                            "relay=%s; overload=%s; overheat=%s; temp=%dC; event=%s",
                            telemetry.relayOn, telemetry.overloadProtection,
                            telemetry.overheatProtection, telemetry.temperatureC,
                            telemetry.eventCode == null ? "" : telemetry.eventCode);
                    break;
                }
            }
        }

        long now = System.currentTimeMillis();
        ApplianceDiagnosticsCatalog.Entry entry = lastMatch.entry;
        diagnosticsStore.recordIncident(
                activeMac, outlet, textOf(codeInput), textOf(symptomInput),
                entry.meaning, entry.sourceUrl, powerW, energyKWh, rckContext, now);
        historyStore.recordEvent(activeMac, outlet, "APPLIANCE_DIAGNOSTIC",
                entry.brand + " " + entry.modelSeries + " " + entry.primaryCode() +
                        " · " + entry.meaning + " · " + rckContext, now);
        refreshBindingAndHistory();
        Snackbar.make(historyText, R.string.diagnostics_recorded, Snackbar.LENGTH_SHORT).show();
    }

    private void refreshBindingAndHistory() {
        if (activeMac.isEmpty()) {
            bindingStatus.setText(R.string.diagnostics_binding_none);
            historyText.setText(R.string.diagnostics_history_empty);
            refreshLiveElectricalContext();
            return;
        }

        int outlet = selectedOutlet();
        ApplianceDiagnosticsStore.BindingRecord binding =
                diagnosticsStore.binding(activeMac, outlet);
        if (binding == null) {
            bindingStatus.setText(R.string.diagnostics_binding_none);
        } else {
            bindingStatus.setText(getString(R.string.diagnostics_binding_format,
                    binding.displayName(), outlet));
            if (!binding.brand.isEmpty()) brandInput.setText(binding.brand);
            if (!binding.category.isEmpty()) categoryInput.setText(binding.category);
            if (!binding.model.isEmpty()) modelInput.setText(binding.model);
        }

        refreshLiveElectricalContext();

        List<ApplianceDiagnosticsStore.IncidentRecord> incidents =
                diagnosticsStore.recent(activeMac, outlet, 8);
        if (incidents.isEmpty()) {
            historyText.setText(R.string.diagnostics_history_empty);
            return;
        }

        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        StringBuilder history = new StringBuilder();
        for (ApplianceDiagnosticsStore.IncidentRecord incident : incidents) {
            if (history.length() > 0) history.append("\n\n");
            history.append(format.format(new Date(incident.ts)));
            if (!incident.code.isEmpty()) history.append(" · ").append(incident.code);
            history.append("\n").append(incident.diagnosis);
            if (!incident.symptom.isEmpty()) {
                history.append("\n").append(getString(R.string.diagnostics_symptom_label))
                        .append(": ").append(incident.symptom);
            }
            history.append("\n")
                    .append(String.format(Locale.getDefault(), "%.1f W · %.3f kWh",
                            incident.powerW, incident.energyKWh));
            if (!incident.rckContext.isEmpty()) {
                history.append("\nRCK: ").append(incident.rckContext);
            }
        }
        historyText.setText(history.toString());
    }

    private void refreshLiveElectricalContext() {
        if (liveElectricalText == null) return;
        if (activeMac == null || activeMac.isEmpty() || controllerHub == null) {
            liveElectricalText.setText(R.string.diagnostics_live_unavailable);
            if (refreshElectricalButton != null) refreshElectricalButton.setEnabled(false);
            return;
        }

        ControllerHub.DeviceState state = controllerHub.state(activeMac);
        if (state == null || !state.connected || state.telemetry == null) {
            liveElectricalText.setText(R.string.diagnostics_live_device_offline);
            if (refreshElectricalButton != null) refreshElectricalButton.setEnabled(true);
            return;
        }

        int outlet = selectedOutlet();
        for (MttlProtocol.OutletTelemetry telemetry : state.telemetry.outlets) {
            if (telemetry.channel != outlet) continue;

            String relay = telemetry.relayOn
                    ? getString(R.string.diagnostics_live_on)
                    : getString(R.string.diagnostics_live_off);
            String overload = telemetry.overloadProtection
                    ? getString(R.string.diagnostics_live_warning)
                    : getString(R.string.diagnostics_live_normal);
            String overheat = telemetry.overheatProtection
                    ? getString(R.string.diagnostics_live_warning)
                    : getString(R.string.diagnostics_live_normal);
            String event = telemetry.eventCode == null || telemetry.eventCode.trim().isEmpty()
                    ? getString(R.string.diagnostics_live_none)
                    : telemetry.eventCode.trim();

            liveElectricalText.setText(getString(
                    R.string.diagnostics_live_format,
                    outlet,
                    relay,
                    telemetry.powerW,
                    telemetry.energyKWh,
                    telemetry.temperatureC,
                    overload,
                    overheat,
                    event));
            if (refreshElectricalButton != null) refreshElectricalButton.setEnabled(true);
            return;
        }

        liveElectricalText.setText(R.string.diagnostics_live_no_outlet_data);
        if (refreshElectricalButton != null) refreshElectricalButton.setEnabled(true);
    }

    private void openCurrentSource() {
        if (lastMatch == null || lastMatch.entry.sourceUrl.isEmpty()) return;
        openExternal(lastMatch.entry.sourceUrl, sourceButton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controllerHub != null) refreshLiveElectricalContext();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
