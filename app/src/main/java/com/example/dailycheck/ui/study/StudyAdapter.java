package com.example.dailycheck.ui.study;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailycheck.data.entity.StudyRecord;
import com.example.dailycheck.databinding.ItemStudyBinding;

import java.util.List;

/**
 * 学习记录适配器
 */
public class StudyAdapter extends ListAdapter<StudyRecord, StudyAdapter.StudyVH> {

    public interface Listener {
        void onDelete(StudyRecord record);
    }

    private final Listener listener;

    private static final DiffUtil.ItemCallback<StudyRecord> DIFF = new DiffUtil.ItemCallback<StudyRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull StudyRecord o, @NonNull StudyRecord n) {
            return o.id == n.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull StudyRecord o, @NonNull StudyRecord n) {
            return o.subject.equals(n.subject)
                    && eq(o.content, n.content)
                    && o.durationMinutes == n.durationMinutes;
        }

        private boolean eq(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    };

    public StudyAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void submit(List<StudyRecord> list) {
        submitList(list);
    }

    @NonNull
    @Override
    public StudyVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StudyVH(ItemStudyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull StudyVH h, int position) {
        h.bind(getItem(position));
    }

    class StudyVH extends RecyclerView.ViewHolder {
        private final ItemStudyBinding b;

        StudyVH(ItemStudyBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(StudyRecord record) {
            b.tvSubject.setText(record.subject);
            b.tvDuration.setText(record.durationMinutes + " 分钟");
            b.tvContent.setText(record.content == null || record.content.isEmpty()
                    ? "无内容" : record.content);
            b.btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onDelete(getItem(pos));
                }
            });
        }
    }
}
