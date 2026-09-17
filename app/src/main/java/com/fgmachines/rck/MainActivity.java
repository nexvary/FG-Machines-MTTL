package com.fgmachines.rck;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
    private static final int WIFI_SETUP_PERMISSION_REQUEST = 88;

    private TextInputEditText ipInput;
    private TextInputEditText setupSsidInput;
    private TextInputEditText targetWifiSsidInput;
    private TextInputEditText targetWifiPasswordInput;
    private TextInputEditText controllerIpInput;
    private TextView deviceState;
    private TextView discoveryDetail;
    private TextView hotspotStatus;
    private TextView provisionStatus;
    private LinearProgressIndicator progress;
    private MaterialButton scanButton;
    private MaterialButton probeButton;
    private MaterialButton provisionButton;
    private MaterialButton openHotspotButton;
    private MaterialButton refreshHotspotButton;
    private Spinner languageSpinner;
    private MaterialSwitch[] outletSwitches;

    private final ExecutorService commandWorker = Executors.newSingleThreadExecutor();
    private volatile String activeMac;
    private volatile boolean applyingDeviceState;
    private MttlControllerServer controllerServer;
    private MttlProvisioner provisioner;
    private Runnable pendingWifiAction;

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
        setupSsidInput = findViewById(R.id.setupSsidInput);
        targetWifiSsidInput = findViewById(R.id.targetWifiSsidInput);
        targetWifiPasswordInput = findViewById(R.id.targetWifiPasswordInput);
        controllerIpInput = findViewById(R.id.controllerIpInput);
        deviceState = findViewById(R.id.deviceState);
        discoveryDetail = findViewById(R.id.discoveryDetail);
        hotspotStatus = findViewById(R.id.hotspotStatus);
        provisionStatus = findViewById(R.id.provisionStatus);
        progress = findViewById(R.id.progress);
        scanButton = findViewById(R.id.scanButton);
        probeButton = findViewById(R.id.probeButton);
        provisionButton = findViewById(R.id.provisionButton);
        openHotspotButton = findViewById(R.id.openHotspotButton);
        refreshHotspotButton = findViewById(R.id.refreshHotspotButton);
        languageSpinner = findViewById(R.id.languageSpinner);
        outletSwitches = new MaterialSwitch[] {
                findViewById(R.id.outlet1),
                findViewById(R.id.outlet2),
                findViewById(R.id.outlet3),
                findViewById(R.id.outlet4)
        };

        provisioner = new MttlProvisioner(this);
        configureLanguageSelector();
        configureOutletControls();
        configureSetupWorkflow();
        startLocalController();
        refreshHotspotStatus();

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

    private void configureSetupWorkflow() {
        openHotspotButton.setOnClickListener(v -> HotspotSupport.openSystemHotspotSettings(this));
        refreshHotspotButton.setOnClickListener(v -> refreshHotspotStatus());
        provisionButton.setOnClickListener(v -> requestWifiSetupPermission(this::provisionDevice));
    }

    private void refreshHotspotStatus() {
        String controllerIp = HotspotSupport.findControllerIpv4();
        String displayIp = controllerIp == null ? getString(R.string.hotspot_ip_unknown) : controllerIp;
        boolean concurrent = HotspotSupport.supportsSamePhoneProvisioning(this);
        hotspotStatus.setText(getString(
                concurrent ? R.string.hotspot_single_phone_supported : R.string.hotspot_second_device_required,
                displayIp
        ));

        if (controllerIp != null && controllerIpInput != null) {
            String current = textOf(controllerIpInput);
            if (current.isEmpty() || isPrivateIpv4(current)) controllerIpInput.setText(controllerIp);
        }
    }

    private void provisionDevice() {
        String setupSsid = textOf(setupSsidInput);
        String wifiSsid = textOf(targetWifiSsidInput);
        String wifiPassword = textOf(targetWifiPasswordInput);
        String controllerIp = textOf(controllerIpInput);

        if (setupSsid.isEmpty() || wifiSsid.isEmpty() || controllerIp.isEmpty()) {
            Snackbar.make(provisionButton, R.string.missing_setup_fields, Snackbar.LENGTH_LONG).show();
            return;
        }

        setProvisionBusy(true);
        provisionStatus.setText(R.string.provisioning);
        provisioner.provision(setupSsid, wifiSsid, wifiPassword, controllerIp,
                new MttlProvisioner.Callback() {
                    @Override
                    public void onStatus(String status) {
                        runOnUiThread(() -> provisionStatus.setText(status));
                    }

                    @Override
                    public void onComplete() {
                        runOnUiThread(() -> {
                            setProvisionBusy(false);
                            provisionStatus.setText(R.string.provision_complete_detail);
                            deviceState.setText(R.string.provision_complete);
                            discoveryDetail.setText(R.string.provision_complete_detail);
                        });
                    }

                    @Override
                    public void onError(String message, Throwable error) {
                        runOnUiThread(() -> {
                            setProvisionBusy(false);
                            String detail = error == null ? message : message + ": " + safeMessage(error);
                            provisionStatus.setText(getString(R.string.provision_failed, detail));
                            Snackbar.make(provisionButton,
                                    getString(R.string.provision_failed, detail), Snackbar.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void requestWifiSetupPermission(Runnable action) {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.NEARBY_WIFI_DEVICES
                : Manifest.permission.ACCESS_FINE_LOCATION;
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            action.run();
            return;
        }
        pendingWifiAction = action;
        requestPermissions(new String[]{permission}, WIFI_SETUP_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != WIFI_SETUP_PERMISSION_REQUEST) return;
        Runnable action = pendingWifiAction;
        pendingWifiAction = null;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (action != null) action.run();
        } else {
            Snackbar.make(provisionButton, R.string.wifi_permission_required, Snackbar.LENGTH_LONG).show();
        }
    }

    private void setProvisionBusy(boolean busy) {
        provisionButton.setEnabled(!busy);
        openHotspotButton.setEnabled(!busy);
        refreshHotspotButton.setEnabled(!busy);
        setupSsidInput.setEnabled(!busy);
        targetWifiSsidInput.setEnabled(!busy);
        targetWifiPasswordInput.setEnabled(!busy);
        controllerIpInput.setEnabled(!busy);
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
        String host = textOf(ipInput);
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

    private static String textOf(TextInputEditText input) {
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static boolean isPrivateIpv4(String value) {
        return value.startsWith("10.") || value.startsWith("192.168.") || value.startsWith("172.");
    }

    private static String safeMessage(Throwable error) {
        if (error == null) return "unknown";
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? error.getClass().getSimpleName() : value;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hotspotStatus != null) refreshHotspotStatus();
    }

    @Override
    protected void onDestroy() {
        if (provisioner != null) provisioner.close();
        if (controllerServer != null) controllerServer.close();
        commandWorker.shutdownNow();
        super.onDestroy();
    }
}
