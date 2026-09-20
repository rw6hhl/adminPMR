package com.pmr.admin;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/* Экран настроек.
 * Разделы:
 * 1) Смена пароля.
 * 2) Частота обновления списка.
 * 3) Подключение 26 региона к Ростовскому репитеру (кнопки 260/261).
 */
public class SettingsActivity extends AppCompatActivity {

    private EditText passCurrent;
    private EditText passNew;
    private EditText passConfirm;
    private RadioGroup refreshGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        passCurrent = findViewById(R.id.passCurrent);
        passNew     = findViewById(R.id.passNew);
        passConfirm = findViewById(R.id.passConfirm);
        refreshGroup = findViewById(R.id.refreshGroup);

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
        SharedPreferences sp = getSharedPreferences(PasswordActivity.PREFS, MODE_PRIVATE);
        int r = sp.getInt(PasswordActivity.KEY_REFRESH,
                PasswordActivity.DEFAULT_REFRESH);
        switch (r) {
            case 3: refreshGroup.check(R.id.rb3); break;
            case 5: refreshGroup.check(R.id.rb5); break;
            default: refreshGroup.check(R.id.rb2); break;
        }
    }

    private void saveSettings() {
        SharedPreferences sp = getSharedPreferences(PasswordActivity.PREFS, MODE_PRIVATE);
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

        /* Частота обновления */
        int refresh = 2;
        if (refreshGroup.getCheckedRadioButtonId() == R.id.rb3) refresh = 3;
        else if (refreshGroup.getCheckedRadioButtonId() == R.id.rb5) refresh = 5;
        sp.edit().putInt(PasswordActivity.KEY_REFRESH, refresh).apply();

        Toast.makeText(this, R.string.settings_saved,
                Toast.LENGTH_SHORT).show();

        passCurrent.setText("");
        passNew.setText("");
        passConfirm.setText("");
    }
}