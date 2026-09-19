package com.pmr.admin;

import java.util.ArrayList;
import java.util.List;

public class ChanList {

    public static class Item {
        public int i;
        public int cod;
        public int Id;
        public int port;
        public int ip;
        public int ban;
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