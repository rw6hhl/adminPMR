package com.pmr.admin;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/* Адаптер списка абонентов V2.2.
 * С диагностическими Toast при нажатии кнопки БАН.
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
        List<ChanList.Item> sorted = new ArrayList<>();
        ChanList.Item active = null;
        for (ChanList.Item it : items) {
            if (it.i == activeClient) {
                active = it;
            } else {
                sorted.add(it);
            }
        }
        if (active != null) sorted.add(0, active);
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
            h.text.setTextColor(ContextCompat.getColor(ctx, R.color.c_red));
            h.text.setTypeface(null, Typeface.BOLD);
        } else {
            h.text.setTextColor(ContextCompat.getColor(ctx, R.color.c_black));
            h.text.setTypeface(null, Typeface.NORMAL);
        }

        if (it.ban == 1) {
            h.btnBan.setBackgroundTintList(
                    ContextCompat.getColorStateList(ctx, R.color.c_red));
        } else {
            h.btnBan.setBackgroundTintList(
                    ContextCompat.getColorStateList(ctx, R.color.c_green));
        }

        final int cli = it.i;
        final int banState = it.ban;
        h.btnBan.setOnClickListener(v -> {
            /* Диагностика */
            if (PmrService.pmrSocket == null) {
                Toast.makeText(ctx, R.string.toast_ban_null,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!PmrService.pmrSocket.isRunning()) {
                Toast.makeText(ctx, R.string.toast_ban_not_running,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            PmrService.pmrSocket.sendBan(cli);
            String msg = ctx.getString(R.string.toast_ban_sent, cli)
                    + (banState == 1 ? " (разбан)" : " (бан)");
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
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