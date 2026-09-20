package com.pmr.admin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ListFile {

    public static class Item {
        public int id;
        public String name;
        public Item(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final List<Item> items = new ArrayList<>();

    public synchronized void clear() { items.clear(); }

    public synchronized void put(int id, String name) {
        for (Item it : items) {
            if (it.id == id) { it.name = name; return; }
        }
        items.add(new Item(id, name));
    }

    public synchronized String getName(int id) {
        for (Item it : items) {
            if (it.id == id) return it.name;
        }
        return "     ";
    }

    public synchronized List<Item> snapshot() { return new ArrayList<>(items); }
    public synchronized int count() { return items.size(); }

    public synchronized int load(File f) {
        items.clear();
        if (f == null || !f.exists()) return 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) parseLine(line);
        } catch (Exception e) { return 0; }
        return items.size();
    }

    public synchronized int loadFromStream(InputStream in) {
        items.clear();
        if (in == null) return 0;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) parseLine(line);
        } catch (Exception e) { return 0; }
        return items.size();
    }

    public synchronized void save(File f) {
        if (f == null) return;
        try (FileOutputStream fos = new FileOutputStream(f)) {
            StringBuilder sb = new StringBuilder();
            for (Item it : items) {
                sb.append(String.format("%05d", it.id)).append(' ')
                  .append(it.name).append('\n');
            }
            fos.write(sb.toString().getBytes("UTF-8"));
        } catch (Exception ignored) {}
    }

    private void parseLine(String line) {
        if (line == null) return;
        if (line.length() < 6) return;
        if (line.charAt(5) != ' ') return;
        try {
            int id = Integer.parseInt(line.substring(0, 5).trim());
            String name = line.substring(6);
            items.add(new Item(id, name));
        } catch (NumberFormatException ignored) {}
    }
}