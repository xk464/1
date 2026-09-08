package com.example.dailycheck.ui.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.dailycheck.data.AppDatabase;
import com.example.dailycheck.data.dao.TaskDao;
import com.example.dailycheck.databinding.FragmentCalendarBinding;
import com.example.dailycheck.util.DateUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 日历页：以月历形式直观查看每日打卡完成率
 */
public class CalendarFragment extends Fragment {

    private FragmentCalendarBinding binding;
    private AppDatabase db;
    private TaskDao taskDao;
    private CalendarAdapter adapter;

    private int displayYear;
    private int displayMonth; // 0-11

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        taskDao = db.taskDao();
        Calendar c = Calendar.getInstance();
        displayYear = c.get(Calendar.YEAR);
        displayMonth = c.get(Calendar.MONTH);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new CalendarAdapter(this::onDayClick);
        binding.rvCalendar.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.rvCalendar.setHasFixedSize(true);
        binding.rvCalendar.setAdapter(adapter);

        binding.btnPrevMonth.setOnClickListener(v -> {
            displayMonth--;
            if (displayMonth < 0) {
                displayMonth = 11;
                displayYear--;
            }
            loadCalendar();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            displayMonth++;
            if (displayMonth > 11) {
                displayMonth = 0;
                displayYear++;
            }
            loadCalendar();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCalendar();
    }

    private void loadCalendar() {
        binding.tvMonth.setText(String.format(Locale.CHINA, "%d年%d月", displayYear, displayMonth + 1));
        List<DateUtils.CalendarCell> cells = DateUtils.buildMonthCells(displayYear, displayMonth);

        // 计算当月日期范围（含补齐可能跨入相邻月，这里按实际月份天日计算）
        Calendar c = Calendar.getInstance();
        c.set(displayYear, displayMonth, 1);
        String[] monthRange = DateUtils.monthRange(DateUtils.from(displayYear, displayMonth, 1));
        List<TaskDao.DayStat> stats = taskDao.getDayStats(monthRange[0], monthRange[1]);
        Map<String, TaskDao.DayStat> map = new HashMap<>();
        for (TaskDao.DayStat s : stats) {
            map.put(s.date, s);
        }
        for (DateUtils.CalendarCell cell : cells) {
            if (cell.date == null) continue;
            TaskDao.DayStat s = map.get(cell.date);
            if (s != null) {
                cell.hasData = true;
                cell.taskTotal = s.total;
                cell.taskDone = s.done;
            }
        }
        adapter.setCells(cells);
    }

    private void onDayClick(DateUtils.CalendarCell cell) {
        if (cell.date == null) return;
        String msg;
        if (cell.taskTotal == 0) {
            msg = cell.date + "：无打卡记录";
        } else {
            msg = String.format(Locale.CHINA, "%s：完成 %d / %d 项任务",
                    cell.date, cell.taskDone, cell.taskTotal);
        }
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
