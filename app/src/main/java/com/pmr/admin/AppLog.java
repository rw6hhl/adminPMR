package com.pmr.admin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/* Хранилище логов приложения.
 * Хранит до MAX строк. Все строки с временем.
 */
public class AppLog {

    public static final int MAX = 500;
    private static final List<String> lines = new ArrayList<>();

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    public static synchronized void add(String s) {
        if (s == null) return;
        String line = "[" + FMT.format(new Date()) + "] " + s;
        lines.add(line);
        if (lines.size() > MAX) lines.remove(0);
    }

    public static synchronized void clear() {
        lines.clear();
    }

    public static synchronized List<String> snapshot() {
        return new ArrayList<>(lines);
    }

    public static synchronized String dump() {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) sb.append(s).append('\n');
        return sb.toString();
    }

    public static synchronized int size() {
        return lines.size();
    }
}