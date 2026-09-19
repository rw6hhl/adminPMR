package com.pmr.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/* Экран ввода пароля.
 * Пароль: Rostov2026.
 * Спрашивается ОДИН РАЗ при запуске программы.
 * После верного ввода — переход к MainActivity.
 */
public class PasswordActivity extends AppCompatActivity {

    private static final String PASSWORD = "Rostov2026";

    private EditText passInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (PASSWORD.equals(entered)) {
            Intent i = new Intent(PasswordActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        } else {
            Toast.makeText(this, "Неверный пароль",
                    Toast.LENGTH_SHORT).show();
            passInput.setText("");
        }
    }
}