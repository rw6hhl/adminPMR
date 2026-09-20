package com.pmr.admin;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/* Главный экран — список абонентов в канале.
 * Заголовок: СПИСОК АБОНЕНТОВ.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQ_NOTIF = 100;

    private RecyclerView recycler;
    private ChanAdapter adapter;
    private Handler handler;
    private int refreshMs = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView title = findViewById(R.id.listTitle);
        if (title != null) {
            title.setText(R.string.list_title);
        }

        recycler = findViewById(R.id.recyclerChan);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChanAdapter(this);
        recycler.setAdapter(adapter);

        Button settingsBtn = findViewById(R.id.btnSettings);
        if (settingsBtn != null) {
            settingsBtn.setOnClickListener(v -> {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            });
        }

        handler = new Handler(Looper.getMainLooper());

        requestNotifPermission();
        startServiceSafe();

        handler.post(uiLoop);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sp = getSharedPreferences(
                PasswordActivity.PREFS, MODE_PRIVATE);
        refreshMs = sp.getInt(PasswordActivity.KEY_REFRESH,
                PasswordActivity.DEFAULT_REFRESH) * 1000;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            stopService(new Intent(this, PmrService.class));
        }
    }

    private final Runnable uiLoop = new Runnable() {
        @Override
        public void run() {
            refreshUI();
            handler.postDelayed(this, refreshMs);
        }
    };

    private void refreshUI() {
        if (PmrService.pmrSocket == null) return;

        java.util.List<ChanList.Item> lst = PmrService.chanList.snapshot();
        int activeClient = PmrService.pmrSocket.getActiveClient();
        adapter.setData(lst, activeClient, PmrService.listFile);
    }

    private void requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF);
            }
        }
    }

    private void startServiceSafe() {
        Intent svc = new Intent(this, PmrService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }
}