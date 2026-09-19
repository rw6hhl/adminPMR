package com.pmr.admin;

import java.util.LinkedList;
import java.util.Queue;

/* Очередь команд для передачи в главный цикл.
 * Используется, если нужно складывать команды в очередь
 * (например, из UI, чтобы не блокировать основной поток).
 */
public class CmdQueue {

    private static final int MAX = 32;
    private final Queue<String> q = new LinkedList<>();

    public synchronized boolean push(String cmd) {
        if (cmd == null) return false;
        if (q.size() >= MAX) return false;
        q.offer(cmd);
        return true;
    }

    public synchronized String pop() {
        return q.poll();
    }

    public synchronized void clear() {
        q.clear();
    }

    public synchronized int size() {
        return q.size();
    }
}