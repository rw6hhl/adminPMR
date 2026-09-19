package com.pmr.admin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/* Экран приветствия.
 * Показывается 5 секунд, затем передаёт управление PasswordActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 5000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent i = new Intent(SplashActivity.this, PasswordActivity.class);
            startActivity(i);
            finish();
        }, SPLASH_DELAY_MS);
    }
}