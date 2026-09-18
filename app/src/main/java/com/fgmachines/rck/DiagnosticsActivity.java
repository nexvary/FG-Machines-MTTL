package com.fgmachines.rck;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

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
    private TextView resultText;
    private TextView historyText;
    private MaterialButton sourceButton;

    private FleetStore fleetStore;
    private ApplianceDiagnosticsStore diagnosticsStore;
    private HistoryStore historyStore;
    private ControllerHub controllerHub;

    private final List<FleetStore.DeviceRecord> devices = new ArrayList<>();
    private String activeMac = "";
    private ApplianceDiagnosticsCatalog.Match lastMatch;

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

        findViewById(R.id.diagnosticsBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.diagnosticsSearchButton).setOnClickListener(v -> runSearch());
        findViewById(R.id.diagnosticsBindButton).setOnClickListener(v -> saveBinding());
        findViewById(R.id.diagnosticsRecordButton).setOnClickListener(v -> recordIncident());
        sourceButton.setOnClickListener(v -> openCurrentSource());
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
        resultText = findViewById(R.id.diagnosticsResult);
        historyText = findViewById(R.id.diagnosticsHistory);
        sourceButton = findViewById(R.id.diagnosticsSourceButton);
        sourceButton.setEnabled(false);
        sourceButton.setAlpha(0.55f);
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
        diagnosticsStore.bind(activeMac, outlet, brand, category, model, nickname,
                System.currentTimeMillis());
        refreshBindingAndHistory();
        Snackbar.make(bindingStatus, R.string.diagnostics_bound, Snackbar.LENGTH_SHORT).show();
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

    private void openCurrentSource() {
        if (lastMatch == null || lastMatch.entry.sourceUrl.isEmpty()) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lastMatch.entry.sourceUrl)));
        } catch (RuntimeException error) {
            Snackbar.make(sourceButton, R.string.open_link_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
