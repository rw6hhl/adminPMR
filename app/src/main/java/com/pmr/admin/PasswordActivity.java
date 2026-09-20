package com.pmr.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/* Экран ввода пароля V2.6.
 * Экран НЕ ГАСНЕТ, пока приложение открыто (FLAG_KEEP_SCREEN_ON).
 */
public class PasswordActivity extends AppCompatActivity {

    public static final String PREFS = "admin_pmr_prefs";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_REFRESH = "refresh_sec";
    public static final String KEY_REQUIRE_PASSWORD = "require_password";
    public static final String KEY_PORT_PRM = "port_prm";
    public static final String KEY_CHECK_SYSTEM = "check_system";
    public static final String KEY_26_STATE = "state_26";

    public static final String DEFAULT_PASSWORD = "Rostov2026";
    public static final int    DEFAULT_REFRESH = 2;
    public static final int    DEFAULT_PORT_PRM = 5323;

    private EditText passInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_password);

        passInput = findViewById(R.id.passInput);
        Button passBtn = findViewById(R.id.passBtn);

        if (passBtn != null) {
            passBtn.setOnClickListener(v -> checkPassword());
        }
    }

    private void checkPassword() {
        if (passInput == null) return;
        String entered = passInput.getText().toString();
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = sp.getString(KEY_PASSWORD, DEFAULT_PASSWORD);

        if (saved.equals(entered)) {
            Intent i = new Intent(PasswordActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        } else {
            Toast.makeText(this, R.string.password_error,
                    Toast.LENGTH_SHORT).show();
            passInput.setText("");
        }
    }
}