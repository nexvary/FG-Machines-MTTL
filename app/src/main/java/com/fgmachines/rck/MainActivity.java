package com.fgmachines.rck;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "fg_rck_settings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_HOTSPOT_IP = "hotspot_controller_ip";
    private static final String PREF_TARGET_SSID = "target_ssid";
    private static final String PREF_SETUP_SSID = "setup_ssid";
    private static final String PREF_SETUP_MODE = "setup_mode";
    private static final String PREF_TARIFF = "energy_tariff_egp";
    private static final String PREF_STRIP_NAME = "strip_name";
    private static final String PREF_ROOM_NAME = "room_name";
    private static final String PREF_OUTLET_NAME_PREFIX = "outlet_name_";
    private static final double NOMINAL_VOLTAGE_V = 220.0;
    private static final int SETUP_MODE_ROUTER = 0;
    private static final int SETUP_MODE_TWO_PHONE = 1;
    private static final int SETUP_MODE_SINGLE_PHONE = 2;
    private static final String[] LANGUAGE_TAGS = {"ar", "en", "tr", "es", "de"};
    private static final int WIFI_SETUP_PERMISSION_REQUEST = 88;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 89;
    private static final int EXPORT_HISTORY_REQUEST = 90;
    private static final int VOICE_CONTROL_REQUEST = 91;
    private static final String PREF_ALERTS_ENABLED = "alerts_enabled";
    private static final String PREF_REMOTE_ENDPOINT = "remote_endpoint";
    private static final String PREF_REMOTE_TOKEN = "remote_token";
    private static final String FG_MACHINES_FACEBOOK_URL = "https://www.facebook.com/share/1Hx66RKhd2/";
    private static final String ALAA_MOHAMED_FACEBOOK_URL = "https://www.facebook.com/share/1DGDH6q8xV/";

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
    private TextInputEditText hardwareIdentityInput;
    private MaterialButton identifyHardwareButton;
    private TextView hardwareCatalogSummary;
    private TextView hardwareIdentityResult;
    private MaterialButton provisionButton;
    private MaterialButton manualProvisionButton;
    private MaterialButton openHotspotButton;
    private MaterialButton refreshHotspotButton;
    private MaterialButton openWifiButton;
    private MaterialButton fgMachinesFacebookButton;
    private MaterialButton alaaMohamedFacebookButton;
    private Spinner languageSpinner;
    private Spinner setupModeSpinner;
    private TextView setupModeDescription;
    private TextView setupModeBadge;
    private TextView setupReadinessText;
    private TextView totalPowerValue;
    private TextView totalEnergyValue;
    private TextView voltageValue;
    private TextView currentEstimateValue;
    private TextView maxTemperatureValue;
    private TextView estimatedCostValue;
    private TextView safetyStatus;
    private TextView[] outletTelemetryViews;
    private TextInputEditText tariffInput;
    private MaterialButton saveTariffButton;
    private MaterialButton refreshTelemetryButton;
    private TextInputEditText stripNameInput;
    private TextInputEditText roomNameInput;
    private TextInputEditText[] outletNameInputs;
    private MaterialButton saveDeviceNamesButton;
    private MaterialButton forgetDeviceButton;
    private MaterialSwitch alertsSwitch;
    private MaterialSwitch[] autoOffSwitches;
    private TextInputEditText[] autoOffMinutesInputs;
    private MaterialSwitch[] powerLimitSwitches;
    private TextInputEditText[] powerLimitInputs;
    private MaterialSwitch[] standbySwitches;
    private TextInputEditText[] standbyWattsInputs;
    private TextInputEditText[] standbyMinutesInputs;
    private MaterialSwitch[] scheduleSwitches;
    private TextInputEditText[] scheduleOnInputs;
    private TextInputEditText[] scheduleOffInputs;
    private Spinner[] scheduleDaySpinners;
    private MaterialButton saveAutomationButton;
    private TextView automationSummary;
    private MaterialSwitch awayModeSwitch;
    private TextInputEditText awayStartInput;
    private TextInputEditText awayEndInput;
    private TextInputEditText awayMinMinutesInput;
    private TextInputEditText awayMaxMinutesInput;
    private MaterialSwitch[] awayOutletSwitches;
    private MaterialButton saveAwayModeButton;
    private TextView awayModeSummary;
    private TextView runtimeSummary;
    private Spinner fleetDeviceSpinner;
    private Spinner fleetRoomSpinner;
    private TextView fleetStatus;
    private TextView fleetOverview;
    private TextView fleetDevicesList;
    private MaterialButton emergencyRoomOffButton;
    private MaterialButton emergencyAllOffButton;
    private HistorySparklineView historySparkline;
    private TextView historySummary;
    private TextView historyCostSummary;
    private TextView historyRecent;
    private TextInputEditText sceneNameInput;
    private MaterialButton saveSceneButton;
    private Spinner sceneSpinner;
    private MaterialButton applySceneButton;
    private MaterialButton deleteSceneButton;
    private TextView sceneStatus;
    private MaterialButton exportHistoryButton;
    private MaterialSwitch setupGuardCheck;
    private TextInputEditText alertPowerInput;
    private TextInputEditText alertTempInput;
    private TextInputEditText alertEnergyInput;
    private MaterialButton saveAlertLimitsButton;
    private TextView apiEndpointText;
    private TextInputEditText shareNameInput;
    private Spinner shareRoleSpinner;
    private MaterialButton createShareButton;
    private MaterialButton createHaTokenButton;
    private TextView shareTokenText;
    private TextView shareCodeText;
    private Spinner shareEntriesSpinner;
    private MaterialButton revokeShareButton;
    private MaterialButton voiceControlButton;
    private TextView voiceStatus;
    private TextInputEditText remoteShareCodeInput;
    private MaterialButton importShareCodeButton;
    private TextInputEditText remoteEndpointInput;
    private TextInputEditText remoteTokenInput;
    private MaterialButton remoteRefreshButton;
    private Spinner remoteDeviceSpinner;
    private Spinner remoteOutletSpinner;
    private MaterialButton remoteOnButton;
    private MaterialButton remoteOffButton;
    private TextView remoteStatus;
    private TextInputEditText cloudEndpointInput;
    private TextInputEditText cloudEmailInput;
    private TextInputEditText cloudPasswordInput;
    private MaterialButton cloudRegisterButton;
    private MaterialButton cloudLoginButton;
    private MaterialSwitch cloudSyncSwitch;
    private TextView cloudStatus;
    private TextView usbPort1Status;
    private TextView usbPort2Status;
    private TextView usbDiscoveryStatus;
    private MaterialButton startUsbDiscoveryButton;
    private MaterialButton refreshUsbDiscoveryButton;
    private TextView step1Status;
    private TextView step2Status;
    private TextView step3Status;
    private LinearProgressIndicator setupProgress;
    private MaterialSwitch[] outletSwitches;
    private View[] pages;
    private MaterialButton[] navButtons;
    private int currentPage;

    private final ExecutorService commandWorker = Executors.newSingleThreadExecutor();
    private volatile String activeMac;
    private volatile String activeFirmwareVersion;
    private volatile boolean applyingDeviceState;
    private double lastReportedEnergyKWh;
    private ControllerHub controllerHub;
    private MttlControllerServer.Listener controllerListener;
    private MttlProvisioner provisioner;
    private Runnable pendingWifiAction;
    private boolean provisionBusy;
    private boolean provisioningSucceeded;
    private FleetStore fleetStore;
    private HistoryStore historyStore;
    private OutletRuntimeStore runtimeStore;
    private UsbDiscoveryStore usbDiscoveryStore;
    private AccessControlStore accessStore;
    private SceneStore sceneStore;
    private final List<FleetStore.DeviceRecord> visibleFleetDevices = new ArrayList<>();
    private final List<AccessControlStore.AccessEntry> visibleAccessEntries = new ArrayList<>();
    private final List<RemoteApiClient.RemoteDevice> remoteDevices = new ArrayList<>();
    private final List<SceneStore.Scene> visibleScenes = new ArrayList<>();
    private String fleetRoomFilter = "";
    private String pendingExportMac;

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
        for (String tag : LANGUAGE_TAGS) if (tag.equals(language)) return true;
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applySystemBarInsets();
        bindViews();

        provisioner = new MttlProvisioner(this);
        fleetStore = new FleetStore(this);
        historyStore = new HistoryStore(this);
        runtimeStore = new OutletRuntimeStore(this);
        usbDiscoveryStore = new UsbDiscoveryStore(this);
        accessStore = new AccessControlStore(this);
        sceneStore = new SceneStore(this);
        controllerHub = ControllerHub.get(this);
        Intent controllerIntent = new Intent(this, MttlControllerService.class);
        controllerIntent.setAction(MttlControllerService.ACTION_START);
        ContextCompat.startForegroundService(this, controllerIntent);
        configureNavigation();
        configureLanguageSelector();
        configureSetupModeSelector();
        configureOutletControls();
        configureUsbHardware();
        configureSetupWorkflow();
        configureSetupReadiness();
        configureEnergyDashboard();
        configureDeviceNaming();
        configureAlerts();
        configureAutomationSettings();
        configureAwayMode();
        configureFleet();
        configureScenes();
        configureHistory();
        configureAlertLimits();
        configureSharing();
        configureVoiceControl();
        configureCloudAccount();
        configureRemoteControl();
        configureAboutLinks();
        restoreSetupProfile();
        startLocalController();
        updateHotspotStatus(false);

        scanButton.setOnClickListener(v -> startScan());
        probeButton.setOnClickListener(v -> probeCurrentHost());
        configureHardwareIdentification();
        ipInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                probeCurrentHost();
                return true;
            }
            return false;
        });
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.mainRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void bindViews() {
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
        hardwareIdentityInput = findViewById(R.id.hardwareIdentityInput);
        identifyHardwareButton = findViewById(R.id.identifyHardwareButton);
        hardwareCatalogSummary = findViewById(R.id.hardwareCatalogSummary);
        hardwareIdentityResult = findViewById(R.id.hardwareIdentityResult);
        provisionButton = findViewById(R.id.provisionButton);
        manualProvisionButton = findViewById(R.id.manualProvisionButton);
        openHotspotButton = findViewById(R.id.openHotspotButton);
        refreshHotspotButton = findViewById(R.id.refreshHotspotButton);
        openWifiButton = findViewById(R.id.openWifiButton);
        fgMachinesFacebookButton = findViewById(R.id.fgMachinesFacebookButton);
        alaaMohamedFacebookButton = findViewById(R.id.alaaMohamedFacebookButton);
        languageSpinner = findViewById(R.id.languageSpinner);
        setupModeSpinner = findViewById(R.id.setupModeSpinner);
        setupModeDescription = findViewById(R.id.setupModeDescription);
        setupModeBadge = findViewById(R.id.setupModeBadge);
        setupReadinessText = findViewById(R.id.setupReadinessText);
        totalPowerValue = findViewById(R.id.totalPowerValue);
        totalEnergyValue = findViewById(R.id.totalEnergyValue);
        voltageValue = findViewById(R.id.voltageValue);
        currentEstimateValue = findViewById(R.id.currentEstimateValue);
        maxTemperatureValue = findViewById(R.id.maxTemperatureValue);
        estimatedCostValue = findViewById(R.id.estimatedCostValue);
        safetyStatus = findViewById(R.id.safetyStatus);
        tariffInput = findViewById(R.id.tariffInput);
        saveTariffButton = findViewById(R.id.saveTariffButton);
        refreshTelemetryButton = findViewById(R.id.refreshTelemetryButton);
        stripNameInput = findViewById(R.id.stripNameInput);
        roomNameInput = findViewById(R.id.roomNameInput);
        saveDeviceNamesButton = findViewById(R.id.saveDeviceNamesButton);
        forgetDeviceButton = findViewById(R.id.forgetDeviceButton);
        alertsSwitch = findViewById(R.id.alertsSwitch);
        saveAutomationButton = findViewById(R.id.saveAutomationButton);
        automationSummary = findViewById(R.id.automationSummary);
        awayModeSwitch = findViewById(R.id.awayModeSwitch);
        awayStartInput = findViewById(R.id.awayStartInput);
        awayEndInput = findViewById(R.id.awayEndInput);
        awayMinMinutesInput = findViewById(R.id.awayMinMinutesInput);
        awayMaxMinutesInput = findViewById(R.id.awayMaxMinutesInput);
        saveAwayModeButton = findViewById(R.id.saveAwayModeButton);
        awayModeSummary = findViewById(R.id.awayModeSummary);
        runtimeSummary = findViewById(R.id.runtimeSummary);
        fleetDeviceSpinner = findViewById(R.id.fleetDeviceSpinner);
        fleetRoomSpinner = findViewById(R.id.fleetRoomSpinner);
        fleetStatus = findViewById(R.id.fleetStatus);
        fleetOverview = findViewById(R.id.fleetOverview);
        fleetDevicesList = findViewById(R.id.fleetDevicesList);
        emergencyRoomOffButton = findViewById(R.id.emergencyRoomOffButton);
        emergencyAllOffButton = findViewById(R.id.emergencyAllOffButton);
        historySparkline = findViewById(R.id.historySparkline);
        historySummary = findViewById(R.id.historySummary);
        historyCostSummary = findViewById(R.id.historyCostSummary);
        historyRecent = findViewById(R.id.historyRecent);
        sceneNameInput = findViewById(R.id.sceneNameInput);
        saveSceneButton = findViewById(R.id.saveSceneButton);
        sceneSpinner = findViewById(R.id.sceneSpinner);
        applySceneButton = findViewById(R.id.applySceneButton);
        deleteSceneButton = findViewById(R.id.deleteSceneButton);
        sceneStatus = findViewById(R.id.sceneStatus);
        exportHistoryButton = findViewById(R.id.exportHistoryButton);
        setupGuardCheck = findViewById(R.id.setupGuardCheck);
        alertPowerInput = findViewById(R.id.alertPowerInput);
        alertTempInput = findViewById(R.id.alertTempInput);
        alertEnergyInput = findViewById(R.id.alertEnergyInput);
        saveAlertLimitsButton = findViewById(R.id.saveAlertLimitsButton);
        apiEndpointText = findViewById(R.id.apiEndpointText);
        shareNameInput = findViewById(R.id.shareNameInput);
        shareRoleSpinner = findViewById(R.id.shareRoleSpinner);
        createShareButton = findViewById(R.id.createShareButton);
        createHaTokenButton = findViewById(R.id.createHaTokenButton);
        shareTokenText = findViewById(R.id.shareTokenText);
        shareCodeText = findViewById(R.id.shareCodeText);
        shareEntriesSpinner = findViewById(R.id.shareEntriesSpinner);
        revokeShareButton = findViewById(R.id.revokeShareButton);
        voiceControlButton = findViewById(R.id.voiceControlButton);
        voiceStatus = findViewById(R.id.voiceStatus);
        remoteShareCodeInput = findViewById(R.id.remoteShareCodeInput);
        importShareCodeButton = findViewById(R.id.importShareCodeButton);
        remoteEndpointInput = findViewById(R.id.remoteEndpointInput);
        remoteTokenInput = findViewById(R.id.remoteTokenInput);
        remoteRefreshButton = findViewById(R.id.remoteRefreshButton);
        remoteDeviceSpinner = findViewById(R.id.remoteDeviceSpinner);
        remoteOutletSpinner = findViewById(R.id.remoteOutletSpinner);
        remoteOnButton = findViewById(R.id.remoteOnButton);
        remoteOffButton = findViewById(R.id.remoteOffButton);
        remoteStatus = findViewById(R.id.remoteStatus);
        cloudEndpointInput = findViewById(R.id.cloudEndpointInput);
        cloudEmailInput = findViewById(R.id.cloudEmailInput);
        cloudPasswordInput = findViewById(R.id.cloudPasswordInput);
        cloudRegisterButton = findViewById(R.id.cloudRegisterButton);
        cloudLoginButton = findViewById(R.id.cloudLoginButton);
        cloudSyncSwitch = findViewById(R.id.cloudSyncSwitch);
        cloudStatus = findViewById(R.id.cloudStatus);
        usbPort1Status = findViewById(R.id.usbPort1Status);
        usbPort2Status = findViewById(R.id.usbPort2Status);
        usbDiscoveryStatus = findViewById(R.id.usbDiscoveryStatus);
        startUsbDiscoveryButton = findViewById(R.id.startUsbDiscoveryButton);
        refreshUsbDiscoveryButton = findViewById(R.id.refreshUsbDiscoveryButton);
        step1Status = findViewById(R.id.step1Status);
        step2Status = findViewById(R.id.step2Status);
        step3Status = findViewById(R.id.step3Status);
        setupProgress = findViewById(R.id.setupProgress);
        outletSwitches = new MaterialSwitch[]{
                findViewById(R.id.outlet1), findViewById(R.id.outlet2),
                findViewById(R.id.outlet3), findViewById(R.id.outlet4)
        };
        outletTelemetryViews = new TextView[]{
                findViewById(R.id.outlet1Telemetry), findViewById(R.id.outlet2Telemetry),
                findViewById(R.id.outlet3Telemetry), findViewById(R.id.outlet4Telemetry)
        };
        outletNameInputs = new TextInputEditText[]{
                findViewById(R.id.outlet1NameInput), findViewById(R.id.outlet2NameInput),
                findViewById(R.id.outlet3NameInput), findViewById(R.id.outlet4NameInput)
        };
        autoOffSwitches = new MaterialSwitch[]{
                findViewById(R.id.autoOffSwitch1), findViewById(R.id.autoOffSwitch2),
                findViewById(R.id.autoOffSwitch3), findViewById(R.id.autoOffSwitch4)
        };
        autoOffMinutesInputs = new TextInputEditText[]{
                findViewById(R.id.autoOffMinutes1), findViewById(R.id.autoOffMinutes2),
                findViewById(R.id.autoOffMinutes3), findViewById(R.id.autoOffMinutes4)
        };
        powerLimitSwitches = new MaterialSwitch[]{
                findViewById(R.id.powerLimitSwitch1), findViewById(R.id.powerLimitSwitch2),
                findViewById(R.id.powerLimitSwitch3), findViewById(R.id.powerLimitSwitch4)
        };
        powerLimitInputs = new TextInputEditText[]{
                findViewById(R.id.powerLimitW1), findViewById(R.id.powerLimitW2),
                findViewById(R.id.powerLimitW3), findViewById(R.id.powerLimitW4)
        };
        standbySwitches = new MaterialSwitch[]{
                findViewById(R.id.standbySwitch1), findViewById(R.id.standbySwitch2),
                findViewById(R.id.standbySwitch3), findViewById(R.id.standbySwitch4)
        };
        standbyWattsInputs = new TextInputEditText[]{
                findViewById(R.id.standbyWatts1), findViewById(R.id.standbyWatts2),
                findViewById(R.id.standbyWatts3), findViewById(R.id.standbyWatts4)
        };
        standbyMinutesInputs = new TextInputEditText[]{
                findViewById(R.id.standbyMinutes1), findViewById(R.id.standbyMinutes2),
                findViewById(R.id.standbyMinutes3), findViewById(R.id.standbyMinutes4)
        };
        awayOutletSwitches = new MaterialSwitch[]{
                findViewById(R.id.awayOutlet1), findViewById(R.id.awayOutlet2),
                findViewById(R.id.awayOutlet3), findViewById(R.id.awayOutlet4)
        };
        scheduleSwitches = new MaterialSwitch[]{
                findViewById(R.id.scheduleSwitch1), findViewById(R.id.scheduleSwitch2),
                findViewById(R.id.scheduleSwitch3), findViewById(R.id.scheduleSwitch4)
        };
        scheduleOnInputs = new TextInputEditText[]{
                findViewById(R.id.scheduleOn1), findViewById(R.id.scheduleOn2),
                findViewById(R.id.scheduleOn3), findViewById(R.id.scheduleOn4)
        };
        scheduleOffInputs = new TextInputEditText[]{
                findViewById(R.id.scheduleOff1), findViewById(R.id.scheduleOff2),
                findViewById(R.id.scheduleOff3), findViewById(R.id.scheduleOff4)
        };
        scheduleDaySpinners = new Spinner[]{
                findViewById(R.id.scheduleDayMode1), findViewById(R.id.scheduleDayMode2),
                findViewById(R.id.scheduleDayMode3), findViewById(R.id.scheduleDayMode4)
        };
        pages = new View[]{
                findViewById(R.id.pageHome), findViewById(R.id.pageSetup),
                findViewById(R.id.pageScan), findViewById(R.id.pageSettings),
                findViewById(R.id.pageAbout)
        };
        navButtons = new MaterialButton[]{
                findViewById(R.id.navHome), findViewById(R.id.navSetup),
                findViewById(R.id.navScan), findViewById(R.id.navSettings),
                findViewById(R.id.navAbout)
        };
    }

    private void configureNavigation() {
        for (int i = 0; i < navButtons.length; i++) {
            final int page = i;
            navButtons[i].setOnClickListener(v -> showPage(page));
        }
        findViewById(R.id.homeToSetup).setOnClickListener(v -> showPage(1));
        findViewById(R.id.homeToScan).setOnClickListener(v -> showPage(2));
        findViewById(R.id.homeToDiagnostics).setOnClickListener(v -> {
            Intent intent = new Intent(this, DiagnosticsActivity.class);
            if (activeMac != null && !activeMac.trim().isEmpty()) {
                intent.putExtra(DiagnosticsActivity.EXTRA_MAC, activeMac);
            }
            startActivity(intent);
        });
        showPage(0);
    }

    private void showPage(int page) {
        if (page < 0 || page >= pages.length) return;
        currentPage = page;
        for (int i = 0; i < pages.length; i++) {
            boolean selected = i == page;
            pages[i].setVisibility(selected ? View.VISIBLE : View.GONE);
            navButtons[i].setAlpha(selected ? 1f : 0.62f);
            navButtons[i].animate()
                    .scaleX(selected ? 1.04f : 0.96f)
                    .scaleY(selected ? 1.04f : 0.96f)
                    .setDuration(140)
                    .start();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentPage != 0) {
            showPage(0);
            return;
        }
        super.onBackPressed();
    }

    private void configureDeviceNaming() {
        clearDeviceNamingFields();
        saveDeviceNamesButton.setOnClickListener(v -> {
            if (activeMac == null || fleetStore == null) {
                Snackbar.make(saveDeviceNamesButton, R.string.select_device_first, Snackbar.LENGTH_LONG).show();
                return;
            }
            fleetStore.updateLabels(activeMac, textOf(stripNameInput), textOf(roomNameInput));
            for (int i = 0; i < outletNameInputs.length; i++) {
                fleetStore.updateOutletName(activeMac, i + 1, textOf(outletNameInputs[i]));
            }
            applyDeviceNames();
            refreshFleetUi();
            Snackbar.make(saveDeviceNamesButton, R.string.names_saved, Snackbar.LENGTH_SHORT).show();
        });

        forgetDeviceButton.setOnClickListener(v -> {
            if (activeMac == null || fleetStore == null) {
                Snackbar.make(forgetDeviceButton, R.string.select_device_first, Snackbar.LENGTH_LONG).show();
                return;
            }
            if (controllerHub != null && controllerHub.isConnected(activeMac)) {
                Snackbar.make(forgetDeviceButton, R.string.forget_device_disconnect_first, Snackbar.LENGTH_LONG).show();
                return;
            }
            String forgottenMac = activeMac;
            fleetStore.remove(forgottenMac);
            activeMac = null;
            activeFirmwareVersion = null;
            clearDeviceNamingFields();
            clearTelemetryUi();
            setOutletControlsEnabled(false);
            refreshFleetUi();
            refreshHistory();
            Snackbar.make(forgetDeviceButton, R.string.device_forgotten, Snackbar.LENGTH_SHORT).show();
        });
    }

    private void applyDeviceNames() {
        for (int i = 0; i < outletSwitches.length; i++) {
            String name = activeMac == null || fleetStore == null
                    ? "" : fleetStore.outletName(activeMac, i + 1);
            outletSwitches[i].setText(name == null || name.trim().isEmpty()
                    ? getString(outletNameResource(i)) : name.trim());
        }
        loadSelectedDeviceLabels();
    }

    private void loadSelectedDeviceLabels() {
        if (stripNameInput == null || roomNameInput == null) return;
        FleetStore.DeviceRecord record = activeMac == null || fleetStore == null
                ? null : fleetStore.get(activeMac);
        if (record == null) {
            clearDeviceNamingFields();
            return;
        }
        stripNameInput.setText(record.name);
        roomNameInput.setText(record.room);
        for (int i = 0; i < outletNameInputs.length; i++) {
            outletNameInputs[i].setText(fleetStore.outletName(activeMac, i + 1));
        }
    }

    private void clearDeviceNamingFields() {
        if (stripNameInput != null) stripNameInput.setText("");
        if (roomNameInput != null) roomNameInput.setText("");
        if (outletNameInputs != null) {
            for (TextInputEditText input : outletNameInputs) if (input != null) input.setText("");
        }
    }

    private int outletNameResource(int index) {
        switch (index) {
            case 0: return R.string.outlet_1;
            case 1: return R.string.outlet_2;
            case 2: return R.string.outlet_3;
            default: return R.string.outlet_4;
        }
    }

    private void configureEnergyDashboard() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        double tariff = Double.longBitsToDouble(prefs.getLong(PREF_TARIFF,
                Double.doubleToRawLongBits(0.0)));
        if (tariff > 0) tariffInput.setText(String.format(Locale.US, "%.4f", tariff));

        saveTariffButton.setOnClickListener(v -> {
            double value = parsePositiveDouble(textOf(tariffInput));
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putLong(PREF_TARIFF, Double.doubleToRawLongBits(value))
                    .apply();
            updateEstimatedCost();
            refreshHistory();
            Snackbar.make(saveTariffButton, R.string.tariff_saved, Snackbar.LENGTH_SHORT).show();
        });

        refreshTelemetryButton.setOnClickListener(v -> {
            String mac = activeMac;
            if (mac == null || controllerHub == null) {
                Snackbar.make(refreshTelemetryButton, R.string.telemetry_device_offline, Snackbar.LENGTH_SHORT).show();
                return;
            }
            commandWorker.execute(() -> {
                try {
                    controllerHub.refresh(mac);
                } catch (IOException error) {
                    runOnUiThread(() -> Snackbar.make(refreshTelemetryButton,
                            getString(R.string.command_failed, safeMessage(error)), Snackbar.LENGTH_LONG).show());
                }
            });
        });
        clearTelemetryUi();
    }

    private void updateTelemetryUi(MttlProtocol.Telemetry telemetry) {
        double totalPower = 0.0;
        double totalEnergy = 0.0;
        int maxTemp = Integer.MIN_VALUE;
        String protectionEvent = null;

        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
            totalPower += Math.max(0.0, outlet.powerW);
            totalEnergy += Math.max(0.0, outlet.energyKWh);
            maxTemp = Math.max(maxTemp, outlet.temperatureC);
            if (!"00".equals(outlet.eventCode)) protectionEvent = outlet.eventCode;

            if (outlet.channel >= 1 && outlet.channel <= outletTelemetryViews.length) {
                outletTelemetryViews[outlet.channel - 1].setText(getString(
                        R.string.outlet_telemetry_format,
                        outlet.powerW, outlet.energyKWh, outlet.temperatureC));
            }
        }

        lastReportedEnergyKWh = totalEnergy;
        totalPowerValue.setText(getString(R.string.power_value, totalPower));
        totalEnergyValue.setText(getString(R.string.energy_value, totalEnergy));
        currentEstimateValue.setText(getString(R.string.current_estimate_value,
                totalPower / NOMINAL_VOLTAGE_V));
        maxTemperatureValue.setText(maxTemp == Integer.MIN_VALUE
                ? getString(R.string.value_temperature_empty)
                : getString(R.string.temperature_value, maxTemp));
        voltageValue.setText(getString(R.string.voltage_not_exposed));
        updateEstimatedCost();

        if (protectionEvent != null) {
            safetyStatus.setText(getString(R.string.protection_event, protectionEvent));
            safetyStatus.setTextColor(getColor(R.color.fg_warning));
        } else if (totalPower > 3000.0) {
            safetyStatus.setText(R.string.high_load_warning);
            safetyStatus.setTextColor(getColor(R.color.fg_warning));
        } else {
            safetyStatus.setText(R.string.telemetry_normal);
            safetyStatus.setTextColor(getColor(R.color.fg_green));
        }
    }

    private void updateEstimatedCost() {
        double tariff = Double.longBitsToDouble(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(PREF_TARIFF, Double.doubleToRawLongBits(0.0)));
        if (tariff <= 0.0 || lastReportedEnergyKWh <= 0.0) {
            estimatedCostValue.setText(R.string.value_cost_empty);
            return;
        }
        estimatedCostValue.setText(getString(R.string.cost_value,
                lastReportedEnergyKWh * tariff));
    }

    private void clearTelemetryUi() {
        lastReportedEnergyKWh = 0.0;
        if (totalPowerValue != null) totalPowerValue.setText(R.string.value_power_empty);
        if (totalEnergyValue != null) totalEnergyValue.setText(R.string.value_energy_empty);
        if (voltageValue != null) voltageValue.setText(R.string.voltage_not_exposed);
        if (currentEstimateValue != null) currentEstimateValue.setText(R.string.value_current_empty);
        if (maxTemperatureValue != null) maxTemperatureValue.setText(R.string.value_temperature_empty);
        if (estimatedCostValue != null) estimatedCostValue.setText(R.string.value_cost_empty);
        if (safetyStatus != null) {
            safetyStatus.setText(R.string.telemetry_waiting);
            safetyStatus.setTextColor(getColor(R.color.fg_text_secondary));
        }
        if (outletTelemetryViews != null) {
            for (TextView value : outletTelemetryViews) {
                if (value != null) value.setText(R.string.outlet_telemetry_waiting);
            }
        }
    }

    private static double parsePositiveDouble(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            double parsed = Double.parseDouble(value.trim().replace(',', '.'));
            return parsed > 0.0 && Double.isFinite(parsed) ? parsed : 0.0;
        } catch (NumberFormatException error) {
            return 0.0;
        }
    }

    private void configureAutomationSettings() {
        for (int i = 0; i < 4; i++) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this, R.array.automation_day_mode_labels, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            scheduleDaySpinners[i].setAdapter(adapter);
        }
        saveAutomationButton.setOnClickListener(v -> saveAutomationSettings());
        loadAutomationSettingsForActiveDevice();
    }

    private void saveAutomationSettings() {
        if (activeMac == null) {
            Snackbar.make(saveAutomationButton, R.string.select_device_first, Snackbar.LENGTH_LONG).show();
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int enabledRules = 0;

        for (int i = 0; i < 4; i++) {
            int channel = i + 1;
            boolean autoOffEnabled = autoOffSwitches[i].isChecked();
            int minutes = parsePositiveInt(textOf(autoOffMinutesInputs[i]));
            if (autoOffEnabled && minutes <= 0) {
                autoOffMinutesInputs[i].setError(getString(R.string.automation_invalid_minutes));
                Snackbar.make(saveAutomationButton, R.string.automation_fix_fields, Snackbar.LENGTH_LONG).show();
                return;
            }

            boolean powerLimitEnabled = powerLimitSwitches[i].isChecked();
            int powerLimitW = parsePositiveInt(textOf(powerLimitInputs[i]));
            if (powerLimitEnabled && powerLimitW <= 0) {
                powerLimitInputs[i].setError(getString(R.string.automation_invalid_power_limit));
                Snackbar.make(saveAutomationButton, R.string.automation_fix_fields, Snackbar.LENGTH_LONG).show();
                return;
            }

            boolean standbyEnabled = standbySwitches[i].isChecked();
            int standbyW = parsePositiveInt(textOf(standbyWattsInputs[i]));
            int standbyMinutes = parsePositiveInt(textOf(standbyMinutesInputs[i]));
            if (standbyEnabled && (standbyW <= 0 || standbyMinutes <= 0)) {
                if (standbyW <= 0) standbyWattsInputs[i].setError(
                        getString(R.string.automation_invalid_standby_w));
                if (standbyMinutes <= 0) standbyMinutesInputs[i].setError(
                        getString(R.string.automation_invalid_minutes));
                Snackbar.make(saveAutomationButton, R.string.automation_fix_fields, Snackbar.LENGTH_LONG).show();
                return;
            }

            boolean scheduleEnabled = scheduleSwitches[i].isChecked();
            String onTime = textOf(scheduleOnInputs[i]);
            String offTime = textOf(scheduleOffInputs[i]);
            boolean hasScheduleTime = !onTime.isEmpty() || !offTime.isEmpty();
            if (scheduleEnabled && (!hasScheduleTime
                    || !LocalAutomationEngine.isValidTime(onTime)
                    || !LocalAutomationEngine.isValidTime(offTime))) {
                if (!LocalAutomationEngine.isValidTime(onTime)) {
                    scheduleOnInputs[i].setError(getString(R.string.automation_invalid_time));
                }
                if (!LocalAutomationEngine.isValidTime(offTime)) {
                    scheduleOffInputs[i].setError(getString(R.string.automation_invalid_time));
                }
                Snackbar.make(saveAutomationButton, R.string.automation_fix_fields, Snackbar.LENGTH_LONG).show();
                return;
            }

            editor.putBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_AUTO_OFF_ENABLED, activeMac, channel), autoOffEnabled);
            editor.putInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_AUTO_OFF_MINUTES, activeMac, channel),
                    minutes > 0 ? minutes : 30);
            editor.putBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_POWER_LIMIT_ENABLED, activeMac, channel), powerLimitEnabled);
            editor.putInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_POWER_LIMIT_W, activeMac, channel),
                    powerLimitW > 0 ? powerLimitW : 0);
            editor.putBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_STANDBY_ENABLED, activeMac, channel), standbyEnabled);
            editor.putInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_STANDBY_W, activeMac, channel),
                    standbyW > 0 ? standbyW : 0);
            editor.putInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_STANDBY_MINUTES, activeMac, channel),
                    standbyMinutes > 0 ? standbyMinutes : 5);
            editor.putBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_ENABLED, activeMac, channel), scheduleEnabled);
            editor.putString(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_ON, activeMac, channel),
                    LocalAutomationEngine.normalizeTime(onTime));
            editor.putString(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_OFF, activeMac, channel),
                    LocalAutomationEngine.normalizeTime(offTime));
            editor.putInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_DAY_MODE, activeMac, channel),
                    scheduleDaySpinners[i].getSelectedItemPosition());

            if (autoOffEnabled) enabledRules++;
            if (powerLimitEnabled) enabledRules++;
            if (standbyEnabled) enabledRules++;
            if (scheduleEnabled) enabledRules++;
            editor.remove(LocalAutomationEngine.deadlinePreferenceKey(activeMac, channel));
            editor.remove(LocalAutomationEngine.standbyDeadlinePreferenceKey(activeMac, channel));
        }
        editor.apply();
        updateAutomationSummary();
        Snackbar.make(saveAutomationButton,
                getString(R.string.automation_saved, enabledRules), Snackbar.LENGTH_SHORT).show();
    }

    private void updateAutomationSummary() {
        if (automationSummary == null) return;
        if (activeMac == null) {
            automationSummary.setText(R.string.automation_select_device);
            automationSummary.setTextColor(getColor(R.color.fg_silver));
            return;
        }
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int rules = 0;
        for (int channel = 1; channel <= 4; channel++) {
            if (prefs.getBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_AUTO_OFF_ENABLED, activeMac, channel), false)) rules++;
            if (prefs.getBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_POWER_LIMIT_ENABLED, activeMac, channel), false)) rules++;
            if (prefs.getBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_STANDBY_ENABLED, activeMac, channel), false)) rules++;
            if (prefs.getBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_ENABLED, activeMac, channel), false)) rules++;
        }
        if (prefs.getBoolean(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_ENABLED, activeMac), false)) rules++;
        automationSummary.setText(rules == 0
                ? getString(R.string.automation_none)
                : getString(R.string.automation_active_count, rules));
        automationSummary.setTextColor(getColor(rules == 0 ? R.color.fg_silver : R.color.fg_green));
    }

    private void loadAutomationSettingsForActiveDevice() {
        boolean enabled = activeMac != null;
        saveAutomationButton.setEnabled(enabled);
        if (!enabled) {
            for (int i = 0; i < 4; i++) {
                autoOffSwitches[i].setChecked(false);
                autoOffMinutesInputs[i].setText("30");
                powerLimitSwitches[i].setChecked(false);
                powerLimitInputs[i].setText("");
                standbySwitches[i].setChecked(false);
                standbyWattsInputs[i].setText("");
                standbyMinutesInputs[i].setText("5");
                scheduleSwitches[i].setChecked(false);
                scheduleOnInputs[i].setText("");
                scheduleOffInputs[i].setText("");
                scheduleDaySpinners[i].setSelection(LocalAutomationEngine.DAY_EVERY_DAY, false);
            }
            updateAutomationSummary();
            return;
        }

        migrateLegacyAutomation(activeMac);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        for (int i = 0; i < 4; i++) {
            int channel = i + 1;
            autoOffSwitches[i].setChecked(prefs.getBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_AUTO_OFF_ENABLED, activeMac, channel), false));
            autoOffMinutesInputs[i].setText(String.valueOf(prefs.getInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_AUTO_OFF_MINUTES, activeMac, channel), 30)));

            powerLimitSwitches[i].setChecked(prefs.getBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_POWER_LIMIT_ENABLED, activeMac, channel), false));
            int power = prefs.getInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_POWER_LIMIT_W, activeMac, channel), 0);
            powerLimitInputs[i].setText(power > 0 ? String.valueOf(power) : "");

            standbySwitches[i].setChecked(prefs.getBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_STANDBY_ENABLED, activeMac, channel), false));
            int standbyW = prefs.getInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_STANDBY_W, activeMac, channel), 0);
            standbyWattsInputs[i].setText(standbyW > 0 ? String.valueOf(standbyW) : "");
            standbyMinutesInputs[i].setText(String.valueOf(prefs.getInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_STANDBY_MINUTES, activeMac, channel), 5)));

            scheduleSwitches[i].setChecked(prefs.getBoolean(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_ENABLED, activeMac, channel), false));
            scheduleOnInputs[i].setText(prefs.getString(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_ON, activeMac, channel), ""));
            scheduleOffInputs[i].setText(prefs.getString(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_OFF, activeMac, channel), ""));
            int dayMode = prefs.getInt(LocalAutomationEngine.deviceKey(
                    LocalAutomationEngine.KEY_SCHEDULE_DAY_MODE, activeMac, channel),
                    LocalAutomationEngine.DAY_EVERY_DAY);
            if (dayMode < LocalAutomationEngine.DAY_EVERY_DAY
                    || dayMode > LocalAutomationEngine.DAY_WEEKENDS) {
                dayMode = LocalAutomationEngine.DAY_EVERY_DAY;
            }
            scheduleDaySpinners[i].setSelection(dayMode, false);
        }
        updateAutomationSummary();
    }

    private void configureAwayMode() {
        saveAwayModeButton.setOnClickListener(v -> saveAwayModeSettings());
        loadAwayModeForActiveDevice();
        refreshRuntimeSummary();
    }

    private void saveAwayModeSettings() {
        if (activeMac == null) {
            Snackbar.make(saveAwayModeButton, R.string.select_device_first, Snackbar.LENGTH_LONG).show();
            return;
        }
        boolean enabled = awayModeSwitch.isChecked();
        String start = textOf(awayStartInput);
        String end = textOf(awayEndInput);
        int minMinutes = parsePositiveInt(textOf(awayMinMinutesInput));
        int maxMinutes = parsePositiveInt(textOf(awayMaxMinutesInput));
        int mask = 0;
        for (int i = 0; i < awayOutletSwitches.length; i++) {
            if (awayOutletSwitches[i].isChecked()) mask |= (1 << i);
        }

        if (enabled) {
            if (!LocalAutomationEngine.isValidTime(start) || start.isEmpty()) {
                awayStartInput.setError(getString(R.string.automation_invalid_time));
                return;
            }
            if (!LocalAutomationEngine.isValidTime(end) || end.isEmpty()) {
                awayEndInput.setError(getString(R.string.automation_invalid_time));
                return;
            }
            if (minMinutes <= 0 || maxMinutes < minMinutes) {
                awayMinMinutesInput.setError(getString(R.string.away_interval_invalid));
                awayMaxMinutesInput.setError(getString(R.string.away_interval_invalid));
                return;
            }
            if (mask == 0) {
                Snackbar.make(saveAwayModeButton,
                        R.string.away_select_outlet, Snackbar.LENGTH_LONG).show();
                return;
            }
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putBoolean(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_ENABLED, activeMac), enabled);
        editor.putString(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_START, activeMac),
                LocalAutomationEngine.normalizeTime(start));
        editor.putString(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_END, activeMac),
                LocalAutomationEngine.normalizeTime(end));
        editor.putInt(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_MIN_MINUTES, activeMac),
                minMinutes > 0 ? minMinutes : 15);
        editor.putInt(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_MAX_MINUTES, activeMac),
                maxMinutes > 0 ? maxMinutes : 45);
        editor.putInt(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_OUTLET_MASK, activeMac), mask);
        editor.remove(LocalAutomationEngine.awayNextPreferenceKey(activeMac));
        editor.apply();

        loadAwayModeForActiveDevice();
        updateAutomationSummary();
        if (historyStore != null) {
            historyStore.recordEvent(activeMac, 0, "away_mode_config",
                    enabled ? "enabled" : "disabled", System.currentTimeMillis());
        }
        Snackbar.make(saveAwayModeButton,
                enabled ? R.string.away_mode_saved : R.string.away_mode_disabled,
                Snackbar.LENGTH_SHORT).show();
    }

    private void loadAwayModeForActiveDevice() {
        boolean hasDevice = activeMac != null;
        awayModeSwitch.setEnabled(hasDevice);
        saveAwayModeButton.setEnabled(hasDevice);
        for (MaterialSwitch outlet : awayOutletSwitches) outlet.setEnabled(hasDevice);

        if (!hasDevice) {
            awayModeSwitch.setChecked(false);
            awayStartInput.setText("18:00");
            awayEndInput.setText("23:00");
            awayMinMinutesInput.setText("15");
            awayMaxMinutesInput.setText("45");
            for (MaterialSwitch outlet : awayOutletSwitches) outlet.setChecked(false);
            awayModeSummary.setText(R.string.away_mode_waiting);
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        awayModeSwitch.setChecked(prefs.getBoolean(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_ENABLED, activeMac), false));
        awayStartInput.setText(prefs.getString(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_START, activeMac), "18:00"));
        awayEndInput.setText(prefs.getString(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_END, activeMac), "23:00"));
        awayMinMinutesInput.setText(String.valueOf(prefs.getInt(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_MIN_MINUTES, activeMac), 15)));
        awayMaxMinutesInput.setText(String.valueOf(prefs.getInt(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_MAX_MINUTES, activeMac), 45)));
        int mask = prefs.getInt(LocalAutomationEngine.deviceKey(
                LocalAutomationEngine.KEY_AWAY_OUTLET_MASK, activeMac), 0);
        for (int i = 0; i < awayOutletSwitches.length; i++) {
            awayOutletSwitches[i].setChecked((mask & (1 << i)) != 0);
        }
        awayModeSummary.setText(awayModeSwitch.isChecked()
                ? getString(R.string.away_mode_active_format,
                        textOf(awayStartInput), textOf(awayEndInput),
                        Integer.bitCount(mask))
                : getString(R.string.away_mode_off));
    }

    private void refreshRuntimeSummary() {
        if (runtimeSummary == null || runtimeStore == null) return;
        if (activeMac == null) {
            runtimeSummary.setText(R.string.runtime_waiting);
            return;
        }
        long now = System.currentTimeMillis();
        StringBuilder text = new StringBuilder();
        for (int outlet = 1; outlet <= 4; outlet++) {
            OutletRuntimeStore.Snapshot snapshot = runtimeStore.snapshot(activeMac, outlet, now);
            if (outlet > 1) text.append('\n');
            if (!snapshot.known) {
                text.append(getString(R.string.runtime_unknown_line, outlet));
            } else {
                text.append(getString(R.string.runtime_line_format,
                        outlet,
                        snapshot.on ? getString(R.string.remote_on) : getString(R.string.remote_off),
                        OutletRuntimeStore.formatDuration(snapshot.runtimeMs),
                        snapshot.switchCount));
            }
        }
        runtimeSummary.setText(text.toString());
    }

    private void migrateLegacyAutomation(String mac) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean("automation_legacy_consumed", false)) return;
        boolean anyLegacy = false;
        SharedPreferences.Editor editor = prefs.edit();
        for (int channel = 1; channel <= 4; channel++) {
            if (prefs.contains(LocalAutomationEngine.KEY_AUTO_OFF_ENABLED + channel)) {
                anyLegacy = true;
                editor.putBoolean(LocalAutomationEngine.deviceKey(
                        LocalAutomationEngine.KEY_AUTO_OFF_ENABLED, mac, channel),
                        prefs.getBoolean(LocalAutomationEngine.KEY_AUTO_OFF_ENABLED + channel, false));
                editor.putInt(LocalAutomationEngine.deviceKey(
                        LocalAutomationEngine.KEY_AUTO_OFF_MINUTES, mac, channel),
                        prefs.getInt(LocalAutomationEngine.KEY_AUTO_OFF_MINUTES + channel, 30));
                editor.putBoolean(LocalAutomationEngine.deviceKey(
                        LocalAutomationEngine.KEY_SCHEDULE_ENABLED, mac, channel),
                        prefs.getBoolean(LocalAutomationEngine.KEY_SCHEDULE_ENABLED + channel, false));
                editor.putString(LocalAutomationEngine.deviceKey(
                        LocalAutomationEngine.KEY_SCHEDULE_ON, mac, channel),
                        prefs.getString(LocalAutomationEngine.KEY_SCHEDULE_ON + channel, ""));
                editor.putString(LocalAutomationEngine.deviceKey(
                        LocalAutomationEngine.KEY_SCHEDULE_OFF, mac, channel),
                        prefs.getString(LocalAutomationEngine.KEY_SCHEDULE_OFF + channel, ""));
                editor.putInt(LocalAutomationEngine.deviceKey(
                        LocalAutomationEngine.KEY_SCHEDULE_DAY_MODE, mac, channel),
                        prefs.getInt(LocalAutomationEngine.KEY_SCHEDULE_DAY_MODE + channel,
                                LocalAutomationEngine.DAY_EVERY_DAY));
            }
        }
        if (anyLegacy) editor.putBoolean("automation_legacy_consumed", true);
        editor.apply();
    }

    private static int parsePositiveInt(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : 0;
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private void configureFleet() {
        fleetRoomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object selected = parent.getItemAtPosition(position);
                String value = selected == null ? "" : selected.toString();
                fleetRoomFilter = position == 0 ? "" : value;
                refreshFleetDeviceSpinner();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        fleetDeviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= visibleFleetDevices.size()) return;
                selectFleetDevice(visibleFleetDevices.get(position).mac);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        emergencyRoomOffButton.setOnClickListener(v -> requestEmergencyOff(true));
        emergencyAllOffButton.setOnClickListener(v -> requestEmergencyOff(false));
        refreshFleetUi();
    }

    private void refreshFleetUi() {
        if (fleetStore == null || fleetRoomSpinner == null) return;
        List<String> rooms = new ArrayList<>();
        rooms.add(getString(R.string.all_rooms));
        rooms.addAll(fleetStore.rooms());
        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, rooms);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fleetRoomSpinner.setAdapter(roomAdapter);
        int roomIndex = 0;
        if (!fleetRoomFilter.isEmpty()) {
            for (int i = 1; i < rooms.size(); i++) {
                if (fleetRoomFilter.equalsIgnoreCase(rooms.get(i))) {
                    roomIndex = i;
                    break;
                }
            }
        }
        fleetRoomSpinner.setSelection(roomIndex, false);
        refreshFleetDeviceSpinner();
        updateApiEndpoint();
    }

    private void refreshFleetDeviceSpinner() {
        visibleFleetDevices.clear();
        List<String> labels = new ArrayList<>();
        for (FleetStore.DeviceRecord record : fleetStore.list()) {
            if (!fleetRoomFilter.isEmpty() && !fleetRoomFilter.equalsIgnoreCase(record.room)) continue;
            visibleFleetDevices.add(record);
            ControllerHub.DeviceState live = controllerHub == null ? null : controllerHub.state(record.mac);
            String label = record.displayName();
            if (!record.room.isEmpty()) label += " · " + record.room;
            label += live != null && live.connected
                    ? " · " + getString(R.string.fleet_online)
                    : " · " + getString(R.string.fleet_offline);
            labels.add(label);
        }
        if (labels.isEmpty()) labels.add(getString(R.string.fleet_no_devices));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fleetDeviceSpinner.setAdapter(adapter);

        refreshFleetOverviewAndList();

        if (!visibleFleetDevices.isEmpty()) {
            String preferred = activeMac != null ? FleetStore.normalizeMac(activeMac) : fleetStore.selectedMac();
            int selected = 0;
            for (int i = 0; i < visibleFleetDevices.size(); i++) {
                if (visibleFleetDevices.get(i).mac.equalsIgnoreCase(preferred)) {
                    selected = i;
                    break;
                }
            }
            fleetDeviceSpinner.setSelection(selected, false);
            selectFleetDevice(visibleFleetDevices.get(selected).mac);
        } else {
            fleetStatus.setText(R.string.fleet_waiting);
        }
    }

    private void refreshFleetOverviewAndList() {
        if (fleetOverview == null || fleetDevicesList == null || fleetStore == null) return;

        List<FleetStore.DeviceRecord> records = new ArrayList<>();
        for (FleetStore.DeviceRecord record : fleetStore.list()) {
            if (!fleetRoomFilter.isEmpty() && !fleetRoomFilter.equalsIgnoreCase(record.room)) continue;
            records.add(record);
        }

        int total = records.size();
        int online = 0;
        double totalPowerW = 0.0;
        double totalEnergyKWh = 0.0;
        StringBuilder list = new StringBuilder();

        for (FleetStore.DeviceRecord record : records) {
            ControllerHub.DeviceState live = controllerHub == null ? null : controllerHub.state(record.mac);
            boolean connected = live != null && live.connected;
            if (connected) online++;

            double powerW = fleetPowerW(live);
            double energyKWh = fleetEnergyKWh(live);
            totalPowerW += powerW;
            totalEnergyKWh += energyKWh;

            if (list.length() > 0) list.append("\n");
            boolean selected = activeMac != null && record.mac.equalsIgnoreCase(activeMac);
            list.append(selected ? "▶ " : "  ");
            if (connected) {
                list.append(getString(R.string.fleet_device_line_online,
                        record.displayName(),
                        record.room == null || record.room.trim().isEmpty()
                                ? getString(R.string.room_unassigned) : record.room.trim(),
                        powerW,
                        energyKWh));
            } else {
                String lastSeen = record.lastSeenAt <= 0
                        ? getString(R.string.unknown_value)
                        : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                .format(new java.util.Date(record.lastSeenAt));
                list.append(getString(R.string.fleet_device_line_offline,
                        record.displayName(),
                        record.room == null || record.room.trim().isEmpty()
                                ? getString(R.string.room_unassigned) : record.room.trim(),
                        lastSeen));
            }
        }

        int offline = Math.max(0, total - online);
        String scope = fleetRoomFilter == null || fleetRoomFilter.trim().isEmpty()
                ? getString(R.string.fleet_scope_all)
                : getString(R.string.fleet_scope_room, fleetRoomFilter.trim());
        fleetOverview.setText(getString(R.string.fleet_overview_format,
                scope, total, online, offline, totalPowerW, totalEnergyKWh));
        fleetDevicesList.setText(list.length() == 0
                ? getString(R.string.fleet_list_waiting)
                : list.toString());

        int allTargets = countEmergencyTargets(false);
        int roomTargets = countEmergencyTargets(true);
        emergencyAllOffButton.setText(getString(R.string.fleet_all_off_count, allTargets));
        emergencyRoomOffButton.setText(getString(R.string.room_all_off_count, roomTargets));
    }

    private static double fleetPowerW(ControllerHub.DeviceState state) {
        if (state == null || !state.connected || state.telemetry == null) return 0.0;
        double total = 0.0;
        for (MttlProtocol.OutletTelemetry outlet : state.telemetry.outlets) {
            total += Math.max(0.0, outlet.powerW);
        }
        return total;
    }

    private static double fleetEnergyKWh(ControllerHub.DeviceState state) {
        if (state == null || !state.connected || state.telemetry == null) return 0.0;
        double total = 0.0;
        for (MttlProtocol.OutletTelemetry outlet : state.telemetry.outlets) {
            total += Math.max(0.0, outlet.energyKWh);
        }
        return total;
    }

    private void selectFleetDevice(String mac) {
        String key = FleetStore.normalizeMac(mac);
        if (key.isEmpty()) return;
        activeMac = key;
        fleetStore.select(key);
        migrateLegacyLabelsIfNeeded(key);
        ControllerHub.DeviceState state = controllerHub.state(key);
        activeFirmwareVersion = state == null ? null : state.firmwareVersion;
        applyDeviceNames();
        loadAutomationSettingsForActiveDevice();
        loadAwayModeForActiveDevice();
        refreshRuntimeSummary();

        if (state != null && state.connected) {
            setOutletControlsEnabled(true);
            deviceState.setText(getString(R.string.controller_connected,
                    ModelCatalog.PRIMARY_MODEL, state.firmwareVersion));
            FleetStore.DeviceRecord record = fleetStore.get(key);
            discoveryDetail.setText(getString(R.string.device_location_detail,
                    record == null ? "" : record.displayName(),
                    record == null ? "" : record.room,
                    state.remoteAddress));
            if (state.telemetry != null) {
                applyingDeviceState = true;
                try {
                    for (MttlProtocol.OutletTelemetry outlet : state.telemetry.outlets) {
                        if (outlet.channel >= 1 && outlet.channel <= outletSwitches.length) {
                            outletSwitches[outlet.channel - 1].setChecked(outlet.relayOn);
                        }
                    }
                    updateTelemetryUi(state.telemetry);
                } finally {
                    applyingDeviceState = false;
                }
            }
        } else {
            setOutletControlsEnabled(false);
            clearTelemetryUi();
            deviceState.setText(R.string.controller_disconnected);
        }
        updateFleetStatus();
        refreshFleetOverviewAndList();
        refreshUsbDiscoveryStatus();
        refreshScenes();
        refreshHistory();
        refreshRuntimeSummary();
    }

    private void migrateLegacyLabelsIfNeeded(String mac) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean("fleet_legacy_labels_migrated", false)) return;
        FleetStore.DeviceRecord record = fleetStore.get(mac);
        if (record == null) return;

        String oldName = prefs.getString(PREF_STRIP_NAME, "");
        String oldRoom = prefs.getString(PREF_ROOM_NAME, "");
        boolean hasLegacy = (oldName != null && !oldName.trim().isEmpty())
                || (oldRoom != null && !oldRoom.trim().isEmpty());
        for (int outlet = 1; outlet <= 4; outlet++) {
            String oldOutlet = prefs.getString(PREF_OUTLET_NAME_PREFIX + outlet, "");
            hasLegacy |= oldOutlet != null && !oldOutlet.trim().isEmpty();
        }
        if (!hasLegacy) {
            prefs.edit().putBoolean("fleet_legacy_labels_migrated", true).apply();
            return;
        }

        if (record.name.isEmpty() && record.room.isEmpty()) {
            fleetStore.updateLabels(mac, oldName, oldRoom);
        }
        for (int outlet = 1; outlet <= 4; outlet++) {
            if (fleetStore.outletName(mac, outlet).isEmpty()) {
                fleetStore.updateOutletName(mac, outlet,
                        prefs.getString(PREF_OUTLET_NAME_PREFIX + outlet, ""));
            }
        }
        prefs.edit().putBoolean("fleet_legacy_labels_migrated", true).apply();
    }

    private void updateFleetStatus() {
        if (fleetStatus == null || fleetStore == null) return;
        int total = fleetStore.list().size();
        int connected = controllerHub == null ? 0 : controllerHub.connectedStates().size();
        FleetStore.DeviceRecord record = activeMac == null ? null : fleetStore.get(activeMac);
        ControllerHub.DeviceState state = activeMac == null ? null : controllerHub.state(activeMac);
        String lastSeen = record == null || record.lastSeenAt <= 0
                ? getString(R.string.unknown_value)
                : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new java.util.Date(record.lastSeenAt));
        String uptime = state == null || !state.connected || state.connectedSince <= 0
                ? getString(R.string.not_connected)
                : formatDuration(System.currentTimeMillis() - state.connectedSince);
        fleetStatus.setText(getString(R.string.fleet_status_format,
                connected, total, lastSeen, uptime));
    }

    private void configureScenes() {
        sceneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= visibleScenes.size()) {
                    sceneStatus.setText(R.string.no_scenes);
                    return;
                }
                SceneStore.Scene scene = visibleScenes.get(position);
                sceneStatus.setText(getString(R.string.scene_selected_status,
                        scene.name, scene.onCount()));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        saveSceneButton.setOnClickListener(v -> {
            if (activeMac == null || !controllerHub.isConnected(activeMac)) {
                Snackbar.make(sceneStatus, R.string.scene_device_offline, Snackbar.LENGTH_LONG).show();
                return;
            }
            String name = textOf(sceneNameInput);
            if (name.isEmpty()) {
                sceneNameInput.setError(getString(R.string.scene_name_required));
                return;
            }
            int mask = 0;
            for (int i = 0; i < outletSwitches.length; i++) {
                if (outletSwitches[i].isChecked()) mask |= (1 << i);
            }
            SceneStore.Scene scene = sceneStore.save(activeMac, name, mask);
            sceneNameInput.setText("");
            refreshScenes();
            Snackbar.make(sceneStatus,
                    getString(R.string.scene_saved, scene.name), Snackbar.LENGTH_SHORT).show();
        });

        applySceneButton.setOnClickListener(v -> {
            int position = sceneSpinner.getSelectedItemPosition();
            if (position < 0 || position >= visibleScenes.size()) return;
            SceneStore.Scene scene = visibleScenes.get(position);
            if (activeMac == null || !controllerHub.isConnected(activeMac)) {
                Snackbar.make(sceneStatus, R.string.scene_device_offline, Snackbar.LENGTH_LONG).show();
                return;
            }
            Snackbar.make(sceneStatus,
                    getString(R.string.confirm_scene_apply, scene.name, scene.onCount()),
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.confirm_action, action -> applyScene(scene))
                    .show();
        });

        deleteSceneButton.setOnClickListener(v -> {
            int position = sceneSpinner.getSelectedItemPosition();
            if (position < 0 || position >= visibleScenes.size()) return;
            sceneStore.delete(visibleScenes.get(position).id);
            refreshScenes();
            Snackbar.make(sceneStatus, R.string.scene_deleted, Snackbar.LENGTH_SHORT).show();
        });
        refreshScenes();
    }

    private void refreshScenes() {
        if (sceneSpinner == null || sceneStore == null) return;
        visibleScenes.clear();
        if (activeMac != null) visibleScenes.addAll(sceneStore.list(activeMac));
        List<String> labels = new ArrayList<>();
        for (SceneStore.Scene scene : visibleScenes) {
            labels.add(getString(R.string.scene_label_format, scene.name, scene.onCount()));
        }
        if (labels.isEmpty()) labels.add(getString(activeMac == null
                ? R.string.scenes_waiting : R.string.no_scenes));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sceneSpinner.setAdapter(adapter);
        boolean enabled = activeMac != null && !visibleScenes.isEmpty();
        applySceneButton.setEnabled(enabled);
        deleteSceneButton.setEnabled(enabled);
        saveSceneButton.setEnabled(activeMac != null && controllerHub.isConnected(activeMac));
        sceneStatus.setText(activeMac == null
                ? R.string.scenes_waiting
                : (visibleScenes.isEmpty() ? R.string.no_scenes : R.string.scene_ready));
    }

    private void applyScene(SceneStore.Scene scene) {
        if (scene == null || activeMac == null || !scene.mac.equalsIgnoreCase(activeMac)) return;
        String mac = activeMac;
        applySceneButton.setEnabled(false);
        commandWorker.execute(() -> {
            int sent = 0;
            int failures = 0;
            for (int outlet = 1; outlet <= 4; outlet++) {
                try {
                    controllerHub.setOutlet(mac, outlet, scene.outletOn(outlet));
                    sent++;
                } catch (IOException error) {
                    failures++;
                }
            }
            if (historyStore != null) {
                historyStore.recordEvent(mac, 0, "scene_apply", scene.name,
                        System.currentTimeMillis());
            }
            final int sentCount = sent;
            final int failedCount = failures;
            runOnUiThread(() -> {
                refreshScenes();
                refreshHistory();
                Snackbar.make(sceneStatus,
                        getString(R.string.scene_applied, sentCount, failedCount),
                        failedCount == 0 ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
            });
        });
    }

    private void configureHistory() {
        exportHistoryButton.setOnClickListener(v -> startHistoryExport());
        refreshHistory();
    }

    private void refreshHistory() {
        if (historyStore == null || historySparkline == null) return;
        if (activeMac == null) {
            historySparkline.setPoints(new ArrayList<>());
            historySummary.setText(R.string.history_waiting);
            historyCostSummary.setText(R.string.history_cost_waiting);
            historyRecent.setText(R.string.history_no_events);
            return;
        }
        long now = System.currentTimeMillis();
        Calendar day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);

        Calendar week = (Calendar) day.clone();
        int dow = week.get(Calendar.DAY_OF_WEEK);
        int daysSinceSunday = dow - Calendar.SUNDAY;
        week.add(Calendar.DAY_OF_MONTH, -daysSinceSunday);

        Calendar month = (Calendar) day.clone();
        month.set(Calendar.DAY_OF_MONTH, 1);

        HistoryStore.Summary daily = historyStore.summary(activeMac, day.getTimeInMillis());
        HistoryStore.Summary weekly = historyStore.summary(activeMac, week.getTimeInMillis());
        HistoryStore.Summary monthly = historyStore.summary(activeMac, month.getTimeInMillis());
        historySummary.setText(getString(R.string.history_summary_format,
                daily.energyDeltaKWh, weekly.energyDeltaKWh, monthly.energyDeltaKWh,
                daily.maxPowerW));
        double tariff = Double.longBitsToDouble(getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(PREF_TARIFF, Double.doubleToRawLongBits(0.0)));
        if (tariff > 0.0) {
            historyCostSummary.setText(getString(R.string.history_cost_format,
                    daily.energyDeltaKWh * tariff,
                    weekly.energyDeltaKWh * tariff,
                    monthly.energyDeltaKWh * tariff));
        } else {
            historyCostSummary.setText(R.string.history_cost_waiting);
        }
        historySparkline.setPoints(historyStore.recentPower(activeMac, 120));

        List<HistoryStore.EventRecord> events = historyStore.recentEvents(activeMac, 12);
        if (events.isEmpty()) {
            historyRecent.setText(R.string.history_no_events);
        } else {
            StringBuilder text = new StringBuilder();
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            for (HistoryStore.EventRecord event : events) {
                if (text.length() > 0) text.append('\n');
                text.append(format.format(new java.util.Date(event.ts)))
                        .append(" · ").append(eventKindLabel(event.kind));
                if (event.outlet > 0) text.append(" #").append(event.outlet);
                if (event.detail != null && !event.detail.isEmpty()) {
                    text.append(" · ").append(event.detail);
                }
            }
            historyRecent.setText(text.toString());
        }
    }

    private String eventKindLabel(String kind) {
        if (kind == null) return getString(R.string.event_other);
        switch (kind) {
            case "connected": return getString(R.string.event_connected);
            case "disconnected": return getString(R.string.event_disconnected);
            case "relay_state": return getString(R.string.event_relay_state);
            case "scene_apply": return getString(R.string.event_scene_apply);
            case "alert": return getString(R.string.event_alert);
            case "automation_auto_off": return getString(R.string.event_auto_off);
            case "automation_power_limit": return getString(R.string.event_power_limit);
            case "automation_standby_cutoff": return getString(R.string.event_standby_cutoff);
            case "automation_schedule": return getString(R.string.event_schedule);
            case "automation_away_toggle": return getString(R.string.event_away_toggle);
            case "away_mode_config": return getString(R.string.event_away_config);
            case "device_share_created": return getString(R.string.event_device_share);
            case "voice_command": return getString(R.string.event_voice_command);
            case "usb_discovery_started": return getString(R.string.event_usb_discovery);
            case "usb_discovery_frame": return getString(R.string.event_usb_frame);
            default: return kind;
        }
    }

    private void requestEmergencyOff(boolean roomOnly) {
        if (controllerHub == null || controllerHub.connectedStates().isEmpty()) {
            Snackbar.make(fleetStatus, R.string.no_connected_devices, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (roomOnly && selectedEmergencyRoom().isEmpty()) {
            Snackbar.make(fleetStatus, R.string.no_room_selected, Snackbar.LENGTH_LONG).show();
            return;
        }
        int targets = countEmergencyTargets(roomOnly);
        if (targets <= 0) {
            Snackbar.make(fleetStatus, R.string.no_connected_targets, Snackbar.LENGTH_LONG).show();
            return;
        }
        int message = roomOnly ? R.string.confirm_room_off_count : R.string.confirm_fleet_off_count;
        Snackbar.make(fleetStatus, getString(message, targets), Snackbar.LENGTH_LONG)
                .setAction(R.string.confirm_action, v -> executeEmergencyOff(roomOnly))
                .show();
    }

    private int countEmergencyTargets(boolean roomOnly) {
        if (controllerHub == null) return 0;
        String targetRoom = roomOnly ? selectedEmergencyRoom() : "";
        if (roomOnly && targetRoom.isEmpty()) return 0;
        int count = 0;
        for (ControllerHub.DeviceState state : controllerHub.connectedStates()) {
            if (!roomOnly) {
                count++;
                continue;
            }
            FleetStore.DeviceRecord record = fleetStore == null ? null : fleetStore.get(state.mac);
            if (record != null && targetRoom.equalsIgnoreCase(record.room)) count++;
        }
        return count;
    }

    private String selectedEmergencyRoom() {
        if (fleetRoomFilter != null && !fleetRoomFilter.trim().isEmpty()) {
            return fleetRoomFilter.trim();
        }
        FleetStore.DeviceRecord record = activeMac == null || fleetStore == null
                ? null : fleetStore.get(activeMac);
        return record == null || record.room == null ? "" : record.room.trim();
    }

    private void executeEmergencyOff(boolean roomOnly) {
        String targetRoom = roomOnly ? selectedEmergencyRoom() : "";
        List<ControllerHub.DeviceState> targets = new ArrayList<>();
        for (ControllerHub.DeviceState state : controllerHub.connectedStates()) {
            if (!roomOnly) {
                targets.add(state);
                continue;
            }
            FleetStore.DeviceRecord record = fleetStore.get(state.mac);
            if (record != null && targetRoom.equalsIgnoreCase(record.room)) targets.add(state);
        }
        if (targets.isEmpty()) {
            Snackbar.make(fleetStatus, R.string.no_connected_targets, Snackbar.LENGTH_LONG).show();
            return;
        }

        emergencyRoomOffButton.setEnabled(false);
        emergencyAllOffButton.setEnabled(false);
        commandWorker.execute(() -> {
            int commands = 0;
            int failures = 0;
            for (ControllerHub.DeviceState state : targets) {
                for (int outlet = 1; outlet <= 4; outlet++) {
                    try {
                        controllerHub.setOutlet(state.mac, outlet, false);
                        commands++;
                    } catch (IOException error) {
                        failures++;
                    }
                }
                if (historyStore != null) {
                    historyStore.recordEvent(state.mac, 0,
                            roomOnly ? "room_all_off" : "fleet_all_off",
                            roomOnly ? targetRoom : "all", System.currentTimeMillis());
                }
            }
            final int sent = commands;
            final int failed = failures;
            runOnUiThread(() -> {
                emergencyRoomOffButton.setEnabled(true);
                emergencyAllOffButton.setEnabled(true);
                Snackbar.make(fleetStatus,
                        getString(R.string.emergency_off_result, sent, failed),
                        failed == 0 ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG).show();
                refreshHistory();
            });
        });
    }

    private void startHistoryExport() {
        if (activeMac == null || historyStore == null) {
            Snackbar.make(historySummary, R.string.select_device_first, Snackbar.LENGTH_LONG).show();
            return;
        }
        pendingExportMac = FleetStore.normalizeMac(activeMac);
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "FG-RCK-" + pendingExportMac + "-history.csv");
        startActivityForResult(intent, EXPORT_HISTORY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_CONTROL_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> results =
                        data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    executeVoicePhrase(results.get(0));
                } else {
                    voiceStatus.setText(R.string.voice_no_result);
                }
            } else {
                voiceStatus.setText(R.string.voice_cancelled);
            }
            return;
        }
        if (requestCode != EXPORT_HISTORY_REQUEST || resultCode != RESULT_OK
                || data == null || data.getData() == null || pendingExportMac == null) {
            return;
        }
        Uri uri = data.getData();
        String exportMac = pendingExportMac;
        pendingExportMac = null;
        exportHistoryButton.setEnabled(false);
        commandWorker.execute(() -> {
            try (OutputStream stream = getContentResolver().openOutputStream(uri, "w")) {
                if (stream == null) throw new IOException("Could not open export destination");
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        stream, java.nio.charset.StandardCharsets.UTF_8)) {
                    historyStore.writeCsv(exportMac, writer);
                }
                runOnUiThread(() -> {
                    exportHistoryButton.setEnabled(true);
                    Snackbar.make(historySummary, R.string.history_exported,
                            Snackbar.LENGTH_SHORT).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    exportHistoryButton.setEnabled(true);
                    Snackbar.make(historySummary,
                            getString(R.string.history_export_failed, safeMessage(error)),
                            Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void configureAlertLimits() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        double power = Double.longBitsToDouble(prefs.getLong(
                MttlControllerService.PREF_ALERT_POWER_W, Double.doubleToRawLongBits(3000.0)));
        int temp = prefs.getInt(MttlControllerService.PREF_ALERT_TEMP_C, 0);
        double energy = Double.longBitsToDouble(prefs.getLong(
                MttlControllerService.PREF_ALERT_DAILY_ENERGY_KWH, Double.doubleToRawLongBits(0.0)));
        alertPowerInput.setText(String.format(Locale.US, "%.0f", power));
        if (temp > 0) alertTempInput.setText(String.valueOf(temp));
        if (energy > 0) alertEnergyInput.setText(String.format(Locale.US, "%.2f", energy));

        saveAlertLimitsButton.setOnClickListener(v -> {
            double powerLimit = parsePositiveDouble(textOf(alertPowerInput));
            int tempLimit = parsePositiveInt(textOf(alertTempInput));
            double energyLimit = parsePositiveDouble(textOf(alertEnergyInput));
            if (powerLimit <= 0) powerLimit = 3000.0;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putLong(MttlControllerService.PREF_ALERT_POWER_W,
                            Double.doubleToRawLongBits(powerLimit))
                    .putInt(MttlControllerService.PREF_ALERT_TEMP_C, tempLimit)
                    .putLong(MttlControllerService.PREF_ALERT_DAILY_ENERGY_KWH,
                            Double.doubleToRawLongBits(energyLimit))
                    .apply();
            Snackbar.make(saveAlertLimitsButton, R.string.alert_limits_saved,
                    Snackbar.LENGTH_SHORT).show();
        });
    }

    private void configureSharing() {
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(
                this, R.array.share_role_labels, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shareRoleSpinner.setAdapter(roleAdapter);

        createShareButton.setOnClickListener(v -> {
            if (activeMac == null) {
                Snackbar.make(createShareButton, R.string.share_select_device,
                        Snackbar.LENGTH_LONG).show();
                return;
            }
            AccessControlStore.Role role = roleForPosition(
                    shareRoleSpinner.getSelectedItemPosition());
            AccessControlStore.AccessEntry entry = accessStore.createScoped(
                    textOf(shareNameInput), role, activeMac);
            showAccessToken(entry);
            if (historyStore != null) {
                historyStore.recordEvent(activeMac, 0, "device_share_created",
                        role.name(), System.currentTimeMillis());
            }
            refreshSharingEntries();
        });
        createHaTokenButton.setOnClickListener(v -> {
            AccessControlStore.AccessEntry entry = accessStore.create(
                    "Home Assistant", AccessControlStore.Role.CONTROL);
            showAccessToken(entry);
            refreshSharingEntries();
        });
        revokeShareButton.setOnClickListener(v -> {
            int position = shareEntriesSpinner.getSelectedItemPosition();
            if (position < 0 || position >= visibleAccessEntries.size()) return;
            accessStore.revokeKey(visibleAccessEntries.get(position).tokenHash);
            refreshSharingEntries();
            shareTokenText.setText(R.string.share_token_waiting);
            shareCodeText.setText(R.string.share_code_waiting);
        });
        shareTokenText.setOnClickListener(v -> copySensitiveText(
                shareTokenText, "FG Machines access token", R.string.token_copied));
        shareCodeText.setOnClickListener(v -> copySensitiveText(
                shareCodeText, "FG Machines device share code", R.string.share_code_copied));
        refreshSharingEntries();
        updateApiEndpoint();
    }

    private void copySensitiveText(TextView source, String label, int confirmationMessage) {
        CharSequence value = source.getText();
        if (value == null || value.length() == 0) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        Snackbar.make(source, confirmationMessage, Snackbar.LENGTH_SHORT).show();
    }

    private void showAccessToken(AccessControlStore.AccessEntry entry) {
        shareTokenText.setText(entry.token);
        String ip = HotspotSupport.findControllerIpv4();
        if (ip != null && !entry.token.isEmpty()) {
            try {
                String code = ShareCode.encode(
                        "http://" + ip + ":" + LocalApiServer.PORT,
                        entry.token, entry.role, entry.scopeMac);
                shareCodeText.setText(code);
            } catch (IllegalArgumentException error) {
                shareCodeText.setText(R.string.share_code_unavailable);
            }
        } else {
            shareCodeText.setText(R.string.share_code_unavailable);
        }
        Snackbar.make(shareTokenText,
                getString(R.string.access_created, entry.name, entry.role.name()),
                Snackbar.LENGTH_LONG).show();
    }

    private void refreshSharingEntries() {
        visibleAccessEntries.clear();
        visibleAccessEntries.addAll(accessStore.list());
        List<String> labels = new ArrayList<>();
        for (AccessControlStore.AccessEntry entry : visibleAccessEntries) {
            String scope = entry.isDeviceScoped()
                    ? entry.scopeMac : getString(R.string.share_scope_all);
            labels.add(entry.name + " · " + entry.role.name() + " · " + scope);
        }
        if (labels.isEmpty()) labels.add(getString(R.string.no_shared_users));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shareEntriesSpinner.setAdapter(adapter);
        revokeShareButton.setEnabled(!visibleAccessEntries.isEmpty());
    }

    private AccessControlStore.Role roleForPosition(int position) {
        if (position == 2) return AccessControlStore.Role.ADMIN;
        if (position == 1) return AccessControlStore.Role.CONTROL;
        return AccessControlStore.Role.VIEW;
    }

    private void updateApiEndpoint() {
        if (apiEndpointText == null) return;
        String ip = HotspotSupport.findControllerIpv4();
        apiEndpointText.setText(ip == null
                ? getString(R.string.api_endpoint_waiting)
                : "http://" + ip + ":" + LocalApiServer.PORT);
    }

    private void configureVoiceControl() {
        voiceStatus.setText(R.string.voice_ready);
        voiceControlButton.setOnClickListener(v -> startVoiceRecognition());
    }

    private void startVoiceRecognition() {
        if (activeMac == null || !controllerHub.isConnected(activeMac)) {
            voiceStatus.setText(R.string.voice_requires_connected_device);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.voice_prompt));
        try {
            voiceStatus.setText(R.string.voice_listening);
            startActivityForResult(intent, VOICE_CONTROL_REQUEST);
        } catch (Exception error) {
            voiceStatus.setText(R.string.voice_unavailable);
        }
    }

    private void executeVoicePhrase(String phrase) {
        VoiceCommandParser.Command command = VoiceCommandParser.parse(phrase);
        if (!command.isKnown()) {
            voiceStatus.setText(getString(R.string.voice_not_understood, phrase));
            return;
        }
        if (activeMac == null || !controllerHub.isConnected(activeMac)) {
            voiceStatus.setText(R.string.voice_requires_connected_device);
            return;
        }
        if (command.type == VoiceCommandParser.Type.ALL_ON) {
            voiceStatus.setText(getString(R.string.voice_heard, phrase));
            Snackbar.make(voiceStatus, R.string.voice_confirm_all_on, Snackbar.LENGTH_LONG)
                    .setAction(R.string.confirm_action,
                            v -> sendVoiceCommand(command, phrase))
                    .show();
            return;
        }
        sendVoiceCommand(command, phrase);
    }

    private void sendVoiceCommand(VoiceCommandParser.Command command, String phrase) {
        final String mac = activeMac;
        if (mac == null) return;
        voiceControlButton.setEnabled(false);
        voiceStatus.setText(getString(R.string.voice_heard, phrase));
        commandWorker.execute(() -> {
            int sent = 0;
            int failures = 0;
            int firstOutlet = 1;
            int lastOutlet = 4;
            boolean on = command.type == VoiceCommandParser.Type.OUTLET_ON
                    || command.type == VoiceCommandParser.Type.ALL_ON;
            if (command.type == VoiceCommandParser.Type.OUTLET_ON
                    || command.type == VoiceCommandParser.Type.OUTLET_OFF) {
                firstOutlet = command.outlet;
                lastOutlet = command.outlet;
            }
            for (int outlet = firstOutlet; outlet <= lastOutlet; outlet++) {
                try {
                    controllerHub.setOutlet(mac, outlet, on);
                    sent++;
                } catch (IOException error) {
                    failures++;
                }
            }
            if (historyStore != null) {
                String safePhrase = phrase == null ? "" : phrase.trim();
                if (safePhrase.length() > 120) safePhrase = safePhrase.substring(0, 120);
                historyStore.recordEvent(mac,
                        firstOutlet == lastOutlet ? firstOutlet : 0,
                        "voice_command",
                        command.type.name() + " · " + safePhrase,
                        System.currentTimeMillis());
            }
            try { controllerHub.refresh(mac); } catch (IOException ignored) { }
            final int sentCount = sent;
            final int failureCount = failures;
            runOnUiThread(() -> {
                voiceControlButton.setEnabled(true);
                voiceStatus.setText(getString(R.string.voice_command_result,
                        sentCount, failureCount));
                refreshHistory();
            });
        });
    }

    private void configureCloudAccount() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        cloudEndpointInput.setText(prefs.getString(PREF_REMOTE_ENDPOINT, ""));
        cloudSyncSwitch.setChecked(
                prefs.getBoolean(CloudRelayManager.PREF_CLOUD_SYNC_ENABLED, false));
        String savedToken = prefs.getString(PREF_REMOTE_TOKEN, "");
        cloudStatus.setText(savedToken == null || savedToken.isEmpty()
                ? R.string.cloud_status_signed_out : R.string.cloud_status_ready);

        cloudRegisterButton.setOnClickListener(v -> authenticateCloud(true));
        cloudLoginButton.setOnClickListener(v -> authenticateCloud(false));
        cloudSyncSwitch.setOnCheckedChangeListener((button, checked) -> {
            String endpoint = textOf(cloudEndpointInput);
            String token = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getString(PREF_REMOTE_TOKEN, "");
            if (checked && (endpoint.isEmpty() || token == null || token.isEmpty())) {
                cloudStatus.setText(R.string.cloud_sync_requires_login);
                button.setChecked(false);
                return;
            }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(CloudRelayManager.PREF_CLOUD_SYNC_ENABLED, checked)
                    .putString(PREF_REMOTE_ENDPOINT, endpoint)
                    .apply();
            cloudStatus.setText(checked
                    ? R.string.cloud_status_ready : R.string.cloud_status_signed_out);
        });
    }

    private void authenticateCloud(boolean createAccount) {
        String endpoint = textOf(cloudEndpointInput);
        String email = textOf(cloudEmailInput);
        String password = textOf(cloudPasswordInput);
        if (endpoint.isEmpty() || email.isEmpty() || password.isEmpty()) {
            cloudStatus.setText(R.string.cloud_status_missing);
            return;
        }

        cloudRegisterButton.setEnabled(false);
        cloudLoginButton.setEnabled(false);
        cloudStatus.setText(R.string.cloud_status_authenticating);
        commandWorker.execute(() -> {
            try {
                CloudApiClient api = new CloudApiClient(endpoint);
                CloudApiClient.AuthSession session = createAccount
                        ? api.register(email, password) : api.login(email, password);
                if (session.accessToken.isEmpty()) {
                    throw new IOException("Cloud login returned an empty access token");
                }

                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                String previousEndpoint = prefs.getString(PREF_REMOTE_ENDPOINT, "");
                SharedPreferences.Editor editor = prefs.edit()
                        .putString(PREF_REMOTE_ENDPOINT, endpoint)
                        .putString(PREF_REMOTE_TOKEN, session.accessToken);
                if (previousEndpoint != null && !previousEndpoint.isEmpty()
                        && !previousEndpoint.equals(endpoint)) {
                    editor.remove(CloudRelayManager.PREF_CLOUD_CONTROLLER_ID)
                            .remove(CloudRelayManager.PREF_CLOUD_CONTROLLER_KEY)
                            .remove(CloudRelayManager.PREF_CLOUD_REGISTERED_MACS);
                }
                editor.apply();

                runOnUiThread(() -> {
                    remoteEndpointInput.setText(endpoint);
                    remoteTokenInput.setText(session.accessToken);
                    cloudPasswordInput.setText("");
                    cloudStatus.setText(R.string.cloud_status_ready);
                    cloudRegisterButton.setEnabled(true);
                    cloudLoginButton.setEnabled(true);
                });
            } catch (IOException error) {
                runOnUiThread(() -> {
                    cloudStatus.setText(getString(
                            R.string.cloud_status_failed, safeMessage(error)));
                    cloudRegisterButton.setEnabled(true);
                    cloudLoginButton.setEnabled(true);
                });
            }
        });
    }

    private void configureRemoteControl() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        remoteEndpointInput.setText(prefs.getString(PREF_REMOTE_ENDPOINT, ""));
        remoteTokenInput.setText(prefs.getString(PREF_REMOTE_TOKEN, ""));
        remoteStatus.setText(textOf(remoteEndpointInput).isEmpty()
                ? R.string.cloud_waiting_for_vps : R.string.remote_not_connected);

        ArrayAdapter<CharSequence> outletAdapter = ArrayAdapter.createFromResource(
                this, R.array.remote_outlet_labels, android.R.layout.simple_spinner_item);
        outletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        remoteOutletSpinner.setAdapter(outletAdapter);

        importShareCodeButton.setOnClickListener(v -> importDeviceShareCode());
        remoteRefreshButton.setOnClickListener(v -> refreshRemoteDevices());
        remoteOnButton.setOnClickListener(v -> sendRemoteOutlet(true));
        remoteOffButton.setOnClickListener(v -> sendRemoteOutlet(false));
        remoteOnButton.setEnabled(false);
        remoteOffButton.setEnabled(false);
    }

    private void importDeviceShareCode() {
        try {
            ShareCode.Profile profile = ShareCode.parse(textOf(remoteShareCodeInput));
            remoteEndpointInput.setText(profile.endpoint);
            remoteTokenInput.setText(profile.token);
            remoteStatus.setText(profile.isDeviceScoped()
                    ? getString(R.string.share_code_imported_device, profile.scopeMac)
                    : getString(R.string.share_code_imported_all));
            refreshRemoteDevices();
        } catch (IllegalArgumentException error) {
            remoteStatus.setText(R.string.share_code_invalid);
        }
    }

    private void refreshRemoteDevices() {
        String endpoint = textOf(remoteEndpointInput);
        String token = textOf(remoteTokenInput);
        if (endpoint.isEmpty()) {
            remoteStatus.setText(R.string.cloud_waiting_for_vps);
            remoteOnButton.setEnabled(false);
            remoteOffButton.setEnabled(false);
            return;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_REMOTE_ENDPOINT, endpoint)
                .putString(PREF_REMOTE_TOKEN, token)
                .apply();
        remoteStatus.setText(R.string.remote_connecting);
        remoteRefreshButton.setEnabled(false);
        commandWorker.execute(() -> {
            try {
                List<RemoteApiClient.RemoteDevice> result =
                        new RemoteApiClient(endpoint, token).listDevices();
                runOnUiThread(() -> {
                    remoteDevices.clear();
                    remoteDevices.addAll(result);
                    List<String> labels = new ArrayList<>();
                    for (RemoteApiClient.RemoteDevice device : result) labels.add(device.toString());
                    if (labels.isEmpty()) labels.add(getString(R.string.remote_no_devices));
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this, android.R.layout.simple_spinner_item, labels);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    remoteDeviceSpinner.setAdapter(adapter);
                    boolean available = !result.isEmpty();
                    remoteOnButton.setEnabled(available);
                    remoteOffButton.setEnabled(available);
                    remoteStatus.setText(available
                            ? getString(R.string.remote_connected_count, result.size())
                            : getString(R.string.remote_no_devices));
                    remoteRefreshButton.setEnabled(true);
                });
            } catch (IOException error) {
                runOnUiThread(() -> {
                    remoteStatus.setText(getString(R.string.remote_failed, safeMessage(error)));
                    remoteOnButton.setEnabled(false);
                    remoteOffButton.setEnabled(false);
                    remoteRefreshButton.setEnabled(true);
                });
            }
        });
    }

    private void sendRemoteOutlet(boolean on) {
        int devicePosition = remoteDeviceSpinner.getSelectedItemPosition();
        int outlet = remoteOutletSpinner.getSelectedItemPosition() + 1;
        if (devicePosition < 0 || devicePosition >= remoteDevices.size()) return;
        RemoteApiClient.RemoteDevice device = remoteDevices.get(devicePosition);
        String endpoint = textOf(remoteEndpointInput);
        String token = textOf(remoteTokenInput);
        remoteStatus.setText(R.string.remote_sending);
        commandWorker.execute(() -> {
            try {
                new RemoteApiClient(endpoint, token).setOutlet(device.mac, outlet, on);
                runOnUiThread(() -> remoteStatus.setText(getString(
                        R.string.remote_command_sent, device.name, outlet,
                        on ? getString(R.string.remote_on) : getString(R.string.remote_off))));
            } catch (IOException error) {
                runOnUiThread(() -> remoteStatus.setText(
                        getString(R.string.remote_failed, safeMessage(error))));
            }
        });
    }

    private static String formatDuration(long millis) {
        long minutes = Math.max(0, millis / 60000L);
        long hours = minutes / 60;
        long remaining = minutes % 60;
        return hours > 0 ? hours + "h " + remaining + "m" : remaining + "m";
    }

    private void configureAlerts() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        alertsSwitch.setChecked(prefs.getBoolean(PREF_ALERTS_ENABLED, true));
        alertsSwitch.setOnCheckedChangeListener((button, checked) -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(PREF_ALERTS_ENABLED, checked)
                    .apply();
            if (checked) requestNotificationPermissionIfNeeded();
        });
        if (alertsSwitch.isChecked()) requestNotificationPermissionIfNeeded();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void configureAboutLinks() {
        fgMachinesFacebookButton.setOnClickListener(v -> openExternalUrl(FG_MACHINES_FACEBOOK_URL));
        alaaMohamedFacebookButton.setOnClickListener(v -> openExternalUrl(ALAA_MOHAMED_FACEBOOK_URL));
    }

    private void openExternalUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (RuntimeException error) {
            Snackbar.make(alaaMohamedFacebookButton,
                    getString(R.string.open_link_failed), Snackbar.LENGTH_LONG).show();
        }
    }

    private void configureSetupWorkflow() {
        openHotspotButton.setOnClickListener(v -> HotspotSupport.openSystemHotspotSettings(this));
        refreshHotspotButton.setOnClickListener(v -> captureHotspotAddress());
        openWifiButton.setOnClickListener(v -> HotspotSupport.openWifiSettings(this));
        provisionButton.setOnClickListener(v -> requestWifiSetupPermission(() -> provisionDevice(false)));
        manualProvisionButton.setOnClickListener(v -> requestWifiSetupPermission(() -> provisionDevice(true)));
    }


    private void configureSetupReadiness() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                provisioningSucceeded = false;
                updateSetupReadiness();
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        setupSsidInput.addTextChangedListener(watcher);
        targetWifiSsidInput.addTextChangedListener(watcher);
        targetWifiPasswordInput.addTextChangedListener(watcher);
        controllerIpInput.addTextChangedListener(watcher);
        setupGuardCheck.setOnCheckedChangeListener((button, checked) -> {
            provisioningSucceeded = false;
            updateSetupReadiness();
        });
        updateSetupReadiness();
    }

    private void updateSetupReadiness() {
        if (setupProgress == null || setupReadinessText == null) return;

        boolean controllerReady = isValidIpv4(textOf(controllerIpInput));
        boolean networkReady = !textOf(setupSsidInput).isEmpty()
                && !textOf(targetWifiSsidInput).isEmpty()
                && isValidWpa2Password(textOf(targetWifiPasswordInput));
        boolean securityReady = setupGuardCheck != null && setupGuardCheck.isChecked();
        boolean networkAndControllerReady = controllerReady && networkReady;
        boolean readyToWrite = networkAndControllerReady && securityReady;

        int progressValue = 25;
        if (controllerReady) progressValue = 50;
        if (networkAndControllerReady) progressValue = 75;
        if (readyToWrite) progressValue = 90;
        if (provisioningSucceeded) progressValue = 100;
        setupProgress.setProgressCompat(progressValue, true);

        applyStepStatus(step1Status, controllerReady,
                controllerReady ? R.string.wizard_ready : R.string.wizard_waiting);
        applyStepStatus(step2Status, networkReady,
                networkReady ? R.string.wizard_ready : R.string.wizard_waiting);
        applyStepStatus(step3Status, provisioningSucceeded,
                provisioningSucceeded ? R.string.wizard_done
                        : (readyToWrite ? R.string.wizard_ready : R.string.wizard_locked));

        if (provisioningSucceeded) {
            setupReadinessText.setText(R.string.wizard_complete);
            setupReadinessText.setTextColor(getColor(R.color.fg_green));
        } else if (!controllerReady) {
            setupReadinessText.setText(R.string.wizard_need_controller);
            setupReadinessText.setTextColor(getColor(R.color.fg_warning));
        } else if (!networkReady) {
            setupReadinessText.setText(R.string.wizard_need_network);
            setupReadinessText.setTextColor(getColor(R.color.fg_warning));
        } else if (!securityReady) {
            setupReadinessText.setText(R.string.wizard_need_security);
            setupReadinessText.setTextColor(getColor(R.color.fg_warning));
        } else {
            setupReadinessText.setText(R.string.wizard_ready_to_write);
            setupReadinessText.setTextColor(getColor(R.color.fg_green));
        }

        boolean canProvision = readyToWrite && !provisionBusy;
        manualProvisionButton.setEnabled(canProvision);
        if (provisionButton.getVisibility() == View.VISIBLE) provisionButton.setEnabled(canProvision);
    }

    private void applyStepStatus(TextView view, boolean positive, int labelRes) {
        if (view == null) return;
        view.setText(labelRes);
        view.setTextColor(getColor(positive ? R.color.fg_green : R.color.fg_silver_dark));
        view.setAlpha(positive ? 1f : 0.88f);
    }

    private static boolean isValidIpv4(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String[] parts = value.trim().split("\\.");
        if (parts.length != 4) return false;
        for (String part : parts) {
            try {
                if (part.isEmpty() || (part.length() > 1 && part.startsWith("0"))) return false;
                int n = Integer.parseInt(part);
                if (n < 0 || n > 255) return false;
            } catch (NumberFormatException error) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidWpa2Password(String value) {
        if (value == null) return false;
        String password = value.trim();
        if (password.length() >= 8 && password.length() <= 63) return true;
        return password.length() == 64 && password.matches("[0-9A-Fa-f]{64}");
    }

    private void restoreSetupProfile() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setupSsidInput.setText(prefs.getString(PREF_SETUP_SSID, ""));
        targetWifiSsidInput.setText(prefs.getString(PREF_TARGET_SSID, ""));
        String savedIp = prefs.getString(PREF_HOTSPOT_IP, "");
        if (savedIp != null && !savedIp.isEmpty()) controllerIpInput.setText(savedIp);
        if (setupModeSpinner != null) {
            int savedMode = prefs.getInt(PREF_SETUP_MODE, SETUP_MODE_ROUTER);
            if (savedMode < SETUP_MODE_ROUTER || savedMode > SETUP_MODE_SINGLE_PHONE) savedMode = SETUP_MODE_ROUTER;
            setupModeSpinner.setSelection(savedMode, false);
            applySetupMode(savedMode);
        }
        updateSetupReadiness();
    }

    private void persistSetupProfile() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_SETUP_SSID, textOf(setupSsidInput))
                .putString(PREF_TARGET_SSID, textOf(targetWifiSsidInput))
                .putInt(PREF_SETUP_MODE, setupModeSpinner == null ? SETUP_MODE_ROUTER : setupModeSpinner.getSelectedItemPosition())
                .apply();
    }

    private void captureHotspotAddress() {
        String controllerIp = HotspotSupport.findControllerIpv4();
        if (controllerIp == null) {
            Snackbar.make(refreshHotspotButton, R.string.hotspot_ip_missing, Snackbar.LENGTH_LONG).show();
            updateHotspotStatus(false);
            return;
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_HOTSPOT_IP, controllerIp)
                .apply();
        controllerIpInput.setText(controllerIp);
        persistSetupProfile();
        updateHotspotStatus(false);
        Snackbar.make(refreshHotspotButton,
                getString(R.string.hotspot_ip_saved, controllerIp), Snackbar.LENGTH_LONG).show();
    }

    private void updateHotspotStatus(boolean overwriteWhenMissing) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(PREF_HOTSPOT_IP, "");
        String current = HotspotSupport.findControllerIpv4();
        String currentText = current == null ? getString(R.string.hotspot_ip_unknown) : current;
        String savedText = saved == null || saved.isEmpty() ? getString(R.string.hotspot_ip_unknown) : saved;
        hotspotStatus.setText(getString(R.string.hotspot_status, currentText, savedText));
        if (overwriteWhenMissing && (saved == null || saved.isEmpty()) && current != null) {
            controllerIpInput.setText(current);
        }
    }

    private void provisionDevice(boolean sequential) {
        String setupSsid = textOf(setupSsidInput);
        String wifiSsid = textOf(targetWifiSsidInput);
        String wifiPassword = textOf(targetWifiPasswordInput);
        String controllerIp = textOf(controllerIpInput);
        if (setupSsid.isEmpty() || wifiSsid.isEmpty() || controllerIp.isEmpty()) {
            Snackbar.make(manualProvisionButton, R.string.missing_setup_fields, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (!isValidWpa2Password(wifiPassword)) {
            targetWifiPasswordInput.setError(getString(R.string.wpa2_password_invalid));
            Snackbar.make(manualProvisionButton, R.string.wpa2_password_invalid, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (setupGuardCheck == null || !setupGuardCheck.isChecked()) {
            Snackbar.make(manualProvisionButton, R.string.setup_guard_required, Snackbar.LENGTH_LONG).show();
            return;
        }
        persistSetupProfile();
        provisioningSucceeded = false;
        setProvisionBusy(true);
        provisionStatus.setText(R.string.provisioning);
        updateSetupReadiness();
        int setupMode = setupModeSpinner == null ? SETUP_MODE_ROUTER : setupModeSpinner.getSelectedItemPosition();
        MttlProvisioner.Callback callback = provisionCallback(sequential, setupMode);
        if (sequential) {
            provisioner.provisionCurrentWifi(setupSsid, wifiSsid, wifiPassword, controllerIp, callback);
        } else {
            provisioner.provision(setupSsid, wifiSsid, wifiPassword, controllerIp, callback);
        }
    }

    private MttlProvisioner.Callback provisionCallback(boolean sequential, int setupMode) {
        return new MttlProvisioner.Callback() {
            @Override public void onStatus(String status) {
                runOnUiThread(() -> provisionStatus.setText(status));
            }

            @Override public void onComplete() {
                runOnUiThread(() -> {
                    provisioningSucceeded = true;
                    setProvisionBusy(false);
                    updateSetupReadiness();
                    provisionStatus.setText(sequential
                            ? successMessageForMode(setupMode)
                            : R.string.provision_complete_detail);
                    deviceState.setText(R.string.provision_complete);
                    discoveryDetail.setText(sequential
                            ? successMessageForMode(setupMode)
                            : R.string.provision_complete_detail);
                    if (sequential) {
                        if (setupMode == SETUP_MODE_ROUTER) {
                            HotspotSupport.openWifiSettings(MainActivity.this);
                        } else if (setupMode == SETUP_MODE_SINGLE_PHONE) {
                            HotspotSupport.openSystemHotspotSettings(MainActivity.this);
                        }
                    }
                });
            }

            @Override public void onError(String message, Throwable error) {
                runOnUiThread(() -> {
                    provisioningSucceeded = false;
                    setProvisionBusy(false);
                    updateSetupReadiness();
                    String detail = error == null ? message : message + ": " + safeMessage(error);
                    provisionStatus.setText(getString(R.string.provision_failed, detail));
                    Snackbar.make(manualProvisionButton,
                            getString(R.string.provision_failed, detail), Snackbar.LENGTH_LONG).show();
                });
            }
        };
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
            Snackbar.make(manualProvisionButton, R.string.wifi_permission_required, Snackbar.LENGTH_LONG).show();
        }
    }

    private void setProvisionBusy(boolean busy) {
        provisionBusy = busy;
        openHotspotButton.setEnabled(!busy);
        refreshHotspotButton.setEnabled(!busy);
        openWifiButton.setEnabled(!busy);
        setupModeSpinner.setEnabled(!busy);
        setupSsidInput.setEnabled(!busy);
        targetWifiSsidInput.setEnabled(!busy);
        targetWifiPasswordInput.setEnabled(!busy);
        controllerIpInput.setEnabled(!busy);
        setupGuardCheck.setEnabled(!busy);
        updateSetupReadiness();
    }

    private void configureSetupModeSelector() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.setup_mode_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setupModeSpinner.setAdapter(adapter);

        int savedMode = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_SETUP_MODE, SETUP_MODE_ROUTER);
        if (savedMode < SETUP_MODE_ROUTER || savedMode > SETUP_MODE_SINGLE_PHONE) {
            savedMode = SETUP_MODE_ROUTER;
        }
        setupModeSpinner.setSelection(savedMode, false);
        applySetupMode(savedMode);
        setupModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < SETUP_MODE_ROUTER || position > SETUP_MODE_SINGLE_PHONE) return;
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putInt(PREF_SETUP_MODE, position).apply();
                provisioningSucceeded = false;
                applySetupMode(position);
                updateSetupReadiness();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void applySetupMode(int mode) {
        if (setupModeDescription == null || setupModeBadge == null) return;
        if (mode == SETUP_MODE_TWO_PHONE) {
            setupModeDescription.setText(R.string.setup_mode_two_phone_desc);
            setupModeBadge.setText(R.string.setup_recommended);
            openHotspotButton.setVisibility(View.VISIBLE);
            provisionButton.setVisibility(View.GONE);
        } else if (mode == SETUP_MODE_SINGLE_PHONE) {
            setupModeDescription.setText(R.string.setup_mode_single_desc);
            setupModeBadge.setText(R.string.setup_experimental);
            openHotspotButton.setVisibility(View.VISIBLE);
            provisionButton.setVisibility(View.VISIBLE);
        } else {
            setupModeDescription.setText(R.string.setup_mode_router_desc);
            setupModeBadge.setText(R.string.setup_recommended);
            openHotspotButton.setVisibility(View.GONE);
            provisionButton.setVisibility(View.GONE);
        }
    }

    private int successMessageForMode(int mode) {
        if (mode == SETUP_MODE_TWO_PHONE) return R.string.two_phone_success;
        if (mode == SETUP_MODE_SINGLE_PHONE) return R.string.single_phone_success;
        return R.string.router_success;
    }

    private void configureLanguageSelector() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.language_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String selectedLanguage = prefs.getString(PREF_LANGUAGE, "");
        if (selectedLanguage == null || selectedLanguage.isEmpty()) {
            selectedLanguage = getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        languageSpinner.setSelection(languageIndex(selectedLanguage), false);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= LANGUAGE_TAGS.length) return;
                String requested = LANGUAGE_TAGS[position];
                String current = getSharedPreferences(PREFS, MODE_PRIVATE)
                        .getString(PREF_LANGUAGE, getResources().getConfiguration().getLocales().get(0).getLanguage());
                if (!requested.equals(current)) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(PREF_LANGUAGE, requested).apply();
                    recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private int languageIndex(String language) {
        for (int i = 0; i < LANGUAGE_TAGS.length; i++) if (LANGUAGE_TAGS[i].equals(language)) return i;
        return 1;
    }

    private void configureUsbHardware() {
        usbPort1Status.setText(R.string.usb_port_1_status);
        usbPort2Status.setText(R.string.usb_port_2_status);
        startUsbDiscoveryButton.setOnClickListener(v -> startUsbDiscovery());
        refreshUsbDiscoveryButton.setOnClickListener(v -> refreshUsbDiscoveryStatus());
        refreshUsbDiscoveryStatus();
    }

    private void startUsbDiscovery() {
        String mac = activeMac == null ? "" : FleetStore.normalizeMac(activeMac);
        if (mac.isEmpty() || controllerHub == null || !controllerHub.isConnected(mac)) {
            Snackbar.make(startUsbDiscoveryButton,
                    R.string.usb_discovery_requires_device, Snackbar.LENGTH_LONG).show();
            return;
        }

        long now = System.currentTimeMillis();
        usbDiscoveryStore.start(mac, now);
        if (historyStore != null) {
            historyStore.recordEvent(mac, 0, "usb_discovery_started",
                    UsbHardwareProfile.evidenceSummary(), now);
        }
        refreshUsbDiscoveryStatus();
        Snackbar.make(startUsbDiscoveryButton,
                R.string.usb_discovery_started, Snackbar.LENGTH_LONG).show();
    }

    private void refreshUsbDiscoveryStatus() {
        if (usbDiscoveryStatus == null || usbDiscoveryStore == null) return;
        String mac = activeMac == null ? "" : FleetStore.normalizeMac(activeMac);
        if (mac.isEmpty()) {
            usbDiscoveryStatus.setText(R.string.usb_discovery_select_device);
            return;
        }

        long now = System.currentTimeMillis();
        UsbDiscoveryStore.Snapshot snapshot = usbDiscoveryStore.snapshot(mac, now);
        if (!snapshot.sameDevice) {
            usbDiscoveryStatus.setText(R.string.usb_discovery_idle);
            return;
        }

        if (snapshot.active) {
            long seconds = (snapshot.remainingMs(now) + 999L) / 1000L;
            String last = snapshot.lastFrame.isEmpty()
                    ? getString(R.string.usb_discovery_no_frames)
                    : snapshot.lastFrame;
            usbDiscoveryStatus.setText(getString(
                    R.string.usb_discovery_active_format,
                    seconds, snapshot.frameCount, last));
        } else {
            String last = snapshot.lastFrame.isEmpty()
                    ? getString(R.string.usb_discovery_no_frames)
                    : snapshot.lastFrame;
            usbDiscoveryStatus.setText(getString(
                    R.string.usb_discovery_complete_format,
                    snapshot.frameCount, last));
        }
    }

    private void configureOutletControls() {
        setOutletControlsEnabled(false);
        for (int i = 0; i < outletSwitches.length; i++) {
            final int outlet = i + 1;
            outletSwitches[i].setOnCheckedChangeListener((button, checked) -> {
                if (applyingDeviceState || !button.isEnabled()) return;
                String mac = activeMac;
                if (mac == null || controllerHub == null) return;
                commandWorker.execute(() -> {
                    try {
                        controllerHub.setOutlet(mac, outlet, checked);
                    } catch (IOException error) {
                        runOnUiThread(() -> Snackbar.make(scanButton,
                                getString(R.string.command_failed, safeMessage(error)), Snackbar.LENGTH_LONG).show());
                    }
                });
            });
        }
    }

    private void startLocalController() {
        controllerListener = new MttlControllerServer.Listener() {
            @Override public void onListening(int port) {
                runOnUiThread(() -> {
                    if (activeMac == null) {
                        deviceState.setText(R.string.controller_listening);
                        discoveryDetail.setText(R.string.scan_explanation);
                    }
                });
            }

            @Override public void onDeviceConnected(MttlProtocol.BootInfo bootInfo, String remoteAddress) {
                String key = FleetStore.normalizeMac(bootInfo.mac);
                fleetStore.register(key, bootInfo.firmwareVersion, System.currentTimeMillis());
                String preferred = fleetStore.selectedMac();
                if (activeMac == null) {
                    if (!preferred.isEmpty() && controllerHub.isConnected(preferred)) {
                        activeMac = preferred;
                    } else {
                        activeMac = key;
                        fleetStore.select(key);
                    }
                }
                if (key.equalsIgnoreCase(activeMac)) activeFirmwareVersion = bootInfo.firmwareVersion;
                runOnUiThread(() -> {
                    refreshFleetUi();
                    if (key.equalsIgnoreCase(activeMac)) selectFleetDevice(activeMac);
                    else updateFleetStatus();
                });
            }

            @Override public void onDeviceDisconnected(String mac) {
                String key = FleetStore.normalizeMac(mac);
                boolean selectedDisconnected = key.equalsIgnoreCase(activeMac == null ? "" : activeMac);
                if (selectedDisconnected) {
                    List<ControllerHub.DeviceState> connected = controllerHub.connectedStates();
                    if (connected.isEmpty()) {
                        activeMac = null;
                        activeFirmwareVersion = null;
                    } else {
                        activeMac = connected.get(0).mac;
                        activeFirmwareVersion = connected.get(0).firmwareVersion;
                        fleetStore.select(activeMac);
                    }
                }
                runOnUiThread(() -> {
                    refreshFleetUi();
                    if (activeMac == null) {
                        setOutletControlsEnabled(false);
                        clearTelemetryUi();
                        clearDeviceNamingFields();
                        deviceState.setText(R.string.controller_disconnected);
                        discoveryDetail.setText(R.string.locked);
                        refreshFleetOverviewAndList();
                        loadAwayModeForActiveDevice();
                        refreshHistory();
                        refreshRuntimeSummary();
                    } else if (selectedDisconnected) {
                        selectFleetDevice(activeMac);
                    } else {
                        updateFleetStatus();
                    }
                });
            }

            @Override public void onOutletState(String mac, MttlProtocol.OutletState state) {
                if (!isActive(mac)) return;
                runOnUiThread(() -> applyOutletState(state.outlet, state.on));
            }

            @Override public void onTelemetry(String mac, MttlProtocol.Telemetry telemetry) {
                if (!isActive(mac)) return;
                runOnUiThread(() -> {
                    applyingDeviceState = true;
                    try {
                        for (MttlProtocol.OutletTelemetry outlet : telemetry.outlets) {
                            if (outlet.channel >= 1 && outlet.channel <= outletSwitches.length) {
                                outletSwitches[outlet.channel - 1].setChecked(outlet.relayOn);
                            }
                        }
                        updateTelemetryUi(telemetry);
                        updateFleetStatus();
                        refreshFleetOverviewAndList();
                        refreshHistory();
                        refreshRuntimeSummary();
                    } finally { applyingDeviceState = false; }
                });
            }

            @Override public void onProtocolFrame(String mac, String frame) { }

            @Override public void onError(String message, Throwable error) {
                runOnUiThread(() -> {
                    deviceState.setText(getString(R.string.controller_error, safeMessage(error)));
                    if (activeMac == null) setOutletControlsEnabled(false);
                });
            }
        };
        controllerHub.addListener(controllerListener, true);
        try {
            controllerHub.start();
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
        try { outletSwitches[outlet - 1].setChecked(on); }
        finally { applyingDeviceState = false; }
    }

    private void setOutletControlsEnabled(boolean enabled) {
        for (MaterialSwitch outletSwitch : outletSwitches) outletSwitch.setEnabled(enabled);
    }

    private void configureHardwareIdentification() {
        if (identifyHardwareButton == null || hardwareIdentityInput == null) return;

        identifyHardwareButton.setOnClickListener(v -> identifyHardware());
        hardwareIdentityInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                identifyHardware();
                return true;
            }
            return false;
        });
        if (hardwareCatalogSummary != null) {
            hardwareCatalogSummary.setText(R.string.hardware_catalog_summary);
        }
        if (hardwareIdentityResult != null) {
            hardwareIdentityResult.setText(R.string.hardware_identity_waiting);
        }
    }

    private void identifyHardware() {
        String label = textOf(hardwareIdentityInput);
        if (label.isEmpty()) {
            hardwareIdentityInput.setError(getString(R.string.hardware_identity_required));
            return;
        }

        HardwareCatalog.Profile profile = HardwareCatalog.identifyFromLabel(label);
        if (profile == null) {
            hardwareIdentityResult.setText(R.string.hardware_identity_unknown);
            return;
        }

        if (profile.requiresGateway()) {
            hardwareIdentityResult.setText(getString(
                    R.string.hardware_identity_gateway_format,
                    profile.manufacturer,
                    profile.displayModel,
                    profile.usbPortCount,
                    profile.ratedVac,
                    profile.ratedA,
                    profile.maxPowerW));
        } else {
            hardwareIdentityResult.setText(getString(
                    R.string.hardware_identity_local_format,
                    profile.manufacturer,
                    profile.displayModel));
        }
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

    private static String safeMessage(Throwable error) {
        if (error == null) return "unknown";
        String value = error.getMessage();
        return value == null || value.trim().isEmpty() ? error.getClass().getSimpleName() : value;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hotspotStatus != null) updateHotspotStatus(false);
        updateSetupReadiness();
        updateAutomationSummary();
        loadAwayModeForActiveDevice();
        refreshRuntimeSummary();
        refreshFleetUi();
        refreshUsbDiscoveryStatus();
        refreshHistory();
        refreshSharingEntries();
        updateApiEndpoint();
    }

    @Override
    protected void onDestroy() {
        if (provisioner != null) provisioner.close();
        if (controllerHub != null && controllerListener != null) {
            controllerHub.removeListener(controllerListener);
        }
        commandWorker.shutdownNow();
        if (historyStore != null) historyStore.close();
        super.onDestroy();
    }
}
