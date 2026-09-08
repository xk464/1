package com.example.dailycheck.ui.calendar;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailycheck.databinding.ItemCalendarDayBinding;
import com.example.dailycheck.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 日历网格适配器：6 行 x 7 列
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayVH> {

    public interface Listener {
        void onDayClick(DateUtils.CalendarCell cell);
    }

    private final List<DateUtils.CalendarCell> cells = new ArrayList<>();
    private final Listener listener;

    public CalendarAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setCells(List<DateUtils.CalendarCell> list) {
        cells.clear();
        cells.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DayVH(ItemCalendarDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DayVH h, int position) {
        h.bind(cells.get(position));
    }

    @Override
    public int getItemCount() {
        return cells.size();
    }

    class DayVH extends RecyclerView.ViewHolder {
        private final ItemCalendarDayBinding b;

        DayVH(ItemCalendarDayBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(DateUtils.CalendarCell cell) {
            if (cell.date == null) {
                // 空位
                b.tvDay.setText("");
                b.getRoot().setBackgroundResource(android.R.color.transparent);
                b.tvRate.setVisibility(android.view.View.GONE);
                b.getRoot().setOnClickListener(null);
                return;
            }
            String[] parts = cell.date.split("-");
            b.tvDay.setText(parts[2]);
            b.tvRate.setVisibility(android.view.View.VISIBLE);

            if (cell.taskTotal == 0) {
                // 无任务
                b.tvRate.setText("");
                b.getRoot().setBackgroundResource(com.example.dailycheck.R.drawable.bg_calendar_empty);
            } else if (cell.taskDone >= cell.taskTotal) {
                // 全部完成
                b.tvRate.setText("完成");
                b.getRoot().setBackgroundResource(com.example.dailycheck.R.drawable.bg_calendar_done);
            } else {
                // 部分完成
                int pct = (int) (cell.taskDone * 100f / cell.taskTotal);
                b.tvRate.setText(pct + "%");
                b.getRoot().setBackgroundResource(com.example.dailycheck.R.drawable.bg_calendar_partial);
            }

            // 今天高亮
            if (cell.isToday) {
                b.tvDay.setBackgroundResource(com.example.dailycheck.R.drawable.bg_today_circle);
            } else {
                b.tvDay.setBackgroundResource(android.R.color.transparent);
            }

            b.getRoot().setOnClickListener(v -> listener.onDayClick(cell));
        }
    }
}
