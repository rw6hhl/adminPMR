package com.pmr.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_NOTIF = 100;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Handler handler;

    /* UI вкладок — заполняются из PagerAdapter */
    private LinearLayout chanContainer;
    private LinearLayout listContainer;
    private TextView   alogText;
    private TextView   termLogText;
    private EditText   termInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        PagerAdapter adapter = new PagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText(R.string.tab_chan); break;
                        case 1: tab.setText(R.string.tab_list); break;
                        case 2: tab.setText(R.string.tab_alog); break;
                        case 3: tab.setText(R.string.tab_term); break;
                        case 4: tab.setText(R.string.tab_ctrl); break;
                        case 5: tab.setText(R.string.tab_hint); break;
                    }
                }).attach();

        handler = new Handler(Looper.getMainLooper());

        requestNotifPermission();
        startServiceSafe();

        /* Периодическое обновление UI */
        handler.post(uiLoop);
    }

    private final Runnable uiLoop = new Runnable() {
        @Override
        public void run() {
            refreshUI();
            handler.postDelayed(this, 2000);
        }
    };

    private void refreshUI() {
        if (PmrService.pmrSocket == null) return;

        /* === Вкладка 0 — список в канале === */
        if (chanContainer != null) {
            chanContainer.removeAllViews();
            List<ChanList.Item> lst = PmrService.chanList.snapshot();
            if (lst.isEmpty()) {
                TextView tv = new TextView(this);
                tv.setText("Список пуст.");
                tv.setTextSize(12);
                chanContainer.addView(tv);
            } else {
                for (ChanList.Item it : lst) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, 4, 0, 4);

                    String ip = intToIp(it.ip);
                    String nm = PmrService.listFile.getName(it.Id);

                    TextView tv = new TextView(this);
                    tv.setText(String.format("%02d  %d  %05d  %s  %s",
                            it.i, it.ban, it.Id, ip, nm));
                    tv.setTextSize(12);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    tv.setLayoutParams(lp);
                    row.addView(tv);

                    Button bb = new Button(this);
                    bb.setText("Бан (b" + it.i + ")");
                    bb.setTextSize(10);
                    final int cli = it.i;
                    bb.setOnClickListener(v -> {
                        if (PmrService.pmrSocket != null)
                            PmrService.pmrSocket.sendBan(cli);
                    });
                    row.addView(bb);

                    chanContainer.addView(row);
                }
            }
        }

        /* === Вкладка 1 — локальный list.txt === */
        if (listContainer != null) {
            listContainer.removeAllViews();
            List<ListFile.Item> lst = PmrService.listFile.snapshot();
            for (ListFile.Item it : lst) {
                TextView tv = new TextView(this);
                tv.setText(String.format("%05d  %s", it.id, it.name));
                tv.setTextSize(12);
                listContainer.addView(tv);
            }
        }

        /* === Вкладка 2 — лог активности === */
        if (alogText != null) {
            List<String> tail = PmrService.activeLog.tail(6);
            StringBuilder sb = new StringBuilder();
            for (String s : tail) sb.append(s).append('\n');
            if (sb.length() == 0) sb.append("(журнал пуст)\n");
            alogText.setText(sb.toString());
        }

        /* === Вкладка 3 — журнал терминала === */
        if (termLogText != null) {
            List<String> tail = PmrService.webLog.tail(20);
            StringBuilder sb = new StringBuilder();
            for (String s : tail) sb.append(s).append('\n');
            if (sb.length() == 0) sb.append("(журнал пуст)\n");
            termLogText.setText(sb.toString());
        }
    }

    /* ==== Вызывается из PagerAdapter при создании вкладки ==== */
    public void bindTabChan(LinearLayout container) {
        this.chanContainer = container;
    }
    public void bindTabList(LinearLayout container) {
        this.listContainer = container;
    }
    public void bindTabAlog(TextView text) {
        this.alogText = text;
    }
    public void bindTabTerm(TextView log, EditText input) {
        this.termLogText = log;
        this.termInput = input;
    }

    /* ==== Передаёт команду из терминала в PmrSocket ==== */
    public void sendTermCommand() {
        if (termInput == null) return;
        String s = termInput.getText().toString();
        if (s.isEmpty()) return;
        if (PmrService.pmrSocket != null)
            PmrService.pmrSocket.processCommand(s);
        termInput.setText("");
    }

    /* ==== Утилита: 32-бит IP -> строка ==== */
    public static String intToIp(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip & 0xFF),
                (ip >> 8) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 24) & 0xFF);
    }

    /* ==== Разрешение на уведомления (Android 13+) ==== */
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

    /* ==== Запуск Foreground Service ==== */
    private void startServiceSafe() {
        Intent svc = new Intent(this, PmrService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }
}