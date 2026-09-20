package com.pmr.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/* Экран логов приложения V2.6.
 * Экран НЕ ГАСНЕТ, пока приложение открыто (FLAG_KEEP_SCREEN_ON).
 */
public class LogActivity extends AppCompatActivity {

    private TextView logText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_log);

        logText = findViewById(R.id.logText);

        Button btnClear = findViewById(R.id.btnLogClear);
        Button btnSave = findViewById(R.id.btnLogSave);
        Button btnShare = findViewById(R.id.btnLogShare);
        Button btnMail = findViewById(R.id.btnLogMail);

        if (btnClear != null) btnClear.setOnClickListener(v -> {
            AppLog.clear();
            refreshLog();
        });

        if (btnSave != null) btnSave.setOnClickListener(v -> saveToFile());

        if (btnShare != null) btnShare.setOnClickListener(v -> shareLog());

        if (btnMail != null) btnMail.setOnClickListener(v -> sendByMail());

        refreshLog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLog();
    }

    private void refreshLog() {
        if (logText == null) return;
        List<String> lines = AppLog.snapshot();
        if (lines.isEmpty()) {
            logText.setText(getString(R.string.log_empty));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : lines) sb.append(s).append('\n');
        logText.setText(sb.toString());
    }

    private void saveToFile() {
        try {
            String fname = "pmr_log_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date())
                    + ".txt";
            File dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, fname);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(AppLog.dump().getBytes("UTF-8"));
            fos.close();
            Toast.makeText(this,
                    getString(R.string.log_saved) + ": " + f.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
            AppLog.add("лог сохранён в " + f.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this,
                    getString(R.string.log_save_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void shareLog() {
        String body = AppLog.dump();
        if (body.isEmpty()) body = getString(R.string.log_empty);

        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, "PMR Admin log");
        i.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(i, "Поделиться логом"));
    }

    private void sendByMail() {
        String body = AppLog.dump();
        if (body.isEmpty()) body = getString(R.string.log_empty);

        Intent i = new Intent(Intent.ACTION_SENDTO);
        i.setData(Uri.parse("mailto:rw6hhl@mail.ru"));
        i.putExtra(Intent.EXTRA_SUBJECT, "PMR Admin log");
        i.putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Почтовый клиент не найден", Toast.LENGTH_LONG).show();
        }
    }
}