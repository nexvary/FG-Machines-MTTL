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
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

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

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String language = prefs.getString(PREF_LANGUAGE, "");
        if (language == null || language.isEmpty()) {
            language = newBase.getResources().getConfiguration().getLocales().get(0).getLanguage();
        }
        if (!isSupportedLanguage(language)) {
            language = "en";
        }
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

        configureLanguageSelector();

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
        int selectedIndex = languageIndex(selectedLanguage);
        languageSpinner.setSelection(selectedIndex, false);

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
            public void onNothingSelected(AdapterView<?> parent) {
                // Keep the current locale.
            }
        });
    }

    private int languageIndex(String language) {
        for (int i = 0; i < LANGUAGE_TAGS.length; i++) {
            if (LANGUAGE_TAGS[i].equals(language)) return i;
        }
        return 1;
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
}
