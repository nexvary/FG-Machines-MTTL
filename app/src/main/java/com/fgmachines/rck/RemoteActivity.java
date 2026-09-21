package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public final class RemoteActivity extends AppCompatActivity {
    private static final String PREFS = "fg_rck_settings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_SHARP_PROTOCOL = "remote_sharp_protocol";
    private static final String PREF_UNION_PROTOCOL = "remote_union_protocol";
    private static final String[] LANGUAGE_TAGS = {"ar", "en", "tr", "es", "de"};

    private TextView irStatus;
    private TextView categoryStatus;
    private TextView profileStatus;
    private TextView tempValue;
    private MaterialButton scanButton;
    private MaterialButton powerOnButton;
    private MaterialButton powerOffButton;
    private MaterialButton tempDownButton;
    private MaterialButton tempUpButton;
    private MaterialButton swingButton;

    private IrTransmitter transmitter;
    private DeviceProfile.Brand activeBrand = DeviceProfile.Brand.SHARP;
    private DeviceProfile.Protocol activeProtocol;
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
        scanButton = findViewById(R.id.remoteScanButton);
        powerOnButton = findViewById(R.id.remotePowerOnButton);
        powerOffButton = findViewById(R.id.remotePowerOffButton);
        tempDownButton = findViewById(R.id.remoteTempDownButton);
        tempUpButton = findViewById(R.id.remoteTempUpButton);
        swingButton = findViewById(R.id.remoteSwingButton);

        transmitter = new IrTransmitter(this);
        boolean available = transmitter.available();
        irStatus.setText(available
                ? getString(R.string.remote_ir_available) + " · " + transmitter.frequencySummary()
                : getString(R.string.remote_ir_unavailable));
        irStatus.setAlpha(available ? 1f : 0.78f);

        findViewById(R.id.remoteBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.remoteAcButton).setOnClickListener(v ->
                categoryStatus.setText(R.string.remote_ac_selected));
        findViewById(R.id.remoteFanButton).setOnClickListener(v ->
                categoryStatus.setText(R.string.remote_fan_pending));

        findViewById(R.id.remoteSharpButton).setOnClickListener(v ->
                selectBrand(DeviceProfile.Brand.SHARP));
        findViewById(R.id.remoteUnionAirButton).setOnClickListener(v ->
                selectBrand(DeviceProfile.Brand.UNIONAIR));

        scanButton.setOnClickListener(v -> startSmartScan());
        powerOnButton.setOnClickListener(v -> sendState(true, false));
        powerOffButton.setOnClickListener(v -> sendState(false, false));
        tempDownButton.setOnClickListener(v -> changeTemp(-1));
        tempUpButton.setOnClickListener(v -> changeTemp(1));
        swingButton.setOnClickListener(v -> sendState(true, true));

        selectBrand(DeviceProfile.Brand.SHARP);
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

    private void selectBrand(DeviceProfile.Brand brand) {
        activeBrand = brand;
        activeProtocol = loadProtocol(brand);
        String brandName = brand == DeviceProfile.Brand.SHARP ? "Sharp" : "UnionAir";
        categoryStatus.setText(getString(R.string.remote_brand_selected, brandName));
        if (activeProtocol == null) {
            profileStatus.setText(R.string.remote_scan_required);
        } else {
            profileStatus.setText(getString(
                    R.string.remote_profile_ready,
                    brandName,
                    DeviceProfile.protocolLabel(activeProtocol)));
        }
        setControlsEnabled(activeProtocol != null && transmitter.available());
    }

    private DeviceProfile.Protocol loadProtocol(DeviceProfile.Brand brand) {
        String key = brand == DeviceProfile.Brand.SHARP ? PREF_SHARP_PROTOCOL : PREF_UNION_PROTOCOL;
        String value = getSharedPreferences(PREFS, MODE_PRIVATE).getString(key, "");
        if (value == null || value.isEmpty()) return null;
        try {
            return DeviceProfile.Protocol.valueOf(value);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private void saveProtocol(DeviceProfile.Protocol protocol) {
        String key = activeBrand == DeviceProfile.Brand.SHARP
                ? PREF_SHARP_PROTOCOL : PREF_UNION_PROTOCOL;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(key, protocol.name())
                .apply();
    }

    private DeviceProfile.Protocol[] candidates() {
        if (activeBrand == DeviceProfile.Brand.SHARP) {
            return new DeviceProfile.Protocol[]{
                    DeviceProfile.Protocol.SHARP_A907,
                    DeviceProfile.Protocol.SHARP_A903,
                    DeviceProfile.Protocol.SHARP_A705
            };
        }
        return new DeviceProfile.Protocol[]{
                DeviceProfile.Protocol.MIDEA_48,
                DeviceProfile.Protocol.CARRIER_64,
                DeviceProfile.Protocol.SHARP_A907,
                DeviceProfile.Protocol.SHARP_A903,
                DeviceProfile.Protocol.SHARP_A705
        };
    }

    private void startSmartScan() {
        if (!transmitter.available()) {
            profileStatus.setText(R.string.remote_ir_unavailable);
            return;
        }
        scanCandidate(candidates(), 0);
    }

    private void scanCandidate(DeviceProfile.Protocol[] list, int index) {
        if (index >= list.length) {
            profileStatus.setText(R.string.remote_scan_no_match);
            return;
        }
        DeviceProfile.Protocol candidate = list[index];
        try {
            transmitter.send(buildSignal(candidate, true, 24, false));
        } catch (RuntimeException error) {
            profileStatus.setText(getString(R.string.remote_send_failed, error.getClass().getSimpleName()));
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.remote_scan_title, index + 1, list.length))
                .setMessage(getString(
                        R.string.remote_scan_question,
                        DeviceProfile.protocolLabel(candidate)))
                .setPositiveButton(R.string.remote_scan_yes, (dialog, which) -> {
                    activeProtocol = candidate;
                    powerOn = true;
                    tempC = 24;
                    saveProtocol(candidate);
                    profileStatus.setText(getString(
                            R.string.remote_profile_ready,
                            activeBrand == DeviceProfile.Brand.SHARP ? "Sharp" : "UnionAir",
                            DeviceProfile.protocolLabel(candidate)));
                    updateTemp();
                    setControlsEnabled(true);
                })
                .setNegativeButton(R.string.remote_scan_no, (dialog, which) ->
                        scanCandidate(list, index + 1))
                .setNeutralButton(R.string.remote_scan_cancel, null)
                .show();
    }

    private void changeTemp(int delta) {
        if (activeProtocol == null) return;
        tempC = Math.max(17, Math.min(30, tempC + delta));
        updateTemp();
        if (powerOn) sendState(true, false);
    }

    private void updateTemp() {
        tempValue.setText(getString(R.string.remote_temp_value, tempC));
    }

    private void sendState(boolean on, boolean swing) {
        if (activeProtocol == null) {
            profileStatus.setText(R.string.remote_scan_required);
            return;
        }
        try {
            transmitter.send(buildSignal(activeProtocol, on, tempC, swing));
            powerOn = on;
            profileStatus.setText(getString(
                    R.string.remote_command_sent,
                    DeviceProfile.protocolLabel(activeProtocol)));
        } catch (RuntimeException error) {
            profileStatus.setText(getString(R.string.remote_send_failed, error.getClass().getSimpleName()));
        }
    }

    private IrSignal buildSignal(DeviceProfile.Protocol protocol, boolean on, int temp, boolean swing) {
        switch (protocol) {
            case MIDEA_48:
                if (swing) return new MideaAcProtocol().swingToggle();
                return new MideaAcProtocol()
                        .power(on)
                        .temp(temp)
                        .mode(MideaAcProtocol.Mode.COOL)
                        .fan(MideaAcProtocol.Fan.AUTO);
            case CARRIER_64:
                return new Carrier64AcProtocol()
                        .power(on)
                        .temp(temp)
                        .mode(Carrier64AcProtocol.Mode.COOL)
                        .fan(Carrier64AcProtocol.Fan.AUTO)
                        .swing(swing);
            case SHARP_A903:
            case SHARP_A705:
            case SHARP_A907:
            default:
                SharpAcProtocol.Model model = protocol == DeviceProfile.Protocol.SHARP_A903
                        ? SharpAcProtocol.Model.A903
                        : protocol == DeviceProfile.Protocol.SHARP_A705
                        ? SharpAcProtocol.Model.A705
                        : SharpAcProtocol.Model.A907;
                SharpAcProtocol sharp = new SharpAcProtocol()
                        .model(model)
                        .mode(SharpAcProtocol.Mode.COOL)
                        .temp(temp)
                        .fan(SharpAcProtocol.Fan.AUTO);
                if (swing) return sharp.swingToggle();
                return sharp.power(on, powerOn);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        powerOnButton.setEnabled(enabled);
        powerOffButton.setEnabled(enabled);
        tempDownButton.setEnabled(enabled);
        tempUpButton.setEnabled(enabled);
        swingButton.setEnabled(enabled);
        float alpha = enabled ? 1f : 0.5f;
        powerOnButton.setAlpha(alpha);
        powerOffButton.setAlpha(alpha);
        tempDownButton.setAlpha(alpha);
        tempUpButton.setAlpha(alpha);
        swingButton.setAlpha(alpha);
    }
}
