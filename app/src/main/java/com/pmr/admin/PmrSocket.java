package com.pmr.admin;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/* UDP-логика PMR V3.0.
 * Все публичные команды (sendBan, send260, sendL, sendRename, sendDelete, sendList)
 * выполняются в ОТДЕЛЬНОМ ПОТОКЕ — чтобы не было NetworkOnMainThreadException.
 *
 * Изменения V3.0:
 *   - логирование команд через AppLog.addCmd (→ / ←);
 *   - защита от бана администратора (Priznak_pmr);
 *   - вывод активного абонента раз в секунду в лог;
 *   - чтение локального кэша банов (bans_local) при chanList.
 */
public class PmrSocket {

    public static final String IP_SERVER = "185.221.154.39";
    public static final int PORT_PRD = 16000;

    public static int MyMailIndex = 51953;
    public static int MyPChannel  = 5;
    public static int Priznak_pmr = 11777;

    private final ListFile listFile;
    private final ChanList chanList;
    private final ActiveLog activeLog;
    private final WebLog webLog;
    private final CmdQueue cmdQueue;
    private final File filesDir;
    private final Context appCtx;

    private volatile DatagramSocket sock;
    private volatile InetAddress serverAddr;
    private int port_prm = 5323;
    private int kanal_PRD = 0;
    private int kanal_Secret = 0;

    private volatile int active_client_num = -1;
    private volatile int active_tic = 0;
    private int KtoActiv = -1;
    private int KtoTic = 0;

    private volatile boolean running = false;
    private Thread threadUdp;
    private Thread threadTimer;

    /* Тик для вывода активного абонента в лог (раз в секунду). */
    private int activeLogTick = 0;

    public PmrSocket(Context ctx,
                     ListFile lf, ChanList cl, ActiveLog al, WebLog wl,
                     CmdQueue cq, File filesDir) {
        this.appCtx = ctx;
        this.listFile = lf;
        this.chanList = cl;
        this.activeLog = al;
        this.webLog = wl;
        this.cmdQueue = cq;
        this.filesDir = filesDir;
    }

    public int getActiveClient() { return active_client_num; }

    public boolean isRunning() {
        return running && sock != null && serverAddr != null;
    }

    public int getPortPrm() { return port_prm; }
    public int getKanalPRD() { return kanal_PRD; }
    public int getKanalSecret() { return kanal_Secret; }

    public void start() {
        if (running) return;

        SharedPreferences sp = appCtx.getSharedPreferences(
                PasswordActivity.PREFS, Context.MODE_PRIVATE);
        port_prm = sp.getInt(PasswordActivity.KEY_PORT_PRM,
                PasswordActivity.DEFAULT_PORT_PRM);

        AppLog.add("PmrSocket.start() — начало");

        try {
            sock = new DatagramSocket(port_prm);
            sock.setSoTimeout(100);
            serverAddr = InetAddress.getByName(IP_SERVER);
        } catch (Exception e) {
            AppLog.add("PmrSocket: ошибка сокета — " + e);
            return;
        }

        if (MyPChannel == 0) kanal_PRD = 0;
        else kanal_PRD = ((MyMailIndex & 0xF) * 8) + MyPChannel;
        kanal_Secret = (MyMailIndex & 0xFFFFFFF0) >> 4;

        AppLog.add("PmrSocket: port=" + port_prm
                + ", kanal=" + kanal_PRD
                + ", secret=" + kanal_Secret
                + ", server=" + IP_SERVER);

        running = true;
        threadUdp = new Thread(this::udpLoop, "pmr-udp");
        threadUdp.start();
        threadTimer = new Thread(this::timerLoop, "pmr-timer");
        threadTimer.start();
    }

    public void stop() {
        AppLog.add("PmrSocket.stop()");
        running = false;
        try { if (sock != null) sock.close(); } catch (Exception ignored) {}
    }

