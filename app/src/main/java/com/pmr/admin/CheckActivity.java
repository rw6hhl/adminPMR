package com.pmr.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/* Экран проверки системы V2.6.
 * Экран НЕ ГАСНЕТ, пока приложение открыто (FLAG_KEEP_SCREEN_ON).
 * Переход — только по кнопке "ПРОДОЛЖИТЬ".
 */
public class CheckActivity extends AppCompatActivity {

    private TextView tvPort, tvServer, tvChannel, tvSecret;
    private TextView tvPassword, tvAutostart;
    private volatile boolean checksDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_check);

        tvPort = findViewById(R.id.checkPortVal);
        tvServer = findViewById(R.id.checkServerVal);
        tvChannel = findViewById(R.id.checkChannelVal);
        tvSecret = findViewById(R.id.checkSecretVal);
        tvPassword = findViewById(R.id.checkPasswordVal);
        tvAutostart = findViewById(R.id.checkAutostartVal);

        Button btnContinue = findViewById(R.id.btnContinue);
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> goNext());
        }

        showKnownValues();
        new Thread(this::runChecks).start();
    }

    private void showKnownValues() {
        SharedPreferences sp = getSharedPreferences(
                PasswordActivity.PREFS, MODE_PRIVATE);

        int port = sp.getInt(PasswordActivity.KEY_PORT_PRM,
                PasswordActivity.DEFAULT_PORT_PRM);
        int myIdx = PmrSocket.MyMailIndex;
        int myCh = PmrSocket.MyPChannel;
        int kanal = (myCh == 0) ? 0 : ((myIdx & 0xF) * 8) + myCh;
        int secret = (myIdx & 0xFFFFFFF0) >> 4;

        boolean requirePass = sp.getBoolean(
                PasswordActivity.KEY_REQUIRE_PASSWORD, true);

        if (tvPort != null)
            tvPort.setText(String.valueOf(port) + "  ...");
        if (tvChannel != null)
            tvChannel.setText(String.valueOf(kanal));
        if (tvSecret != null)
            tvSecret.setText(String.valueOf(secret));
        if (tvPassword != null)
            tvPassword.setText(requirePass
                    ? R.string.check_on : R.string.check_off);
        if (tvAutostart != null)
            tvAutostart.setText(R.string.check_off);
    }

    private void runChecks() {
        SharedPreferences sp = getSharedPreferences(
                PasswordActivity.PREFS, MODE_PRIVATE);

        final int port = sp.getInt(PasswordActivity.KEY_PORT_PRM,
                PasswordActivity.DEFAULT_PORT_PRM);

        final boolean portFree = checkPortFree(port);
        final String portText = String.valueOf(port) + "  "
                + (portFree ? getString(R.string.check_free)
                            : getString(R.string.check_busy));
        final int portColor = portFree ? R.color.c_green : R.color.c_red;

        final boolean serverOk = checkServer();
        final String serverText = "185.221.154.39  "
                + (serverOk ? getString(R.string.check_available)
                            : getString(R.string.check_unavailable));
        final int serverColor = serverOk ? R.color.c_green : R.color.c_red;

        checksDone = true;

        Handler h = new Handler(Looper.getMainLooper());
        h.post(() -> {
            if (tvPort != null) {
                tvPort.setText(portText);
                tvPort.setTextColor(ContextCompat.getColor(
                        CheckActivity.this, portColor));
            }
            if (tvServer != null) {
                tvServer.setText(serverText);
                tvServer.setTextColor(ContextCompat.getColor(
                        CheckActivity.this, serverColor));
            }
        });
    }

    private boolean checkPortFree(int port) {
        DatagramSocket s = null;
        try {
            s = new DatagramSocket(port);
            s.setSoTimeout(100);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null) s.close();
        }
    }

    private boolean checkServer() {
        DatagramSocket s = null;
        try {
            s = new DatagramSocket();
            s.setSoTimeout(1000);
            InetAddress addr = InetAddress.getByName(PmrSocket.IP_SERVER);
            byte[] buf = new byte[4];
            buf[0] = 7;
            buf[1] = 0;
            int secret = ((PmrSocket.MyMailIndex & 0xFFFFFFF0) >> 4);
            buf[2] = (byte)(secret & 0xFF);
            buf[3] = (byte)((secret >> 8) & 0xFF);
            s.send(new DatagramPacket(buf, 4, addr, 16013));
            byte[] recv = new byte[64];
            DatagramPacket p = new DatagramPacket(recv, recv.length);
            s.receive(p);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null) s.close();
        }
    }

    private void goNext() {
        SharedPreferences sp = getSharedPreferences(
                PasswordActivity.PREFS, MODE_PRIVATE);
        boolean requirePass = sp.getBoolean(
                PasswordActivity.KEY_REQUIRE_PASSWORD, true);

        Intent i;
        if (requirePass) {
            i = new Intent(CheckActivity.this, PasswordActivity.class);
        } else {
            i = new Intent(CheckActivity.this, MainActivity.class);
        }
        startActivity(i);
        finish();
    }
}