package com.pmr.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/* Экран приветствия.
 * Показывается 5 секунд, затем передаёт управление:
 * - PasswordActivity, если пароль требуется;
 * - MainActivity, если пароль отключён.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 5000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences sp = getSharedPreferences(
                    PasswordActivity.PREFS, MODE_PRIVATE);
            boolean requirePass = sp.getBoolean(
                    PasswordActivity.KEY_REQUIRE_PASSWORD, true);

            Intent i;
            if (requirePass) {
                i = new Intent(SplashActivity.this, PasswordActivity.class);
            } else {
                i = new Intent(SplashActivity.this, MainActivity.class);
            }
            startActivity(i);
            finish();
        }, SPLASH_DELAY_MS);
    }
}