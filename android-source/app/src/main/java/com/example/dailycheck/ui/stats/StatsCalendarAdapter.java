package com.example.dailycheck.ui.stats;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dailycheck.databinding.ItemStatsDayBinding;
import com.example.dailycheck.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计页日历适配器：支持周历(7格)和月历(42格)两种模式
 * 每个格显示日期、学习分钟数、支出额、打卡完成标记
 */
public class StatsCalendarAdapter extends RecyclerView.Adapter<StatsCalendarAdapter.DayVH> {

    public interface Listener {
        void onDayClick(DateUtils.CalendarCell cell);
    }

    private final List<DateUtils.CalendarCell> cells = new ArrayList<>();
    private final Listener listener;

    public StatsCalendarAdapter(Listener listener) {
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
        return new DayVH(ItemStatsDayBinding.inflate(
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
        private final ItemStatsDayBinding b;

        DayVH(ItemStatsDayBinding b) {
            super(b.getRoot());
            this.b = b;
        }

        void bind(DateUtils.CalendarCell cell) {
            if (cell.date == null) {
                b.tvDay.setText("");
                b.tvDay.setBackgroundResource(android.R.color.transparent);
                b.tvStudyMin.setVisibility(View.GONE);
                b.tvExpense.setVisibility(View.GONE);
                b.dotIndicator.setVisibility(View.GONE);
                b.getRoot().setOnClickListener(null);
                return;
            }

            String[] parts = cell.date.split("-");
            b.tvDay.setText(parts[2]);

            // 今日高亮
            if (cell.isToday) {
                b.tvDay.setBackgroundResource(com.example.dailycheck.R.drawable.bg_today_circle);
            } else {
                b.tvDay.setBackgroundResource(android.R.color.transparent);
            }

            // 学习时长
            if (cell.studyMinutes > 0) {
                b.tvStudyMin.setVisibility(View.VISIBLE);
                b.tvStudyMin.setText(cell.studyMinutes + "m");
            } else {
                b.tvStudyMin.setVisibility(View.GONE);
            }

            // 支出
            if (cell.expenseAmount > 0) {
                b.tvExpense.setVisibility(View.VISIBLE);
                b.tvExpense.setText(String.format(java.util.Locale.CHINA, "%.0f", cell.expenseAmount));
            } else {
                b.tvExpense.setVisibility(View.GONE);
            }

            // 打卡完成标记
            if (cell.taskTotal > 0 && cell.taskDone >= cell.taskTotal) {
                b.dotIndicator.setVisibility(View.VISIBLE);
            } else {
                b.dotIndicator.setVisibility(View.GONE);
            }

            b.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onDayClick(cell);
            });
        }
    }
}
