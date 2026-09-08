package com.example.dailycheck.ui.expense;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailycheck.data.entity.ExpenseRecord;
import com.example.dailycheck.databinding.ItemExpenseBinding;

import java.util.List;
import java.util.Locale;

/**
 * 收支记录适配器
 */
public class ExpenseAdapter extends ListAdapter<ExpenseRecord, ExpenseAdapter.ExpenseVH> {

    public interface Listener {
        void onDelete(ExpenseRecord record);
    }

    private final Listener listener;

    private static final DiffUtil.ItemCallback<ExpenseRecord> DIFF = new DiffUtil.ItemCallback<ExpenseRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull ExpenseRecord o, @NonNull ExpenseRecord n) {
            return o.id == n.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ExpenseRecord o, @NonNull ExpenseRecord n) {
            return o.type == n.type && o.amount == n.amount
                    && eq(o.category, n.category) && eq(o.note, n.note);
        }

        private boolean eq(String a, String b) {
            return a == null ? b == null : a.equals(b);
        }
    };

    public ExpenseAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void submit(List<ExpenseRecord> list) {
        submitList(list);
    }

    @NonNull
    @Override
    public ExpenseVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ExpenseVH(ItemExpenseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseVH h, int position) {
        h.bind(getItem(position));
    }

    class ExpenseVH extends RecyclerView.ViewHolder {
        private final ItemExpenseBinding b;

        ExpenseVH(ItemExpenseBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(ExpenseRecord record) {
            boolean isIncome = record.type == ExpenseRecord.TYPE_INCOME;
            b.tvCategory.setText(record.category);
            b.tvAmount.setText(String.format(Locale.CHINA, "%s%.2f",
                    isIncome ? "+" : "-", record.amount));
            b.tvAmount.setTextColor(itemView.getContext().getResources().getColor(
                    isIncome ? com.example.dailycheck.R.color.income_green
                            : com.example.dailycheck.R.color.expense_red, null));
            b.tvNote.setText(record.note == null || record.note.isEmpty() ? "无备注" : record.note);
            b.btnDelete.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onDelete(getItem(pos));
                }
            });
        }
    }
}
