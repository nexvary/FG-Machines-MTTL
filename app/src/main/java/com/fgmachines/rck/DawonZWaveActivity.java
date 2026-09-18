package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DawonZWaveActivity extends AppCompatActivity {
    private static final String PREFS = "fg_rck_dawon_zwave";
    private static final String PREF_APP_SETTINGS = "fg_rck_settings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_ENDPOINT = "ha_endpoint";
    private static final String PREF_TOKEN = "ha_token";
    private static final String PREF_SWITCH_ENTITY = "switch_entity";
    private static final String[] LANGUAGE_TAGS = {"ar", "en", "tr", "es", "de"};

    private TextInputEditText endpointInput;
    private TextInputEditText tokenInput;
    private MaterialButton connectButton;
    private MaterialButton refreshButton;
    private MaterialButton onButton;
    private MaterialButton offButton;
    private Spinner entitySpinner;
    private TextView statusText;
    private TextView fingerprintText;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<HomeAssistantZWaveClient.SwitchEntity> switches = new ArrayList<>();
    private volatile boolean gatewayReady;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREF_APP_SETTINGS, Context.MODE_PRIVATE);
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
        setContentView(R.layout.activity_dawon_zwave);
        applySystemBarInsets();
        bindViews();
        restoreSettings();
        configureUi();
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.dawonZWaveRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void bindViews() {
        endpointInput = findViewById(R.id.dawonGatewayEndpointInput);
        tokenInput = findViewById(R.id.dawonGatewayTokenInput);
        connectButton = findViewById(R.id.dawonGatewayConnectButton);
        refreshButton = findViewById(R.id.dawonRefreshButton);
        onButton = findViewById(R.id.dawonOnButton);
        offButton = findViewById(R.id.dawonOffButton);
        entitySpinner = findViewById(R.id.dawonSwitchSpinner);
        statusText = findViewById(R.id.dawonGatewayStatus);
        fingerprintText = findViewById(R.id.dawonFingerprint);
    }

    private void restoreSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        endpointInput.setText(prefs.getString(PREF_ENDPOINT, ""));
        tokenInput.setText(prefs.getString(PREF_TOKEN, ""));
        setControlEnabled(false);
    }

    private void configureUi() {
        findViewById(R.id.dawonBackButton).setOnClickListener(v -> finish());
        connectButton.setOnClickListener(v -> connectAndDiscover());
        refreshButton.setOnClickListener(v -> refreshSelectedState());
        onButton.setOnClickListener(v -> setSelectedSwitch(true));
        offButton.setOnClickListener(v -> setSelectedSwitch(false));

        entitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= switches.size()) {
                    setControlEnabled(false);
                    return;
                }
                saveSelectedEntity(switches.get(position).entityId);
                setControlEnabled(gatewayReady);
                renderState(switches.get(position).entityId, switches.get(position).state,
                        switches.get(position).friendlyName);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
                setControlEnabled(false);
            }
        });

        HardwareCatalog.Profile profile = HardwareCatalog.DAWON_MTD_01;
        fingerprintText.setText(getString(R.string.dawon_fingerprint_format,
                profile.displayModel,
                profile.zwaveManufacturerId,
                profile.zwaveProductTypeId,
                profile.zwaveProductId,
                profile.zwaveFrequencyPlan,
                profile.zwaveCommandClasses));
    }

    private void connectAndDiscover() {
        final String endpoint = textOf(endpointInput);
        final String token = textOf(tokenInput);
        if (endpoint.isEmpty()) {
            endpointInput.setError(getString(R.string.dawon_gateway_endpoint_required));
            return;
        }
        if (token.isEmpty()) {
            tokenInput.setError(getString(R.string.dawon_gateway_token_required));
            return;
        }

        saveGatewaySettings(endpoint, token);
        gatewayReady = false;
        setControlEnabled(false);
        setBusy(true);
        statusText.setText(R.string.dawon_gateway_connecting);

        worker.execute(() -> {
            try {
                HomeAssistantZWaveClient client = new HomeAssistantZWaveClient(endpoint, token);
                client.health();
                List<HomeAssistantZWaveClient.SwitchEntity> found = client.listSwitches();
                runOnUiThread(() -> applyDiscoveredSwitches(found));
            } catch (IOException error) {
                runOnUiThread(() -> showError(error));
            }
        });
    }

    private void applyDiscoveredSwitches(List<HomeAssistantZWaveClient.SwitchEntity> found) {
        setBusy(false);
        switches.clear();
        if (found != null) switches.addAll(found);

        List<String> labels = new ArrayList<>();
        for (HomeAssistantZWaveClient.SwitchEntity entity : switches) {
            String prefix = entity.matchScore > 0 ? "MTD-01 · " : "";
            labels.add(prefix + entity.displayName());
        }
        if (labels.isEmpty()) {
            labels.add(getString(R.string.dawon_no_switch_entities));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        entitySpinner.setAdapter(adapter);

        if (switches.isEmpty()) {
            gatewayReady = false;
            statusText.setText(R.string.dawon_gateway_connected_no_switches);
            setControlEnabled(false);
            return;
        }

        String preferred = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(PREF_SWITCH_ENTITY, "");
        int selection = 0;
        if (preferred != null && !preferred.isEmpty()) {
            for (int i = 0; i < switches.size(); i++) {
                if (preferred.equals(switches.get(i).entityId)) {
                    selection = i;
                    break;
                }
            }
        }
        entitySpinner.setSelection(selection, false);
        saveSelectedEntity(switches.get(selection).entityId);
        gatewayReady = true;
        setControlEnabled(true);
        renderState(switches.get(selection).entityId,
                switches.get(selection).state,
                switches.get(selection).friendlyName);

        if (switches.get(selection).matchScore > 0) {
            Snackbar.make(statusText, R.string.dawon_candidate_found, Snackbar.LENGTH_LONG).show();
        } else {
            Snackbar.make(statusText, R.string.dawon_select_switch_manually, Snackbar.LENGTH_LONG).show();
        }
    }

    private void refreshSelectedState() {
        HomeAssistantZWaveClient.SwitchEntity selected = selectedSwitch();
        if (!gatewayReady || selected == null) {
            Snackbar.make(statusText, R.string.dawon_select_switch_first, Snackbar.LENGTH_LONG).show();
            return;
        }
        final String endpoint = textOf(endpointInput);
        final String token = textOf(tokenInput);
        setBusy(true);
        worker.execute(() -> {
            try {
                HomeAssistantZWaveClient client = new HomeAssistantZWaveClient(endpoint, token);
                HomeAssistantZWaveClient.EntityState state = client.getState(selected.entityId);
                runOnUiThread(() -> {
                    setBusy(false);
                    renderState(state.entityId, state.state, state.friendlyName);
                });
            } catch (IOException error) {
                runOnUiThread(() -> showError(error));
            }
        });
    }

    private void setSelectedSwitch(boolean on) {
        HomeAssistantZWaveClient.SwitchEntity selected = selectedSwitch();
        if (!gatewayReady || selected == null) {
            Snackbar.make(statusText, R.string.dawon_select_switch_first, Snackbar.LENGTH_LONG).show();
            return;
        }
        final String endpoint = textOf(endpointInput);
        final String token = textOf(tokenInput);
        setBusy(true);
        statusText.setText(on ? R.string.dawon_sending_on : R.string.dawon_sending_off);

        worker.execute(() -> {
            try {
                HomeAssistantZWaveClient client = new HomeAssistantZWaveClient(endpoint, token);
                client.setSwitch(selected.entityId, on);
                try {
                    Thread.sleep(450L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                HomeAssistantZWaveClient.EntityState state = client.getState(selected.entityId);
                runOnUiThread(() -> {
                    setBusy(false);
                    renderState(state.entityId, state.state, state.friendlyName);
                    Snackbar.make(statusText,
                            on ? R.string.dawon_on_sent : R.string.dawon_off_sent,
                            Snackbar.LENGTH_SHORT).show();
                });
            } catch (IOException error) {
                runOnUiThread(() -> showError(error));
            }
        });
    }

    private HomeAssistantZWaveClient.SwitchEntity selectedSwitch() {
        int position = entitySpinner.getSelectedItemPosition();
        if (position < 0 || position >= switches.size()) return null;
        return switches.get(position);
    }

    private void renderState(String entityId, String state, String friendlyName) {
        String name = friendlyName == null || friendlyName.trim().isEmpty()
                ? entityId : friendlyName.trim();
        boolean on = "on".equalsIgnoreCase(state);
        boolean unavailable = "unavailable".equalsIgnoreCase(state)
                || "unknown".equalsIgnoreCase(state);
        statusText.setText(getString(R.string.dawon_state_format,
                name,
                entityId,
                unavailable ? getString(R.string.dawon_state_unavailable)
                        : (on ? getString(R.string.dawon_state_on)
                        : getString(R.string.dawon_state_off))));
    }

    private void saveGatewaySettings(String endpoint, String token) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_ENDPOINT, endpoint)
                .putString(PREF_TOKEN, token)
                .apply();
    }

    private void saveSelectedEntity(String entityId) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_SWITCH_ENTITY, entityId == null ? "" : entityId)
                .apply();
    }

    private void setBusy(boolean busy) {
        connectButton.setEnabled(!busy);
        boolean controlsReady = !busy && gatewayReady && selectedSwitch() != null;
        refreshButton.setEnabled(controlsReady);
        onButton.setEnabled(controlsReady);
        offButton.setEnabled(controlsReady);
        entitySpinner.setEnabled(!busy);
    }

    private void setControlEnabled(boolean enabled) {
        refreshButton.setEnabled(enabled);
        onButton.setEnabled(enabled);
        offButton.setEnabled(enabled);
    }

    private void showError(IOException error) {
        gatewayReady = false;
        setBusy(false);
        setControlEnabled(false);
        statusText.setText(getString(R.string.dawon_gateway_error, safeMessage(error)));
    }

    private static String textOf(TextInputEditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static String safeMessage(Throwable error) {
        if (error == null) return "unknown";
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? error.getClass().getSimpleName() : value;
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
