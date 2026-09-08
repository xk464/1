package com.example.dailycheck.ui.tasks;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailycheck.data.entity.Task;
import com.example.dailycheck.databinding.ItemTaskBinding;

/**
 * 任务列表适配器
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskVH> {

    public interface Listener {
        void onToggle(Task task, boolean completed);

        void onDelete(Task task);
    }

    private final Listener listener;

    public TaskAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Task> DIFF = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task o, @NonNull Task n) {
            return o.id == n.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task o, @NonNull Task n) {
            return o.isCompleted == n.isCompleted
                    && eq(o.title, n.title);
        }

        private boolean eq(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    };

    public void submit(java.util.List<Task> list) {
        submitList(list);
    }

    @NonNull
    @Override
    public TaskVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskBinding b = ItemTaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskVH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskVH h, int position) {
        Task task = getItem(position);
        h.bind(task);
    }

    class TaskVH extends RecyclerView.ViewHolder {
        private final ItemTaskBinding b;

        TaskVH(ItemTaskBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(Task task) {
            b.cbDone.setOnCheckedChangeListener(null);
            b.cbDone.setChecked(task.isCompleted);
            b.tvTitle.setText(task.title);
            b.tvTitle.setAlpha(task.isCompleted ? 0.5f : 1f);
            b.cbDone.setOnCheckedChangeListener((v, checked) -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onToggle(getItem(pos), checked);
                }
            });
            b.btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onDelete(getItem(pos));
                }
            });
        }
    }
}
