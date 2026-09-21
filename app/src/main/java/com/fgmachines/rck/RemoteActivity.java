package com.fgmachines.rck;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public final class RemoteActivity extends AppCompatActivity {
    private static final String PREFS = "fg_rck_settings";
    private static final String PREF_LANGUAGE = "language";
    private static final String[] LANGUAGE_TAGS = {"ar", "en", "tr", "es", "de"};

    private TextView irStatus;
    private TextView categoryStatus;

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
        MaterialButton acButton = findViewById(R.id.remoteAcButton);
        MaterialButton fanButton = findViewById(R.id.remoteFanButton);

        findViewById(R.id.remoteBackButton).setOnClickListener(v -> finish());
        acButton.setOnClickListener(v -> selectCategory(getString(R.string.remote_ac_category)));
        fanButton.setOnClickListener(v -> selectCategory(getString(R.string.remote_fan_category)));

        ConsumerIrManager irManager =
                (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        boolean available = irManager != null && irManager.hasIrEmitter();
        irStatus.setText(available ? R.string.remote_ir_available : R.string.remote_ir_unavailable);
        irStatus.setAlpha(available ? 1f : 0.78f);
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.remoteRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void selectCategory(String label) {
        categoryStatus.setText(getString(R.string.remote_category_selected, label)
                + "\n" + getString(R.string.remote_profile_status));
    }
}
