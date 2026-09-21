package com.pmr.admin;

import java.util.ArrayList;
import java.util.List;

/* Список абонентов в канале V3.0.
 * Добавлено поле banLocal — локальный кэш бана (SharedPreferences).
 * ban — серверное значение (может быть всегда 0).
 * banLocal — локальное значение (виден бан на этом устройстве).
 */
public class ChanList {

    public static class Item {
        public int i;
        public int cod;
        public int Id;
        public int port;
        public int ip;
        public int ban;
        public int banLocal;
    }

    private final List<Item> items = new ArrayList<>();

    public synchronized void clear() { items.clear(); }
    public synchronized void add(Item it) { if (it != null) items.add(it); }
    public synchronized List<Item> snapshot() { return new ArrayList<>(items); }
    public synchronized int count() { return items.size(); }

    public synchronized int idByClient(int client) {
        for (Item it : items) {
            if (it.i == client) return it.Id;
        }
        return -1;
    }

    public synchronized Item findByClient(int client) {
        for (Item it : items) {
            if (it.i == client) return it;
        }
        return null;
    }
}