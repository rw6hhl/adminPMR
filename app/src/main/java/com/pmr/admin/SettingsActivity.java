package com.pmr.admin;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/* Экран настроек V2.3.
 * Разделы:
 *  1) Смена пароля.
 *  2) Частота обновления списка (секунды).
 *  3) Порт приёма UDP.
 *  4) Требовать пароль при запуске.
 *  5) Показывать проверку системы при запуске.
 *  6) Одна кнопка управления 260/261.
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText passCurrent;
    private EditText passNew;
    private EditText passConfirm;
    private EditText refreshInput;
    private EditText portInput;
    private CheckBox requirePassBox;
    private CheckBox checkSystemBox;
    private Button btn26;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        passCurrent = findViewById(R.id.passCurrent);
        passNew     = findViewById(R.id.passNew);
        passConfirm = findViewById(R.id.passConfirm);
        refreshInput = findViewById(R.id.refreshInput);
        portInput = findViewById(R.id.portInput);
        requirePassBox = findViewById(R.id.requirePassBox);
        checkSystemBox = findViewById(R.id.checkSystemBox);
        btn26 = findViewById(R.id.btn26);

        Button saveBtn = findViewById(R.id.btnSaveSettings);
        if (saveBtn != null) saveBtn.setOnClickListener(v -> saveSettings());

        if (btn26 != null) btn26.setOnClickListener(v -> toggle26());

        loadSettings();
    }

    private void loadSettings() {
        SharedPreferences sp = getSharedPreferences(
                PasswordActivity.PREFS, MODE_PRIVATE);

        int r = sp.getInt(PasswordActivity.KEY_REFRESH,
                PasswordActivity.DEFAULT_REFRESH);
        if (refreshInput != null) refreshInput.setText(String.valueOf(r));

        int p = sp.getInt(PasswordActivity.KEY_PORT_PRM,
                PasswordActivity.DEFAULT_PORT_PRM);
        if (portInput != null) portInput.setText(String.valueOf(p));

        boolean requirePass = sp.getBoolean(
                PasswordActivity.KEY_REQUIRE_PASSWORD, true);
        if (requirePassBox != null) requirePassBox.setChecked(requirePass);

        boolean checkSystem = sp.getBoolean(
                PasswordActivity.KEY_CHECK_SYSTEM, true);
        if (checkSystemBox != null) checkSystemBox.setChecked(checkSystem);

        boolean on26 = sp.getBoolean(PasswordActivity.KEY_26_STATE, false);
        updateBtn26(on26);
    }

    private void toggle26() {
        SharedPreferences sp = getSharedPreferences(
                PasswordActivity.PREFS, MODE_PRIVATE);
        boolean on26 = sp.getBoolean(PasswordActivity.KEY_26_STATE, false);
        boolean newState = !on26;

        if (PmrService.pmrSocket == null) {
            Toast.makeText(this, R.string.toast_26_null,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        PmrService.pmrSocket.send260(newState ? 1 : 0);

        sp.edit().putBoolean(PasswordActivity.KEY_26_STATE, newState).apply();
        updateBtn26(newState);

        Toast.makeText(this,
                newState ? R.string.toast_26_on : R.string.toast_26_off,
                Toast.LENGTH_SHORT).show();
    }

    private void updateBtn26(boolean on) {
        if (btn26 == null) return;
        if (on) {
            btn26.setText(R.string.btn_26_on);
            btn26.setBackgroundTintList(getColorStateList(R.color.c_green));
        } else {
            btn26.setText(R.string.btn_26_off);
            btn26.setBackgroundTintList(getColorStateList(R.color.c_red));
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

        /* Пароль */
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

        /* Частота обновления */
        int refresh = PasswordActivity.DEFAULT_REFRESH;
        try {
            String rs = refreshInput.getText().toString().trim();
            if (!rs.isEmpty()) {
                int v = Integer.parseInt(rs);
                if (v >= 1 && v <= 60) refresh = v;
                else {
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

        /* Порт */
        int port = PasswordActivity.DEFAULT_PORT_PRM;
        try {
            String ps = portInput.getText().toString().trim();
            if (!ps.isEmpty()) {
                int v = Integer.parseInt(ps);
                if (v >= 1024 && v <= 65535) port = v;
                else {
                    Toast.makeText(this, "Порт: 1024..65535",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Порт: число",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        sp.edit().putInt(PasswordActivity.KEY_PORT_PRM, port).apply();

        /* Требовать пароль */
        boolean requirePass = requirePassBox != null && requirePassBox.isChecked();
        sp.edit().putBoolean(PasswordActivity.KEY_REQUIRE_PASSWORD,
                requirePass).apply();

        /* Показывать проверку системы */
        boolean checkSystem = checkSystemBox != null && checkSystemBox.isChecked();
        sp.edit().putBoolean(PasswordActivity.KEY_CHECK_SYSTEM,
                checkSystem).apply();

        Toast.makeText(this, R.string.settings_saved,
                Toast.LENGTH_SHORT).show();

        passCurrent.setText("");
        passNew.setText("");
        passConfirm.setText("");
    }
}