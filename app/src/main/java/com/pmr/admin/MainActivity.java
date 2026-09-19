package com.pmr.admin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private TextView   alogActiveNum;
    private TextView   alogActiveName;
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
        viewPager.setOffscreenPageLimit(5);

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

        /* === Вкладка 0 — список в канале (компактный) === */
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
                    row.setPadding(0, 1, 0, 1);

                    String ip = intToIp(it.ip);
                    String nm = PmrService.listFile.getName(it.Id);

                    TextView tv = new TextView(this);
                    tv.setText(String.format("%02d %05d %s %s",
                            it.i, it.Id, ip, nm));
                    tv.setTextSize(10);
                    tv.setMaxLines(1);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    tv.setLayoutParams(lp);
                    row.addView(tv);

                    Button bb = new Button(this);
                    bb.setText(R.string.btn_ban);
                    bb.setTextSize(9);
                    bb.setPadding(2, 0, 2, 0);
                    bb.setMinWidth(0);
                    bb.setMinimumWidth(0);
                    if (it.ban == 1) {
                        bb.setBackgroundTintList(
                                ContextCompat.getColorStateList(this, R.color.c_red));
                    } else {
                        bb.setBackgroundTintList(
                                ContextCompat.getColorStateList(this, R.color.c_green));
                    }
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
                tv.setTextSize(11);
                listContainer.addView(tv);
            }
        }

        /* === Вкладка 2 — активность === */
        if (alogText != null) {
            List<String> tail = PmrService.activeLog.tail(6);
            StringBuilder sb = new StringBuilder();
            for (String s : tail) sb.append(s).append('\n');
            if (sb.length() == 0) sb.append("(журнал пуст)\n");
            alogText.setText(sb.toString());
        }

        if (alogActiveNum != null && alogActiveName != null) {
            int act = PmrService.pmrSocket.getActiveClient();
            if (act >= 0) {
                alogActiveNum.setText(String.valueOf(act));
                int id = PmrService.chanList.idByClient(act);
                String nm = (id >= 0) ? PmrService.listFile.getName(id) : "";
                if (nm == null || nm.trim().isEmpty()) nm = "(без имени)";
                alogActiveName.setText(nm);
            } else {
                alogActiveNum.setText("—");
                alogActiveName.setText("нет активного");
            }
        }

        /* === Вкладка 3 — терминал === */
        if (termLogText != null) {
            List<String> tail = PmrService.webLog.tail(20);
            StringBuilder sb = new StringBuilder();
            for (String s : tail) sb.append(s).append('\n');
            if (sb.length() == 0) sb.append("(журнал пуст)\n");
            termLogText.setText(sb.toString());
        }
    }

    /* ==== Привязка View из PagerAdapter ==== */
    public void bindTabChan(LinearLayout container) { this.chanContainer = container; }
    public void bindTabList(LinearLayout container) { this.listContainer = container; }
    public void bindTabAlog(TextView alog, TextView num, TextView name) {
        this.alogText = alog;
        this.alogActiveNum = num;
        this.alogActiveName = name;
    }
    public void bindTabTerm(TextView log, EditText input) {
        this.termLogText = log;
        this.termInput = input;
    }

    /* ==== Отправка команды из терминала ==== */
    public void sendTermCommand() {
        if (termInput == null) return;
        String s = termInput.getText().toString();
        if (s.isEmpty()) return;
        if (PmrService.pmrSocket == null) {
            if (PmrService.webLog != null)
                PmrService.webLog.add("служба не запущена");
            return;
        }
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