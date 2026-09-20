package com.pmr.admin;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/* Экран настроек V2.1.
 * Разделы:
 * 1) Смена пароля.
 * 2) Частота обновления списка (поле ввода вручную).
 * 3) Требовать пароль (чекбокс).
 * 4) Подключение 26 региона (кнопки 260/261).
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText passCurrent;
    private EditText passNew;
    private EditText passConfirm;
    private EditText refreshInput;
    private CheckBox requirePassBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        passCurrent = findViewById(R.id.passCurrent);
        passNew     = findViewById(R.id.passNew);
        passConfirm = findViewById(R.id.passConfirm);
        refreshInput = findViewById(R.id.refreshInput);
        requirePassBox = findViewById(R.id.requirePassBox);

        Button saveBtn = findViewById(R.id.btnSaveSettings);
        if (saveBtn != null) saveBtn.setOnClickListener(v -> saveSettings());

        Button btn260 = findViewById(R.id.btn260);
        Button btn261 = findViewById(R.id.btn261);
        if (btn260 != null) btn260.setOnClickListener(v -> {
            if (PmrService.pmrSocket != null) PmrService.pmrSocket.send260(0);
        });
        if (btn261 != null) btn261.setOnClickListener(v -> {
            if (PmrService.pmrSocket != null) PmrService.pmrSocket.send260(1);
        });

        loadSettings();
    }

    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(
                PasswordActivity.PREFS, MODE_PRIVATE);

        int r = sp.getInt(PasswordActivity.KEY_REFRESH,
                PasswordActivity.DEFAULT_REFRESH);
        if (refreshInput != null) {
            refreshInput.setText(String.valueOf(r));
        }

        boolean requirePass = sp.getBoolean(
                PasswordActivity.KEY_REQUIRE_PASSWORD, true);
        if (requirePassBox != null) {
            requirePassBox.setChecked(requirePass);
        }
    }

    private void saveSettings() {
        SharedPreferences sp = getSharedPreferences(
                PasswordActivity.PREFS, MODE_PRIVATE);

        String cur = sp.getString(PasswordActivity.KEY_PASSWORD,
                PasswordActivity.DEFAULT_PASSWORD);

        String enteredCur = passCurrent.getText().toString();
        String newPass = passNew.getText().toString();
        String confirmPass = passConfirm.getText().toString();

        /* Смена пароля — если хотя бы одно поле заполнено */
        if (!enteredCur.isEmpty() || !newPass.isEmpty() || !confirmPass.isEmpty()) {
            if (!cur.equals(enteredCur)) {
                Toast.makeText(this, R.string.settings_error_current,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPass.isEmpty()) {
                Toast.makeText(this, R.string.settings_error_empty,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, R.string.settings_error_mismatch,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            sp.edit().putString(PasswordActivity.KEY_PASSWORD, newPass).apply();
        }

        /* Частота обновления — читаем из поля ввода */
        int refresh = PasswordActivity.DEFAULT_REFRESH;
        try {
            String rs = refreshInput.getText().toString().trim();
            if (!rs.isEmpty()) {
                int v = Integer.parseInt(rs);
                if (v >= 1 && v <= 60) {
                    refresh = v;
                } else {
                    Toast.makeText(this, "Частота: 1..60",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Частота: число",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        sp.edit().putInt(PasswordActivity.KEY_REFRESH, refresh).apply();

        /* Требовать пароль */
        boolean requirePass = requirePassBox != null && requirePassBox.isChecked();
        sp.edit().putBoolean(PasswordActivity.KEY_REQUIRE_PASSWORD,
                requirePass).apply();

        Toast.makeText(this, R.string.settings_saved,
                Toast.LENGTH_SHORT).show();

        passCurrent.setText("");
        passNew.setText("");
        passConfirm.setText("");
    }
}