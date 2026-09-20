package com.pmr.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/* Адаптер списка абонентов.
 * Формат строки: <ID> <Имя> <частота>  [🔴/🟢]
 * Цвет шрифта: зелёный — активный, чёрный — остальные.
 * Кнопка бана: красная — забанен, зелёная — не забанен.
 */
public class ChanAdapter extends RecyclerView.Adapter<ChanAdapter.VH> {

    private final Context ctx;
    private List<ChanList.Item> items = new ArrayList<>();
    private int activeClient = -1;
    private ListFile listFile;

    public ChanAdapter(Context c) {
        this.ctx = c;
    }

    public void setData(List<ChanList.Item> items, int activeClient, ListFile lf) {
        /* Сортировка: сначала активные, затем остальные */
        List<ChanList.Item> sorted = new ArrayList<>();
        ChanList.Item active = null;
        for (ChanList.Item it : items) {
            if (it.i == activeClient) {
                active = it;
            } else {
                sorted.add(it);
            }
        }
        if (active != null) {
            sorted.add(0, active);
        }
        this.items = sorted;
        this.activeClient = activeClient;
        this.listFile = lf;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chan, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChanList.Item it = items.get(position);
        String nm = (listFile != null) ? listFile.getName(it.Id) : "";
        if (nm == null) nm = "";
        nm = nm.trim();
        String line = String.format("%05d %s", it.Id, nm);

        h.text.setText(line);
        h.text.setTextSize(24f);

        if (it.i == activeClient) {
            h.text.setTextColor(ContextCompat.getColor(ctx, R.color.c_green));
        } else {
            h.text.setTextColor(ContextCompat.getColor(ctx, R.color.c_black));
        }

        /* Кнопка бана */
        if (it.ban == 1) {
            h.btnBan.setBackgroundTintList(
                    ContextCompat.getColorStateList(ctx, R.color.c_red));
        } else {
            h.btnBan.setBackgroundTintList(
                    ContextCompat.getColorStateList(ctx, R.color.c_green));
        }

        final int cli = it.i;
        h.btnBan.setOnClickListener(v -> {
            if (PmrService.pmrSocket != null) {
                PmrService.pmrSocket.sendBan(cli);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView text;
        Button btnBan;
        VH(@NonNull View v) {
            super(v);
            text = v.findViewById(R.id.itemText);
            btnBan = v.findViewById(R.id.itemBtnBan);
        }
    }
}