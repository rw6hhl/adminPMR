package com.pmr.admin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/* Хранилище логов приложения V3.0.
 * Хранит до MAX строк. Все строки с временем.
 * Добавлен метод addCmd для логирования команд обмена с сервером.
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

    /* Новый метод: логирование команд обмена с сервером.
     * direction: "→" (исходящее) или "←" (входящее).
     * text: тело команды, например "cmd=222 kanal=13 client=7".
     */
    public static synchronized void addCmd(String direction, String text) {
        if (direction == null) direction = "";
        if (text == null) text = "";
        add(direction + " " + text);
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