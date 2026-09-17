package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "fg_rck_settings";
    private static final String PREF_LANGUAGE = "language";
    private static final String[] LANGUAGE_TAGS = {"ar", "en", "tr", "es", "de"};

    private TextInputEditText ipInput;
    private TextView deviceState;
    private TextView discoveryDetail;
    private LinearProgressIndicator progress;
    private MaterialButton scanButton;
    private MaterialButton probeButton;
    private Spinner languageSpinner;
    private MaterialSwitch[] outletSwitches;

    private final ExecutorService commandWorker = Executors.newSingleThreadExecutor();
    private volatile String activeMac;
    private volatile boolean applyingDeviceState;
    private MttlControllerServer controllerServer;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String language = prefs.getString(PREF_LANGUAGE, "");
        if (language == null || language.isEmpty()) {
            language = newBase.getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        if (!isSupportedLanguage(language)) language = "en";
        super.attachBaseContext(withLanguage(newBase, language));
    }

    private static Context withLanguage(Context context, String language) {
        Locale locale = Locale.forLanguageTag(language);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        return context.createConfigurationContext(configuration);
    }

    private static boolean isSupportedLanguage(String language) {
        for (String tag : LANGUAGE_TAGS) {
            if (tag.equals(language)) return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipInput = findViewById(R.id.ipInput);
        deviceState = findViewById(R.id.deviceState);
        discoveryDetail = findViewById(R.id.discoveryDetail);
        progress = findViewById(R.id.progress);
        scanButton = findViewById(R.id.scanButton);
        probeButton = findViewById(R.id.probeButton);
        languageSpinner = findViewById(R.id.languageSpinner);
        outletSwitches = new MaterialSwitch[] {
                findViewById(R.id.outlet1),
                findViewById(R.id.outlet2),
                findViewById(R.id.outlet3),
                findViewById(R.id.outlet4)
        };

        configureLanguageSelector();
        configureOutletControls();
        startLocalController();

        scanButton.setOnClickListener(v -> startScan());
        probeButton.setOnClickListener(v -> probeCurrentHost());
        ipInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                probeCurrentHost();
                return true;
            }
            return false;
        });
    }

    private void configureLanguageSelector() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.language_labels,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String selectedLanguage = prefs.getString(PREF_LANGUAGE, "");
        if (selectedLanguage == null || selectedLanguage.isEmpty()) {
            selectedLanguage = getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        languageSpinner.setSelection(languageIndex(selectedLanguage), false);

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= LANGUAGE_TAGS.length) return;
                String requested = LANGUAGE_TAGS[position];
                String current = getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getString(PREF_LANGUAGE, getResources().getConfiguration().getLocales().get(0).getLanguage());
                if (!requested.equals(current)) {
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit()
                            .putString(PREF_LANGUAGE, requested)
                            .apply();
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private int languageIndex(String language) {
        for (int i = 0; i < LANGUAGE_TAGS.length; i++) {
            if (LANGUAGE_TAGS[i].equals(language)) return i;
        }
        return 1;
    }

    private void configureOutletControls() {
        setOutletControlsEnabled(false);
        for (int i = 0; i < outletSwitches.length; i++) {
            final int outlet = i + 1;
            outletSwitches[i].setOnCheckedChangeListener((button, checked) -> {
                if (applyingDeviceState || !button.isEnabled()) return;
                String mac = activeMac;
                if (mac == null || controllerServer == null) return;
                commandWorker.execute(() -> {
                    try {
                        controllerServer.setOutlet(mac, outlet, checked);
                    } catch (IOException error) {
                        runOnUiThread(() -> Snackbar.make(
                                scanButton,
                                getString(R.string.command_failed, safeMessage(error)),
                                Snackbar.LENGTH_LONG
                        ).show());
                    }
                });
            });
        }
    }

    private void startLocalController() {
        controllerServer = new MttlControllerServer(new MttlControllerServer.Listener() {
            @Override
            public void onListening(int port) {
                runOnUiThread(() -> {
                    deviceState.setText(R.string.controller_listening);
                    discoveryDetail.setText(R.string.scan_explanation);
                });
            }

            @Override
            public void onDeviceConnected(MttlProtocol.BootInfo bootInfo, String remoteAddress) {
                activeMac = bootInfo.mac;
                runOnUiThread(() -> {
                    setOutletControlsEnabled(true);
                    deviceState.setText(getString(
                            R.string.controller_connected,
                            ModelCatalog.PRIMARY_MODEL,
                            bootInfo.firmwareVersion
                    ));
                    discoveryDetail.setText(remoteAddress);
                });
            }

            @Override
            public void onDeviceDisconnected(String mac) {
                if (!mac.equalsIgnoreCase(activeMac == null ? "" : activeMac)) return;
                activeMac = null;
                runOnUiThread(() -> {
                    setOutletControlsEnabled(false);
                    deviceState.setText(R.string.controller_disconnected);
                    discoveryDetail.setText(R.string.locked);
                });
            }

            @Override
            public void onOutletState(String mac, MttlProtocol.OutletState state) {
                if (!isActive(mac)) return;
                runOnUiThread(() -> applyOutletState(state.outlet, state.on));
            }

            @Override
            public void onTelemetry(String mac, MttlProtocol.Telemetry telemetry) {
                if (!isActive(mac)) return;
                runOnUiThread(() -> {
                    applyingDeviceState = true;
                    try {
                        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
                            if (outlet.channel >= 1 && outlet.channel <= outletSwitches.length) {
                                outletSwitches[outlet.channel - 1].setChecked(outlet.relayOn);
                            }
                        }
                    } finally {
                        applyingDeviceState = false;
                    }
                });
            }

            @Override
            public void onProtocolFrame(String mac, String frame) {
                // Unknown frames are ignored safely; they are not reflected back to the device.
            }

            @Override
            public void onError(String message, Throwable error) {
                runOnUiThread(() -> {
                    deviceState.setText(getString(R.string.controller_error, safeMessage(error)));
                    if (activeMac == null) setOutletControlsEnabled(false);
                });
            }
        });

        try {
            controllerServer.start();
        } catch (IOException error) {
            deviceState.setText(getString(R.string.controller_error, safeMessage(error)));
            setOutletControlsEnabled(false);
        }
    }

    private boolean isActive(String mac) {
        return mac != null && activeMac != null && mac.equalsIgnoreCase(activeMac);
    }

    private void applyOutletState(int outlet, boolean on) {
        if (outlet < 1 || outlet > outletSwitches.length) return;
        applyingDeviceState = true;
        try {
            outletSwitches[outlet - 1].setChecked(on);
        } finally {
            applyingDeviceState = false;
        }
    }

    private void setOutletControlsEnabled(boolean enabled) {
        if (outletSwitches == null) return;
        for (MaterialSwitch outletSwitch : outletSwitches) outletSwitch.setEnabled(enabled);
    }

    private void startScan() {
        setBusy(true);
        deviceState.setText(R.string.scanning);
        discoveryDetail.setText(R.string.scan_explanation);

        DeviceScanner.scanLocal24((hosts, subnet) -> runOnUiThread(() -> {
            setBusy(false);
            if (hosts.isEmpty()) {
                deviceState.setText(R.string.not_found);
                discoveryDetail.setText(getString(R.string.scan_none, subnet));
                return;
            }

            String first = hosts.get(0);
            ipInput.setText(first);
            deviceState.setText(R.string.service_detected);
            discoveryDetail.setText(getString(R.string.scan_found, hosts.size(), subnet, first));
            if (hosts.size() > 1) {
                Snackbar.make(scanButton, getString(R.string.multiple_hosts, hosts.size()), Snackbar.LENGTH_LONG).show();
            }
        }));
    }

    private void probeCurrentHost() {
        String host = ipInput.getText() == null ? "" : ipInput.getText().toString().trim();
        if (TextUtils.isEmpty(host)) {
            ipInput.setError(getString(R.string.enter_ip));
            return;
        }

        setBusy(true);
        deviceState.setText(R.string.probing);
        DeviceScanner.probe(host, (target, reachable, detail) -> runOnUiThread(() -> {
            setBusy(false);
            if (reachable) {
                deviceState.setText(R.string.service_detected);
                discoveryDetail.setText(getString(R.string.probe_success, target));
                Snackbar.make(scanButton, R.string.protocol_locked, Snackbar.LENGTH_LONG).show();
            } else {
                deviceState.setText(R.string.offline);
                discoveryDetail.setText(getString(R.string.probe_failed, target));
            }
        }));
    }

    private void setBusy(boolean busy) {
        progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        scanButton.setEnabled(!busy);
        probeButton.setEnabled(!busy);
        languageSpinner.setEnabled(!busy);
    }

    private static String safeMessage(Throwable error) {
        if (error == null) return "unknown";
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? error.getClass().getSimpleName() : value;
    }

    @Override
    protected void onDestroy() {
        if (controllerServer != null) controllerServer.close();
        commandWorker.shutdownNow();
        super.onDestroy();
    }
}
