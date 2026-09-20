package com.pmr.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* UDP-логика PMR. Все обращения к sock/serverAddr защищены проверками null. */
public class PmrSocket {

    public static final String IP_SERVER = "185.221.154.39";
    public static final int PORT_PRM = 5322;
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

    private volatile DatagramSocket sock;
    private volatile InetAddress serverAddr;
    private int kanal_PRD = 0;
    private int kanal_Secret = 0;

    private volatile int active_client_num = -1;
    private volatile int active_tic = 0;
    private int KtoActiv = -1;
    private int KtoTic = 0;

    private volatile boolean running = false;
    private Thread threadUdp;
    private Thread threadTimer;

    public PmrSocket(ListFile lf, ChanList cl, ActiveLog al, WebLog wl,
                     CmdQueue cq, File filesDir) {
        this.listFile = lf;
        this.chanList = cl;
        this.activeLog = al;
        this.webLog = wl;
        this.cmdQueue = cq;
        this.filesDir = filesDir;
    }

    public int getActiveClient() { return active_client_num; }
    public boolean isRunning() { return running; }

    public void start() {
        if (running) return;
        try {
            sock = new DatagramSocket(PORT_PRM);
            sock.setSoTimeout(100);
            serverAddr = InetAddress.getByName(IP_SERVER);
        } catch (Exception e) {
            if (webLog != null) webLog.add("сокет: " + e.getMessage());
            return;
        }

        if (MyPChannel == 0) kanal_PRD = 0;
        else kanal_PRD = ((MyMailIndex & 0xF) * 8) + MyPChannel;
        kanal_Secret = (MyMailIndex & 0xFFFFFFF0) >> 4;

        if (webLog != null)
            webLog.add("PMR: порт " + PORT_PRM + ", канал " + kanal_PRD
                    + ", секрет " + kanal_Secret);

        running = true;
        threadUdp = new Thread(this::udpLoop, "pmr-udp");
        threadUdp.start();
        threadTimer = new Thread(this::timerLoop, "pmr-timer");
        threadTimer.start();
    }

    public void stop() {
        running = false;
        try { if (sock != null) sock.close(); } catch (Exception ignored) {}
    }

    private void timerLoop() {
        int cikl_PRD = 0;
        int cikl = 0;
        int prevActive = -1;
        long prevStart = 0;

        while (running) {
            if (KtoTic > 0) {
                KtoTic++;
                if (KtoTic > 10) { KtoActiv = -1; KtoTic = 0; }
            }

            int cur = active_client_num;
            if (cur != prevActive) {
                long now = System.currentTimeMillis() / 1000L;
                if (prevActive >= 0) {
                    int dur = (int)(now - prevStart);
                    activeLog.add(fmtTime(now) + " " + prevActive + " выкл (" + dur + "с)");
                }
                if (cur >= 0) {
                    int id = chanList.idByClient(cur);
                    String nm = (id >= 0) ? listFile.getName(id) : "";
                    if (nm == null) nm = "";
                    nm = nm.trim();
                    if (nm.length() > 24) nm = nm.substring(0, 24);
                    if (nm.isEmpty()) {
                        activeLog.add(fmtTime(now) + " " + cur + " вкл");
                    } else {
                        activeLog.add(fmtTime(now) + " " + cur + " вкл " + nm);
                    }
                    prevStart = now;
                }
                prevActive = cur;
                sendL();
            }

            if (active_client_num >= 0) {
                active_tic++;
                if (active_tic > 15) { active_client_num = -1; active_tic = 0; }
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
                            if (kanal_Secret != 0) sendCmdHeader(7, 0, kanal_Secret);
                            break;
                        case 7:
                            break;
                        case 'n':
                            if (client != KolInKanal) { KolInKanal = client; sendL(); }
                            break;
                    }
                } else {
                    switch (command) {
                        case 19: if (n == 324) onWave(client); break;
                        case 21: if (n == 644) onWave(client); break;
                        case 22: if (n == 324) onWave(client); break;
                        case 25: if (n == 324) onWave(client); break;
                        case 26: if (n == 164) onWave(client); break;
                        case 234: handleChanList(buf, n); break;
                        case 123: handleListFile(buf, n, client); break;
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
        } catch (Exception ignored) {}
    }

    private void sendRaw(byte[] buf) {
        DatagramSocket s = sock;
        InetAddress a = serverAddr;
        if (s == null || a == null) return;
        try {
            DatagramPacket p = new DatagramPacket(buf, buf.length, a,
                    PORT_PRD + kanal_PRD);
            s.send(p);
        } catch (Exception ignored) {}
    }

    private void sendCmdHeader(int cmd, int kanal, int client) {
        byte[] buf = new byte[4];
        buf[0] = (byte)(cmd & 0xFF);
        buf[1] = (byte)(kanal & 0xFF);
        buf[2] = (byte)(client & 0xFF);
        buf[3] = (byte)((client >> 8) & 0xFF);
        sendRaw(buf);
    }

    public void sendL() {
        sendCmdHeader(234, 13, 0);
    }

    public void sendBan(int client) {
        sendCmdHeader(222, 13, client);
        sendCmdHeader(234, 13, 0);
    }

    public void send260(int v) {
        sendCmdHeader(221, 13, v);
    }

    public void sendRename(String text) {
        if (text == null) text = "";
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
            if (s == null || a == null) return;
            s.send(new DatagramPacket(buf, buf.length, a, 15999));
        } catch (Exception ignored) {}
    }

    public void sendDelete(int id) {
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
        } catch (Exception ignored) {}
    }

    public void sendList() {
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
        } catch (Exception ignored) {}
    }

    /* Оставлено для совместимости, но в V2.0 не используется */
    public void processCommand(String raw) {
        if (raw == null) return;
    }

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private static String fmtTime(long unixSec) {
        return "[" + TIME_FMT.format(new Date(unixSec * 1000L)) + "]";
    }
}