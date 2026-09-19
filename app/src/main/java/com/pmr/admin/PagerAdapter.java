package com.pmr.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/* 6 вкладок — по одной на каждую секцию.
 * При создании вкладки вызывает методы bindTab* у MainActivity.
 */
public class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.VH> {

    private static final int COUNT = 6;
    private final MainActivity act;

    public PagerAdapter(MainActivity a) {
        this.act = a;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case 0: layoutId = R.layout.tab_chan; break;
            case 1: layoutId = R.layout.tab_list; break;
            case 2: layoutId = R.layout.tab_alog; break;
            case 3: layoutId = R.layout.tab_term; break;
            case 4: layoutId = R.layout.tab_ctrl; break;
            default: layoutId = R.layout.tab_hint; break;
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        switch (position) {
            case 0: {
                LinearLayout container = h.itemView.findViewById(R.id.chanContainer);
                Button btn = h.itemView.findViewById(R.id.btnRefresh);
                if (btn != null) {
                    btn.setOnClickListener(v -> {
                        if (PmrService.pmrSocket != null)
                            PmrService.pmrSocket.sendL();
                    });
                }
                act.bindTabChan(container);
                break;
            }
            case 1: {
                LinearLayout container = h.itemView.findViewById(R.id.listContainer);
                act.bindTabList(container);
                break;
            }
            case 2: {
                TextView alog = h.itemView.findViewById(R.id.alogText);
                TextView num  = h.itemView.findViewById(R.id.alogActiveNum);
                TextView nm   = h.itemView.findViewById(R.id.alogActiveName);
                Button btnClear = h.itemView.findViewById(R.id.btnAlogClear);
                if (btnClear != null) {
                    btnClear.setOnClickListener(v -> {
                        if (PmrService.activeLog != null)
                            PmrService.activeLog.clear();
                    });
                }
                act.bindTabAlog(alog, num, nm);
                break;
            }
            case 3: {
                TextView log = h.itemView.findViewById(R.id.termLog);
                EditText in  = h.itemView.findViewById(R.id.termInput);
                Button btnSend  = h.itemView.findViewById(R.id.btnSend);
                Button btnClear = h.itemView.findViewById(R.id.btnTermClear);
                if (btnSend != null) {
                    btnSend.setOnClickListener(v -> act.sendTermCommand());
                }
                if (btnClear != null) {
                    btnClear.setOnClickListener(v -> {
                        if (PmrService.webLog != null)
                            PmrService.webLog.clear();
                    });
                }
                act.bindTabTerm(log, in);
                break;
            }
            case 4: {
                Button btn260 = h.itemView.findViewById(R.id.btn260);
                Button btn261 = h.itemView.findViewById(R.id.btn261);
                if (btn260 != null) {
                    btn260.setOnClickListener(v -> {
                        if (PmrService.pmrSocket != null)
                            PmrService.pmrSocket.send260(0);
                    });
                }
                if (btn261 != null) {
                    btn261.setOnClickListener(v -> {
                        if (PmrService.pmrSocket != null)
                            PmrService.pmrSocket.send260(1);
                    });
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    public int getItemCount() { return COUNT; }

    @Override
    public int getItemViewType(int position) { return position; }

    static class VH extends RecyclerView.ViewHolder {
        VH(@NonNull View v) { super(v); }
    }
}