package com.pmr.admin;

import java.util.ArrayList;
import java.util.List;

/* Список абонентов в канале.
 * Заполняется из ответа сервера на команду 234.
 * Каждая запись — 13 байт: i(2) cod(2) Id(2) port(2) ip(4) ban(1).
 */
public class ChanList {

    /* Одна запись списка канала */
    public static class Item {
        public int i;      // порядковый номер в списке канала (от 0)
        public int cod;    // код (не используется)
        public int Id;     // PMR-номер (5 цифр)
        public int port;   // порт
        public int ip;     // IPv4 в 32-битном виде
        public int ban;    // 0 — не забанен, 1 — забанен
    }

    private final List<Item> items = new ArrayList<>();

    public synchronized void clear() {
        items.clear();
    }

    public synchronized void add(Item it) {
        if (it == null) return;
        items.add(it);
    }

    public synchronized List<Item> snapshot() {
        return new ArrayList<>(items);
    }

    public synchronized int count() {
        return items.size();
    }

    /* Найти PMR-Id по номеру client (i == client).
     * Возвращает -1, если запись не найдена.
     */
    public synchronized int idByClient(int client) {
        for (Item it : items) {
            if (it.i == client) return it.Id;
        }
        return -1;
    }

    /* Найти запись по client (i == client). Возвращает null, если не найдена. */
    public synchronized Item findByClient(int client) {
        for (Item it : items) {
            if (it.i == client) return it;
        }
        return null;
    }
}