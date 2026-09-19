package com.pmr.admin;

import java.util.ArrayList;
import java.util.List;

/* Журнал команд (для вкладки "Терминал").
 * Хранит не более MAX последних строк.
 */
public class WebLog {

    public static final int MAX = 200;
    private final List<String> lines = new ArrayList<>();

    public synchronized void add(String s) {
        if (s == null) return;
        lines.add(s);
        if (lines.size() > MAX) {
            lines.remove(0);
        }
    }

    public synchronized void clear() {
        lines.clear();
    }

    public synchronized List<String> snapshot() {
        return new ArrayList<>(lines);
    }

    /* Возвращает последние n строк */
    public synchronized List<String> tail(int n) {
        int sz = lines.size();
        int start = Math.max(0, sz - n);
        return new ArrayList<>(lines.subList(start, sz));
    }

    /* Полный текст в одну строку — для сохранения в файл */
    public synchronized String dump() {
        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            sb.append(s).append('\n');
        }
        return sb.toString();
    }

    public synchronized int size() {
        return lines.size();
    }
}