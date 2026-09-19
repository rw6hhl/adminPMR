package com.pmr.admin;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* Вся UDP-логика PMR. Полностью повторяет admin.c (V3.5),
 * адаптирована под Android.
 */
public class PmrSocket {

    /* ==== Константы из admin.c ==== */
    public static final int WAV_8000_16    = 25;
    public static final int WAV_16000_16   = 21;
    public static final int WAV_8000_G711  = 26;
    public static final int WAV_16000_G711 = 22;

    public static final String IP_SERVER = "185.221.154.39";
    public static final int PORT_PRM = 5322;    /* локальный порт приёма (5321 занят Android) */
    public static final int PORT_PRD = 16000;   /* базовый порт сервера */

    /* ==== Настройки PMR ==== */
    public static int MyMailIndex = 51953;
    public static int MyPChannel  = 5;
    public static int Priznak_pmr = 11777;

    /* ==== Глобальные ссылки ==== */
    private final ListFile listFile;
    private final ChanList chanList;
    private final ActiveLog activeLog;
    private final WebLog webLog;
    private final CmdQueue cmdQueue;
    private final File filesDir;

    /* ==== Сокет ==== */
    private DatagramSocket sock;
    private InetAddress serverAddr;
    private int kanal_PRD = 0;
    private int kanal_Secret = 0;

    /* ==== Активный абонент ==== */
    private volatile int active_client_num = -1;
    private volatile int active_tic = 0;
    private int KtoActiv = -1;
    private int KtoTic = 0;

    /* ==== Цикл ==== */
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

    public int getActiveClient() {
        return active_client_num;
    }

    public boolean isRunning() {
        return running;
    }

    /* ==== Запуск ==== */
    public void start() {
        if (running) return;

        try {
            sock = new DatagramSocket(PORT_PRM);
            sock.setSoTimeout(100);
            serverAddr = InetAddress.getByName(IP_SERVER);
        } catch (Exception e) {
            webLog.add("не удалось открыть сокет: " + e.getMessage());
            return;
        }

        /* Преобразование настроек PMR -> канал сервера */
        if (MyPChannel == 0) {
            kanal_PRD = 0;
        } else {
            kanal_PRD = ((MyMailIndex & 0xF) * 8) + MyPChannel;
        }
        kanal_Secret = (MyMailIndex & 0xFFFFFFF0) >> 4;

        webLog.add("PMR: порт " + PORT_PRM
                + ", канал " + kanal_PRD
                + ", секрет " + kanal_Secret);

        running = true;

        threadUdp = new Thread(this::udpLoop, "pmr-udp");
        threadUdp.start();

        threadTimer = new Thread(this::timerLoop, "pmr-timer");
        threadTimer.start();
    }

    /* ==== Остановка ==== */
    public void stop() {
        running = false;
        try {
            if (sock != null) sock.close();
        } catch (Exception ignored) {}
    }