    private void timerLoop() {
        int cikl_PRD = 0;
        int cikl = 0;
        int diag = 0;

        while (running) {
            if (KtoTic > 0) {
                KtoTic++;
                if (KtoTic > 10) { KtoActiv = -1; KtoTic = 0; }
            }

            if (active_client_num >= 0) {
                active_tic++;
                if (active_tic > 15) { active_client_num = -1; active_tic = 0; }
            }

            /* Вывод активного абонента в лог раз в секунду. */
            activeLogTick++;
            if (activeLogTick >= 10) {
                if (active_client_num >= 0) {
                    AppLog.addCmd("←", "active: client=" + active_client_num);
                }
                activeLogTick = 0;
            }

            cikl_PRD++;
            if (cikl_PRD > 9) {
                if (kanal_Secret != 0) sendCmdHeader(7, 0, kanal_Secret);
                else                    sendCmdHeader(0, 0, 0);
                cikl_PRD = 0;
                cikl++;
                if (cikl > 3) {
                    byte[] buf = new byte[6];
                    buf[0] = 0;
                    buf[1] = 0;
                    buf[2] = (byte)(Priznak_pmr & 0xFF);
                    buf[3] = (byte)((Priznak_pmr >> 8) & 0xFF);
                    sendRaw(buf);
                    cikl = 0;
                }
            }

            diag++;
            if (diag > 50) {
                AppLog.add("диаг: sock=" + (sock != null)
                        + ", server=" + (serverAddr != null)
                        + ", running=" + running
                        + ", kanal_PRD=" + kanal_PRD);
                diag = 0;
            }

            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
    }

    private void udpLoop() {
        int KolInKanal = 0;
        byte[] buf = new byte[1640];

        while (running) {
            try {
                DatagramSocket s = sock;
                if (s == null) break;
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                s.receive(p);
                int n = p.getLength();
                if (n < 4) continue;

                int command = buf[0] & 0xFF;
                int kanal   = buf[1] & 0xFF;
                int client  = ((buf[3] & 0xFF) << 8) | (buf[2] & 0xFF);

                if (kanal != kanal_PRD && kanal_PRD != 0) continue;

                if (n == 4) {
                    switch (command) {
                        case 0:
                            AppLog.addCmd("←", "cmd=0 kanal=" + kanal);
                            if (kanal_Secret != 0) sendCmdHeader(7, 0, kanal_Secret);
                            break;
                        case 7:
                            AppLog.addCmd("←", "cmd=7 kanal=" + kanal);
                            break;
                        case 'n':
                            AppLog.addCmd("←", "cmd=n client=" + client);
                            if (client != KolInKanal) { KolInKanal = client; sendL(); }
                            break;
                    }
                } else {
                    switch (command) {
                        case 19: if (n == 324) { AppLog.addCmd("←", "cmd=19 wave client=" + client); onWave(client); } break;
                        case 21: if (n == 644) { AppLog.addCmd("←", "cmd=21 wave client=" + client); onWave(client); } break;
                        case 22: if (n == 324) { AppLog.addCmd("←", "cmd=22 wave client=" + client); onWave(client); } break;
                        case 25: if (n == 324) { AppLog.addCmd("←", "cmd=25 wave client=" + client); onWave(client); } break;
                        case 26: if (n == 164) { AppLog.addCmd("←", "cmd=26 wave client=" + client); onWave(client); } break;
                        case 234:
                            AppLog.addCmd("←", "cmd=234 chanList cnt="
                                    + ((n - 4) / 13) + " size=" + n);
                            handleChanList(buf, n);
                            break;
                        case 123:
                            AppLog.addCmd("←", "cmd=123 list.txt size=" + n);
                            handleListFile(buf, n, client);
                            break;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void onWave(int n) {
        if (KtoActiv == -1) { KtoActiv = n; KtoTic = 1; }
        else { if (KtoActiv != n) return; KtoTic = 1; }
        active_client_num = n;
        active_tic = 0;
    }

    private void handleChanList(byte[] buf, int n) {
        chanList.clear();

        /* Читаем локальный кэш банов. */
        SharedPreferences sp = appCtx.getSharedPreferences(
                PasswordActivity.PREFS, Context.MODE_PRIVATE);
        Set<String> bansLocal = sp.getStringSet(
                ChanAdapter.KEY_BANS_LOCAL, new HashSet<String>());

        int cnt = (n - 4) / 13;
        if (cnt > 102) cnt = 102;
        for (int k = 0; k < cnt; k++) {
            int off = 4 + k * 13;
            ChanList.Item it = new ChanList.Item();
            it.i    = ((buf[off + 1] & 0xFF) << 8) | (buf[off] & 0xFF);
            it.cod  = ((buf[off + 3] & 0xFF) << 8) | (buf[off + 2] & 0xFF);
            it.Id   = ((buf[off + 5] & 0xFF) << 8) | (buf[off + 4] & 0xFF);
            it.port = ((buf[off + 7] & 0xFF) << 8) | (buf[off + 6] & 0xFF);
            it.ip   = ((buf[off + 11] & 0xFF) << 24)
                    | ((buf[off + 10] & 0xFF) << 16)
                    | ((buf[off + 9] & 0xFF) << 8)
                    | (buf[off + 8] & 0xFF);
            it.ban  = buf[off + 12] & 0xFF;
            it.banLocal = bansLocal.contains(String.valueOf(it.Id)) ? 1 : 0;
            chanList.add(it);
        }
    }

    private void handleListFile(byte[] buf, int n, int client) {
        try {
            String text = new String(buf, 4, n - 4, "UTF-8");
            File f = new File(filesDir, "list.txt");
            FileOutputStream fos = new FileOutputStream(f, client != 0);
            fos.write(text.getBytes("UTF-8"));
            fos.close();
            listFile.load(f);
        } catch (Exception e) {
            AppLog.add("ошибка list.txt: " + e);
        }
    }

    /* Низкоуровневая отправка — вызывается ТОЛЬКО из фоновых потоков */
    private void sendRaw(byte[] buf) {
        DatagramSocket s = sock;
        InetAddress a = serverAddr;
        if (s == null || a == null) {
            AppLog.add("sendRaw: sock или server = null");
            return;
        }
        try {
            DatagramPacket p = new DatagramPacket(buf, buf.length, a,
                    PORT_PRD + kanal_PRD);
            s.send(p);
        } catch (Exception e) {
            AppLog.add("sendRaw FAIL: " + e);
        }
    }

    private void sendCmdHeader(int cmd, int kanal, int client) {
        byte[] buf = new byte[4];
        buf[0] = (byte)(cmd & 0xFF);
        buf[1] = (byte)(kanal & 0xFF);
        buf[2] = (byte)(client & 0xFF);
        buf[3] = (byte)((client >> 8) & 0xFF);
        sendRaw(buf);
    }

    /* ============ ПУБЛИЧНЫЕ КОМАНДЫ — В ОТДЕЛЬНОМ ПОТОКЕ ============ */

    public void sendL() {
        new Thread(() -> {
            AppLog.addCmd("→", "cmd=234 list kanal=13 port="
                    + (PORT_PRD + kanal_PRD));
            sendCmdHeader(234, 13, 0);
        }).start();
    }

    public void sendBan(final int client) {
        new Thread(() -> {
            /* Защита от бана администратора. */
            if (client == Priznak_pmr) {
                AppLog.add("бан админа запрещён (client=" + client + ")");
                return;
            }
            AppLog.addCmd("→", "cmd=222 ban kanal=13 client=" + client
                    + " port=" + (PORT_PRD + kanal_PRD));
            sendCmdHeader(222, 13, client);
            sendCmdHeader(234, 13, 0);
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            sendCmdHeader(234, 13, 0);
        }).start();
    }

    public void send260(final int v) {
        new Thread(() -> {
            AppLog.addCmd("→", "cmd=221 26x kanal=13 value=" + v
                    + " port=" + (PORT_PRD + kanal_PRD));
            sendCmdHeader(221, 13, v);
        }).start();
    }

    public void sendRename(final String textIn) {
        new Thread(() -> {
            String text = (textIn == null) ? "" : textIn;
            try {
                byte[] txt = text.getBytes("UTF-8");
                byte[] buf = new byte[4 + txt.length + 1];
                buf[0] = (byte)133;
                buf[1] = 13;
                buf[2] = 0;
                buf[3] = 0;
                System.arraycopy(txt, 0, buf, 4, txt.length);
                buf[4 + txt.length] = 0;
                DatagramSocket s = sock;
                InetAddress a = serverAddr;
                if (s == null || a == null) {
                    AppLog.add("sendRename: sock или server = null");
                    return;
                }
                s.send(new DatagramPacket(buf, buf.length, a, 15999));
                AppLog.addCmd("→", "cmd=133 rename port=15999");
            } catch (Exception e) {
                AppLog.add("sendRename FAIL: " + e);
            }
        }).start();
    }

    public void sendDelete(final int id) {
        new Thread(() -> {
            try {
                DatagramSocket s = sock;
                InetAddress a = serverAddr;
                if (s == null || a == null) return;
                byte[] h = new byte[4];
                h[0] = (byte)143;
                h[1] = 13;
                h[2] = (byte)(id & 0xFF);
                h[3] = (byte)((id >> 8) & 0xFF);
                s.send(new DatagramPacket(h, 4, a, 15999));
                AppLog.addCmd("→", "cmd=143 delete port=15999");
            } catch (Exception ignored) {}
        }).start();
    }

    public void sendList() {
        new Thread(() -> {
            try {
                DatagramSocket s = sock;
                InetAddress a = serverAddr;
                if (s == null || a == null) return;
                byte[] h = new byte[4];
                h[0] = (byte)123;
                h[1] = 13;
                h[2] = 0;
                h[3] = 0;
                s.send(new DatagramPacket(h, 4, a, 15999));
                AppLog.addCmd("→", "cmd=123 list.txt port=15999");
            } catch (Exception ignored) {}
        }).start();
    }

    public void processCommand(String raw) {
        if (raw == null) return;
    }
}