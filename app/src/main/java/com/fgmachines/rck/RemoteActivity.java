package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RemoteActivity extends AppCompatActivity {
    private enum Category { AC, FAN }

    private static final String PREFS = "fg_rck_settings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_AC_BRAND = "remote_ac_brand";
    private static final String PREF_AC_PROTOCOL_PREFIX = "remote_ac_protocol_";
    private static final String PREF_FAN_TARGET = "remote_fan_target";
    private static final String PREF_FAN_PROFILE_PREFIX = "remote_fan_profile_";
    private static final String[] LANGUAGE_TAGS = {"ar", "en", "tr", "es", "de"};

    private TextView irStatus;
    private TextView categoryStatus;
    private TextView profileStatus;
    private TextView tempValue;
    private MaterialButton brandButton;
    private MaterialButton fanTargetButton;
    private MaterialButton scanButton;
    private View acControls;
    private View fanControls;

    private IrTransmitter transmitter;
    private Category category = Category.AC;
    private DeviceProfile.Brand activeBrand = DeviceProfile.Brand.SHARP;
    private DeviceProfile.Protocol activeProtocol;
    private String fanTarget;
    private FanRemoteProfile fanProfile;
    private int tempC = 24;
    private boolean powerOn;

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
        setContentView(R.layout.activity_remote);
        applySystemBarInsets();

        irStatus = findViewById(R.id.remoteIrStatus);
        categoryStatus = findViewById(R.id.remoteCategoryStatus);
        profileStatus = findViewById(R.id.remoteProfileStatus);
        tempValue = findViewById(R.id.remoteTempValue);
        brandButton = findViewById(R.id.remoteBrandButton);
        fanTargetButton = findViewById(R.id.remoteFanTargetButton);
        scanButton = findViewById(R.id.remoteScanButton);
        acControls = findViewById(R.id.remoteAcControls);
        fanControls = findViewById(R.id.remoteFanControls);

        transmitter = new IrTransmitter(this);
        boolean available = transmitter.available();
        irStatus.setText(available
                ? getString(R.string.remote_ir_available) + " · " + transmitter.frequencySummary()
                : getString(R.string.remote_ir_unavailable));
        irStatus.setAlpha(available ? 1f : 0.78f);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        try {
            activeBrand = DeviceProfile.Brand.valueOf(
                    prefs.getString(PREF_AC_BRAND, DeviceProfile.Brand.SHARP.name()));
        } catch (RuntimeException ignored) {
            activeBrand = DeviceProfile.Brand.SHARP;
        }
        fanTarget = prefs.getString(PREF_FAN_TARGET, FanRemoteProfile.targets().get(0));
        activeProtocol = loadAcProtocol(activeBrand);
        fanProfile = loadFanProfile(fanTarget);

        findViewById(R.id.remoteBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.remoteAcButton).setOnClickListener(v -> setCategory(Category.AC));
        findViewById(R.id.remoteFanButton).setOnClickListener(v -> setCategory(Category.FAN));
        brandButton.setOnClickListener(v -> chooseAcBrand());
        fanTargetButton.setOnClickListener(v -> chooseFanTarget());
        scanButton.setOnClickListener(v -> startSmartScan());

        findViewById(R.id.remotePowerOnButton).setOnClickListener(v -> sendAcState(true, false));
        findViewById(R.id.remotePowerOffButton).setOnClickListener(v -> sendAcState(false, false));
        findViewById(R.id.remoteTempDownButton).setOnClickListener(v -> changeTemp(-1));
        findViewById(R.id.remoteTempUpButton).setOnClickListener(v -> changeTemp(1));
        findViewById(R.id.remoteSwingButton).setOnClickListener(v -> sendAcState(true, true));

        findViewById(R.id.remoteFanPowerButton).setOnClickListener(v -> sendFanPower());
        findViewById(R.id.remoteFanSpeedButton).setOnClickListener(v -> sendFanCode("speed"));
        findViewById(R.id.remoteFanSwingButton).setOnClickListener(v -> sendFanCode("swing"));
        findViewById(R.id.remoteFanTimerButton).setOnClickListener(v -> sendFanCode("timer"));
        findViewById(R.id.remoteFanSleepButton).setOnClickListener(v -> sendFanCode("sleep"));

        String uiCategory = getIntent().getStringExtra("fg_ui_remote_category");
        setCategory("fan".equals(uiCategory) ? Category.FAN : Category.AC);
        updateTemp();
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.remoteRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void setCategory(Category value) {
        category = value;
        boolean ac = value == Category.AC;
        brandButton.setVisibility(ac ? View.VISIBLE : View.GONE);
        fanTargetButton.setVisibility(ac ? View.GONE : View.VISIBLE);
        acControls.setVisibility(ac ? View.VISIBLE : View.GONE);
        fanControls.setVisibility(ac ? View.GONE : View.VISIBLE);
        categoryStatus.setText(ac ? R.string.remote_ac_selected : R.string.remote_fan_selected);
        if (ac) {
            brandButton.setText(DeviceProfile.brandLabel(activeBrand));
            showAcProfileStatus();
        } else {
            fanTargetButton.setText(fanTarget);
            showFanProfileStatus();
        }
    }

    private void chooseAcBrand() {
        DeviceProfile.Brand[] brands = DeviceProfile.Brand.values();
        String[] labels = new String[brands.length];
        for (int i = 0; i < brands.length; i++) labels[i] = DeviceProfile.brandLabel(brands[i]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.remote_choose_ac_brand)
                .setItems(labels, (d, which) -> {
                    activeBrand = brands[which];
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(PREF_AC_BRAND, activeBrand.name()).apply();
                    activeProtocol = loadAcProtocol(activeBrand);
                    brandButton.setText(labels[which]);
                    showAcProfileStatus();
                })
                .setNegativeButton(R.string.remote_scan_cancel, null)
                .show();
    }

    private void chooseFanTarget() {
        List<String> targets = FanRemoteProfile.targets();
        new AlertDialog.Builder(this)
                .setTitle(R.string.remote_choose_fan_model)
                .setItems(targets.toArray(new String[0]), (d, which) -> {
                    fanTarget = targets.get(which);
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(PREF_FAN_TARGET, fanTarget).apply();
                    fanProfile = loadFanProfile(fanTarget);
                    fanTargetButton.setText(fanTarget);
                    showFanProfileStatus();
                })
                .setNegativeButton(R.string.remote_scan_cancel, null)
                .show();
    }

    private DeviceProfile.Protocol loadAcProtocol(DeviceProfile.Brand brand) {
        String value = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(PREF_AC_PROTOCOL_PREFIX + brand.name(), "");
        if (value == null || value.isEmpty()) return null;
        try { return DeviceProfile.Protocol.valueOf(value); }
        catch (RuntimeException error) { return null; }
    }

    private DeviceProfile.Protocol defaultProtocol(DeviceProfile.Brand brand) {
        switch (brand) {
            case SHARP: return DeviceProfile.Protocol.SHARP_A907;
            case MIDEA: return DeviceProfile.Protocol.MIDEA_48;
            case CARRIER: return DeviceProfile.Protocol.CARRIER_64;
            case GREE: return DeviceProfile.Protocol.GREE_YAW1F;
            case LG: return DeviceProfile.Protocol.LG_28;
            case SAMSUNG: return DeviceProfile.Protocol.SAMSUNG_AC;
            case HAIER: return DeviceProfile.Protocol.HAIER_YRW02;
            case TOSHIBA: return DeviceProfile.Protocol.TOSHIBA_GENERIC;
            case TCL: return DeviceProfile.Protocol.TCL112;
            case HISENSE: return DeviceProfile.Protocol.KELON168;
            default: return null;
        }
    }

    private void saveAcProtocol(DeviceProfile.Protocol protocol) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_AC_PROTOCOL_PREFIX + activeBrand.name(), protocol.name()).apply();
    }

    private FanRemoteProfile loadFanProfile(String target) {
        String direct = null;
        if (target != null && target.startsWith("Kanazawa")) direct = "kanazawa_tower";
        if (target != null && target.startsWith("Atomberg")) direct = "atomberg_bldc";
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString(PREF_FAN_PROFILE_PREFIX + safeKey(target), direct == null ? "" : direct);
        return FanRemoteProfile.byId(saved);
    }

    private static String safeKey(String value) {
        return Integer.toHexString(value == null ? 0 : value.hashCode());
    }

    private void saveFanProfile(FanRemoteProfile profile) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_FAN_PROFILE_PREFIX + safeKey(fanTarget), profile.id).apply();
    }

    private void showAcProfileStatus() {
        if (activeProtocol == null) {
            profileStatus.setText(R.string.remote_scan_required);
        } else {
            profileStatus.setText(getString(R.string.remote_profile_ready,
                    DeviceProfile.brandLabel(activeBrand),
                    DeviceProfile.protocolLabel(activeProtocol)));
        }
        setAcControlsEnabled(activeProtocol != null && transmitter.available());
    }

    private void showFanProfileStatus() {
        if (fanProfile == null) {
            profileStatus.setText(R.string.remote_fan_scan_required);
        } else {
            profileStatus.setText(getString(R.string.remote_fan_profile_ready,
                    fanTarget, fanProfile.label()));
        }
        setFanControlsEnabled(fanProfile != null && transmitter.available());
    }

    private void startSmartScan() {
        if (!transmitter.available()) {
            profileStatus.setText(R.string.remote_ir_unavailable);
            return;
        }
        if (category == Category.AC) {
            scanAcCandidate(acCandidates(activeBrand), 0);
        } else {
            List<FanRemoteProfile> profiles = FanRemoteProfile.verifiedProfiles();
            scanFanCandidate(profiles, 0);
        }
    }

    private DeviceProfile.Protocol[] acCandidates(DeviceProfile.Brand brand) {
        switch (brand) {
            case SHARP:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.SHARP_A907,
                        DeviceProfile.Protocol.SHARP_A903,DeviceProfile.Protocol.SHARP_A705};
            case MIDEA:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.MIDEA_48};
            case CARRIER:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.CARRIER_64,
                        DeviceProfile.Protocol.TOSHIBA_GENERIC,DeviceProfile.Protocol.MIDEA_48};
            case GREE:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.GREE_YAW1F,
                        DeviceProfile.Protocol.GREE_YBOFB,DeviceProfile.Protocol.GREE_YX1FSF};
            case LG:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.LG_28,
                        DeviceProfile.Protocol.LG2_28};
            case SAMSUNG:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.SAMSUNG_AC};
            case HAIER:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.HAIER_YRW02};
            case TOSHIBA:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.TOSHIBA_GENERIC,
                        DeviceProfile.Protocol.TOSHIBA_WA_TH0X};
            case TCL:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.TCL112};
            case HISENSE:
                return new DeviceProfile.Protocol[]{DeviceProfile.Protocol.KELON168};
            case UNIONAIR:
            case FRESH:
            case OTHER:
            default:
                return allAcCandidates();
        }
    }

    private DeviceProfile.Protocol[] allAcCandidates() {
        return new DeviceProfile.Protocol[]{
                DeviceProfile.Protocol.MIDEA_48,DeviceProfile.Protocol.CARRIER_64,
                DeviceProfile.Protocol.GREE_YAW1F,DeviceProfile.Protocol.GREE_YBOFB,
                DeviceProfile.Protocol.LG_28,DeviceProfile.Protocol.LG2_28,
                DeviceProfile.Protocol.SAMSUNG_AC,DeviceProfile.Protocol.HAIER_YRW02,
                DeviceProfile.Protocol.TOSHIBA_GENERIC,DeviceProfile.Protocol.TOSHIBA_WA_TH0X,
                DeviceProfile.Protocol.TCL112,DeviceProfile.Protocol.KELON168,
                DeviceProfile.Protocol.SHARP_A907,DeviceProfile.Protocol.SHARP_A903,
                DeviceProfile.Protocol.SHARP_A705
        };
    }

    private void scanAcCandidate(DeviceProfile.Protocol[] list, int index) {
        if (index >= list.length) {
            profileStatus.setText(R.string.remote_scan_no_match);
            return;
        }
        DeviceProfile.Protocol candidate = list[index];
        try {
            transmitter.send(buildAcSignal(candidate, true, 24, false));
        } catch (RuntimeException error) {
            profileStatus.setText(getString(R.string.ac_remote_send_failed,
                    error.getClass().getSimpleName()));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_scan_title, index + 1, list.length))
                .setMessage(getString(R.string.remote_scan_question,
                        DeviceProfile.protocolLabel(candidate)))
                .setPositiveButton(R.string.remote_scan_yes, (d, w) -> {
                    activeProtocol = candidate;
                    powerOn = true;
                    tempC = 24;
                    saveAcProtocol(candidate);
                    showAcProfileStatus();
                    updateTemp();
                })
                .setNegativeButton(R.string.remote_scan_no, (d, w) ->
                        scanAcCandidate(list, index + 1))
                .setNeutralButton(R.string.remote_scan_cancel, null)
                .show();
    }

    private void scanFanCandidate(List<FanRemoteProfile> profiles, int index) {
        if (index >= profiles.size()) {
            profileStatus.setText(R.string.remote_fan_scan_no_match);
            return;
        }
        FanRemoteProfile candidate = profiles.get(index);
        try {
            transmitter.send(candidate.signal(candidate.powerOn));
        } catch (RuntimeException error) {
            profileStatus.setText(getString(R.string.ac_remote_send_failed,
                    error.getClass().getSimpleName()));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_scan_title, index + 1, profiles.size()))
                .setMessage(getString(R.string.remote_fan_scan_question, candidate.label()))
                .setPositiveButton(R.string.remote_scan_yes, (d, w) -> {
                    fanProfile = candidate;
                    saveFanProfile(candidate);
                    showFanProfileStatus();
                })
                .setNegativeButton(R.string.remote_scan_no, (d, w) ->
                        scanFanCandidate(profiles, index + 1))
                .setNeutralButton(R.string.remote_scan_cancel, null)
                .show();
    }

    private void changeTemp(int delta) {
        if (activeProtocol == null) return;
        tempC = Math.max(16, Math.min(30, tempC + delta));
        updateTemp();
        if (powerOn) sendAcState(true, false);
    }

    private void updateTemp() {
        tempValue.setText(getString(R.string.remote_temp_value, tempC));
    }

    private void sendAcState(boolean on, boolean swing) {
        if (activeProtocol == null) {
            profileStatus.setText(R.string.remote_scan_required);
            return;
        }
        try {
            transmitter.send(buildAcSignal(activeProtocol, on, tempC, swing));
            powerOn = on;
            profileStatus.setText(getString(R.string.ac_remote_command_sent,
                    DeviceProfile.protocolLabel(activeProtocol)));
        } catch (RuntimeException error) {
            profileStatus.setText(getString(R.string.ac_remote_send_failed,
                    error.getClass().getSimpleName()));
        }
    }

    private IrSignal buildAcSignal(DeviceProfile.Protocol protocol, boolean on, int temp, boolean swing) {
        switch (protocol) {
            case MIDEA_48:
                if (swing) return new MideaAcProtocol().swingToggle();
                return new MideaAcProtocol().power(on).temp(temp)
                        .mode(MideaAcProtocol.Mode.COOL).fan(MideaAcProtocol.Fan.AUTO);
            case CARRIER_64:
                return new Carrier64AcProtocol().power(on).temp(temp)
                        .mode(Carrier64AcProtocol.Mode.COOL).fan(Carrier64AcProtocol.Fan.AUTO)
                        .swing(swing);
            case GREE_YAW1F:
            case GREE_YBOFB:
            case GREE_YX1FSF:
                GreeAcProtocol.Model gm=protocol==DeviceProfile.Protocol.GREE_YBOFB?
                        GreeAcProtocol.Model.YBOFB:protocol==DeviceProfile.Protocol.GREE_YX1FSF?
                        GreeAcProtocol.Model.YX1FSF:GreeAcProtocol.Model.YAW1F;
                return new GreeAcProtocol().model(gm).power(on).temp(temp)
                        .mode(GreeAcProtocol.Mode.COOL).fan(GreeAcProtocol.Fan.AUTO).swing(swing);
            case LG_28:
            case LG2_28:
                LgAcProtocol lg=new LgAcProtocol()
                        .model(protocol==DeviceProfile.Protocol.LG2_28?LgAcProtocol.Model.LG2:LgAcProtocol.Model.LG)
                        .power(on).temp(temp).mode(LgAcProtocol.Mode.COOL).fan(LgAcProtocol.Fan.AUTO);
                return swing?lg.swingToggle():lg;
            case SAMSUNG_AC:
                return new SamsungAcProtocol().power(on).temp(temp)
                        .mode(SamsungAcProtocol.Mode.COOL).fan(SamsungAcProtocol.Fan.AUTO)
                        .swing(swing);
            case HAIER_YRW02:
                return new HaierAcProtocol().power(on).temp(temp)
                        .mode(HaierAcProtocol.Mode.COOL).fan(HaierAcProtocol.Fan.AUTO).swing(swing);
            case TOSHIBA_GENERIC:
            case TOSHIBA_WA_TH0X:
                return new ToshibaAcProtocol()
                        .model(protocol==DeviceProfile.Protocol.TOSHIBA_WA_TH0X?
                                ToshibaAcProtocol.Model.WA_TH0X:ToshibaAcProtocol.Model.GENERIC)
                        .power(on).temp(temp).mode(ToshibaAcProtocol.Mode.COOL)
                        .fan(ToshibaAcProtocol.Fan.AUTO).swing(swing);
            case TCL112:
                return new TclAcProtocol().power(on).temp(temp)
                        .mode(TclAcProtocol.Mode.COOL).fan(TclAcProtocol.Fan.AUTO).swing(swing);
            case KELON168:
                return new Kelon168AcProtocol().power(on).temp(temp)
                        .mode(Kelon168AcProtocol.Mode.COOL).fan(Kelon168AcProtocol.Fan.AUTO)
                        .swing(swing);
            case SHARP_A903:
            case SHARP_A705:
            case SHARP_A907:
            default:
                SharpAcProtocol.Model sm=protocol==DeviceProfile.Protocol.SHARP_A903?
                        SharpAcProtocol.Model.A903:protocol==DeviceProfile.Protocol.SHARP_A705?
                        SharpAcProtocol.Model.A705:SharpAcProtocol.Model.A907;
                SharpAcProtocol sharp=new SharpAcProtocol().model(sm)
                        .mode(SharpAcProtocol.Mode.COOL).temp(temp).fan(SharpAcProtocol.Fan.AUTO);
                if(swing)return sharp.swingToggle();
                return sharp.power(on,powerOn);
        }
    }

    private void sendFanPower() {
        if (fanProfile == null) { profileStatus.setText(R.string.remote_fan_scan_required); return; }
        Long code = powerOn ? fanProfile.powerOff : fanProfile.powerOn;
        if (code == null) code = fanProfile.powerOn;
        sendFanSignal(code, R.string.remote_fan_power);
        powerOn = !powerOn;
    }

    private void sendFanCode(String action) {
        if (fanProfile == null) { profileStatus.setText(R.string.remote_fan_scan_required); return; }
        Long code = "speed".equals(action)?fanProfile.speed:
                "swing".equals(action)?fanProfile.swing:
                "timer".equals(action)?fanProfile.timer:fanProfile.sleep;
        if (code == null) {
            profileStatus.setText(R.string.remote_fan_command_unavailable);
            return;
        }
        sendFanSignal(code, R.string.remote_fan_command);
    }

    private void sendFanSignal(Long code, int labelRes) {
        try {
            transmitter.send(fanProfile.signal(code));
            profileStatus.setText(getString(R.string.remote_fan_command_sent,
                    getString(labelRes), fanProfile.label()));
        } catch (RuntimeException error) {
            profileStatus.setText(getString(R.string.ac_remote_send_failed,
                    error.getClass().getSimpleName()));
        }
    }

    private void setAcControlsEnabled(boolean enabled) {
        int[] ids={R.id.remotePowerOnButton,R.id.remotePowerOffButton,R.id.remoteTempDownButton,
                R.id.remoteTempUpButton,R.id.remoteSwingButton};
        for(int id:ids){View v=findViewById(id);v.setEnabled(enabled);v.setAlpha(enabled?1f:0.5f);}
    }

    private void setFanControlsEnabled(boolean enabled) {
        int[] ids={R.id.remoteFanPowerButton,R.id.remoteFanSpeedButton,R.id.remoteFanSwingButton,
                R.id.remoteFanTimerButton,R.id.remoteFanSleepButton};
        for(int id:ids){View v=findViewById(id);v.setEnabled(enabled);v.setAlpha(enabled?1f:0.5f);}
    }
}