    /* ============ Таймер (10 Гц) ============ */
    private void timerLoop() {
        int cikl_PRD = 0;
        int cikl = 0;
        int prevActive = -1;
        long prevStart = 0;

        while (running) {
            /* KtoTic — сброс активного */
            if (KtoTic > 0) {
                KtoTic++;
                if (KtoTic > 10) {
                    KtoActiv = -1;
                    KtoTic = 0;
                }
            }

            /* Проверка смены активного */
            int cur = active_client_num;
            if (cur != prevActive) {
                long now = System.currentTimeMillis() / 1000L;
                if (prevActive >= 0) {
                    int dur = (int)(now - prevStart);
                    activeLog.add(fmtTime(now) + " client " + prevActive
                            + " — выключился (" + dur + " сек)");
                }
                if (cur >= 0) {
                    int id = chanList.idByClient(cur);
                    String nm = (id >= 0) ? listFile.getName(id) : "";
                    if (nm != null && !nm.trim().isEmpty()) {
                        activeLog.add(fmtTime(now) + " client " + cur
                                + " — включился " + nm);
                    } else {
                        activeLog.add(fmtTime(now) + " client " + cur
                                + " — включился");
                    }
                    prevStart = now;
                }
                prevActive = cur;
                /* Автообновление списка канала */
                sendL();
            }

            /* Сброс активного, если тишина > 1.5 сек */
            if (active_client_num >= 0) {
                active_tic++;
                if (active_tic > 15) {
                    active_client_num = -1;
                    active_tic = 0;
                }
            }

            /* Раз в секунду — тест */
            cikl_PRD++;
            if (cikl_PRD > 9) {
                if (kanal_Secret != 0) {
                    sendCmdHeader(7, 0, kanal_Secret);
                } else {
                    sendCmdHeader(0, 0, 0);
                }
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

    /* ============ UDP приём ============ */
    private void udpLoop() {
        int KolInKanal = 0;
        byte[] buf = new byte[1640];

        while (running) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                sock.receive(p);
                int n = p.getLength();
                if (n < 4) continue;

                int command = buf[0] & 0xFF;
                int kanal   = buf[1] & 0xFF;
                int client  = ((buf[3] & 0xFF) << 8) | (buf[2] & 0xFF);

                if (kanal != kanal_PRD && kanal_PRD != 0) continue;

                if (n == 4) {
                    /* служебные команды */
                    switch (command) {
                        case 0:
                            if (kanal_Secret != 0) {
                                sendCmdHeader(7, 0, kanal_Secret);
                            }
                            break;
                        case 7:
                            break;
                        case 'n':
                            if (client != KolInKanal) {
                                KolInKanal = client;
                                sendL();
                            }
                            break;
                    }
                } else {
                    switch (command) {
                        case 19:
                            if (n == 324) onWave(client);
                            break;
                        case 21:
                            if (n == 644) onWave(client);
                            break;
                        case 22:
                            if (n == 324) onWave(client);
                            break;
                        case 25:
                            if (n == 324) onWave(client);
                            break;
                        case 26:
                            if (n == 164) onWave(client);
                            break;
                        case 234:
                            handleChanList(buf, n);
                            break;
                        case 123:
                            handleListFile(buf, n, client);
                            break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /* Обработка звука — кто-то говорит */
    private void onWave(int n) {
        if (KtoActiv == -1) {
            KtoActiv = n;
            KtoTic = 1;
        } else {
            if (KtoActiv != n) return;
            KtoTic = 1;
        }
        active_client_num = n;
        active_tic = 0;
    }

    /* Разбор команды 234 — список канала */
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
        webLog.add("--- список в канале "
                + fmtTime(System.currentTimeMillis() / 1000L) + " ---");
    }

    /* Разбор команды 123 — новый list.txt */
    private void handleListFile(byte[] buf, int n, int client) {
        try {
            String text = new String(buf, 4, n - 4, "UTF-8");
            File f = new File(filesDir, "list.txt");
            FileOutputStream fos;
            if (client == 0) {
                fos = new FileOutputStream(f, false);
            } else {
                fos = new FileOutputStream(f, true);
            }
            fos.write(text.getBytes("UTF-8"));
            fos.close();
            listFile.load(f);
            webLog.add("list.txt обновлён, Wsego = " + listFile.count());
        } catch (Exception e) {
            webLog.add("ошибка list.txt: " + e.getMessage());
        }
    }

    /* ============ Отправка команд ============ */
    private void sendRaw(byte[] buf) {
        if (sock == null || serverAddr == null) return;
        try {
            DatagramPacket p = new DatagramPacket(buf, buf.length, serverAddr,
                    PORT_PRD + kanal_PRD);
            sock.send(p);
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
        webLog.add("l отправлен");
    }

    public void sendBan(int client) {
        sendCmdHeader(222, 13, client);
        sendCmdHeader(234, 13, 0);
        webLog.add("b" + client + " отправлен (переключить бан)");
    }

    public void send260(int v) {
        sendCmdHeader(221, 13, v);
        webLog.add("Доступ к Ростовскому репитеру = " + v);
    }

    public void sendRename(String text) {
        try {
            byte[] txt = text.getBytes("UTF-8");
            byte[] buf = new byte[4 + txt.length + 1];
            buf[0] = (byte)133;
            buf[1] = 13;
            buf[2] = 0;
            buf[3] = 0;
            System.arraycopy(txt, 0, buf, 4, txt.length);
            buf[4 + txt.length] = 0;
            DatagramPacket p = new DatagramPacket(buf, buf.length,
                    serverAddr, 15999);
            sock.send(p);

            byte[] h = new byte[4];
            h[0] = (byte)123;
            h[1] = 13;
            h[2] = 0;
            h[3] = 0;
            DatagramPacket p2 = new DatagramPacket(h, 4, serverAddr, 15999);
            sock.send(p2);
            webLog.add("rename отправлен");
        } catch (Exception e) {
            webLog.add("ошибка rename: " + e.getMessage());
        }
    }

    public void sendDelete(int id) {
        try {
            byte[] h = new byte[4];
            h[0] = (byte)143;
            h[1] = 13;
            h[2] = (byte)(id & 0xFF);
            h[3] = (byte)((id >> 8) & 0xFF);
            DatagramPacket p = new DatagramPacket(h, 4, serverAddr, 15999);
            sock.send(p);

            byte[] h2 = new byte[4];
            h2[0] = (byte)123;
            h2[1] = 13;
            h2[2] = 0;
            h2[3] = 0;
            DatagramPacket p2 = new DatagramPacket(h2, 4, serverAddr, 15999);
            sock.send(p2);
            webLog.add("delete отправлен");
        } catch (Exception e) {
            webLog.add("ошибка delete: " + e.getMessage());
        }
    }

    public void sendList() {
        try {
            byte[] h = new byte[4];
            h[0] = (byte)123;
            h[1] = 13;
            h[2] = 0;
            h[3] = 0;
            DatagramPacket p = new DatagramPacket(h, 4, serverAddr, 15999);
            sock.send(p);
            webLog.add("list запрошен");
        } catch (Exception e) {
            webLog.add("ошибка list: " + e.getMessage());
        }
    }

    /* ============ Обработка команд из терминала ============ */
    public void processCommand(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) return;
        webLog.add("> " + s);

        if (s.equals("exit") || s.equals("EXIT")) {
            stop();
            return;
        }
        if (s.startsWith("delete") || s.startsWith("DELETE")) {
            try {
                int id = Integer.parseInt(s.substring(7).trim());
                sendDelete(id);
            } catch (Exception e) {
                webLog.add("ошибка delete");
            }
            return;
        }
        if (s.startsWith("rename") || s.startsWith("RENAME")) {
            sendRename(s.substring(6).trim());
            return;
        }
        if (s.startsWith("list") || s.startsWith("LIST")) {
            sendList();
            return;
        }
        if (s.equals("l")) {
            sendL();
            return;
        }
        if (s.length() == 3 && s.charAt(0) == 'b') {
            try {
                int k = Integer.parseInt(s.substring(1));
                sendBan(k);
            } catch (Exception ignored) {}
            return;
        }
        if (s.length() == 3 && s.startsWith("26")) {
            int v = (s.charAt(2) == '0') ? 0 : 1;
            send260(v);
            return;
        }
        webLog.add("неизвестная команда");
    }

    /* ==== Утилита: время ЧЧ:ММ:СС ==== */
    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private static String fmtTime(long unixSec) {
        return "[" + TIME_FMT.format(new Date(unixSec * 1000L)) + "]";
    }
}