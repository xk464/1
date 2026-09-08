package com.example.dailycheck.ui.settings;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailycheck.databinding.ItemTemplateBinding;
import com.example.dailycheck.template.TemplateStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务模板列表适配器
 */
public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.VH> {

    public interface Listener {
        void onApply(TemplateStore.Template template);
    }

    private final List<TemplateStore.Template> items = new ArrayList<>();
    private final Listener listener;

    public TemplateAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<TemplateStore.Template> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemTemplateBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        private final ItemTemplateBinding b;

        VH(ItemTemplateBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(TemplateStore.Template t) {
            b.tvName.setText(t.name);
            b.tvSummary.setText(t.tasks.size() + " 个任务");
            b.getRoot().setOnClickListener(v -> {
                int p = getBindingAdapterPosition();
                if (p != RecyclerView.NO_POSITION) listener.onApply(items.get(p));
            });
        }
    }
}
