package com.example.dailycheck.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.dailycheck.data.AppDatabase;
import com.example.dailycheck.data.dao.ExpenseRecordDao;
import com.example.dailycheck.data.dao.StudyRecordDao;
import com.example.dailycheck.data.dao.TaskDao;
import com.example.dailycheck.databinding.FragmentStatsBinding;
import com.example.dailycheck.util.DateUtils;
import com.example.dailycheck.widget.BarChartView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 统计页：支持月历、周历、饼图三种可视化模式切换
 * 汇总打卡完成率、学习时长、收支结余
 */
public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private AppDatabase db;
    private TaskDao taskDao;
    private StudyRecordDao studyDao;
    private ExpenseRecordDao expenseDao;

    /** 0=周历, 1=月历, 2=饼图 */
    private int mode = 0;

    // 月历当前显示的年月
    private int displayYear;
    private int displayMonth; // 0-11

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        taskDao = db.taskDao();
        studyDao = db.studyRecordDao();
        expenseDao = db.expenseRecordDao();
        java.util.Calendar c = java.util.Calendar.getInstance();
        displayYear = c.get(java.util.Calendar.YEAR);
        displayMonth = c.get(java.util.Calendar.MONTH);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.tabMode.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                mode = tab.getPosition();
                loadData();
            }
            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        // 周历
        binding.rvWeekCal.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.rvWeekCal.setHasFixedSize(true);

        // 月历
        binding.rvMonthCal.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.rvMonthCal.setHasFixedSize(true);

        binding.btnPrevMonth.setOnClickListener(v -> {
            displayMonth--;
            if (displayMonth < 0) { displayMonth = 11; displayYear--; }
            loadData();
        });
        binding.btnNextMonth.setOnClickListener(v -> {
            displayMonth++;
            if (displayMonth > 11) { displayMonth = 0; displayYear++; }
            loadData();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        // 统计范围：周历/饼图用当前周，月历用显示月
        String[] range;
        if (mode == 1) {
            // 月历模式：范围是显示的月份
            String monthStart = DateUtils.from(displayYear, displayMonth, 1);
            range = DateUtils.monthRange(monthStart);
        } else {
            // 周历/饼图模式：范围是当前周
            range = DateUtils.weekRange(DateUtils.today());
        }
        binding.tvRange.setText(range[0] + " ~ " + range[1]);

        // 1. 打卡完成率
        int totalTasks = taskDao.getTotalCountRange(range[0], range[1]);
        int completedTasks = taskDao.getCompletedCountRange(range[0], range[1]);
        int rate = totalTasks == 0 ? 0 : (int) (completedTasks * 100f / totalTasks);
        binding.tvTaskRate.setText(String.format(Locale.CHINA,
                "完成 %d/%d (%d%%)", completedTasks, totalTasks, rate));

        // 2. 学习总时长
        int totalMinutes = studyDao.getTotalMinutes(range[0], range[1]);
        binding.tvStudyTime.setText(String.format(Locale.CHINA,
                "%d 分钟 (%.1fh)", totalMinutes, totalMinutes / 60f));

        // 3. 收支结余
        double expense = expenseDao.getTotalExpense(range[0], range[1]);
        double income = expenseDao.getTotalIncome(range[0], range[1]);
        double balance = income - expense;
        binding.tvBalance.setText(String.format(Locale.CHINA, "%.2f", balance));
        binding.tvBalance.setTextColor(balance >= 0
                ? getResources().getColor(com.example.dailycheck.R.color.income_green, null)
                : getResources().getColor(com.example.dailycheck.R.color.expense_red, null));
        binding.tvIncomeExpense.setText(String.format(Locale.CHINA,
                "收入 %.2f  支出 %.2f", income, expense));

        // 按模式渲染不同视图
        switch (mode) {
            case 0: showWeekCalendar(range); break;
            case 1: showMonthCalendar(range); break;
            case 2: showPieCharts(range); break;
        }

        // 支出分类明细（所有模式都显示）
        renderExpenseBreakdown(range[0], range[1], expense);
    }

    /** 周历模式 */
    private void showWeekCalendar(String[] range) {
        binding.cardWeekCal.setVisibility(View.VISIBLE);
        binding.cardMonthCal.setVisibility(View.GONE);
        binding.cardPie.setVisibility(View.GONE);
        binding.cardBar.setVisibility(View.VISIBLE);

        List<DateUtils.CalendarCell> cells = DateUtils.buildWeekCells(range[0]);
        fillCellData(cells, range);
        StatsCalendarAdapter adapter = new StatsCalendarAdapter(this::onDayClick);
        adapter.setCells(cells);
        binding.rvWeekCal.setAdapter(adapter);

        // 柱状图
        renderBarChart(range);
    }

    /** 月历模式 */
    private void showMonthCalendar(String[] range) {
        binding.cardWeekCal.setVisibility(View.GONE);
        binding.cardMonthCal.setVisibility(View.VISIBLE);
        binding.cardPie.setVisibility(View.GONE);
        binding.cardBar.setVisibility(View.VISIBLE);

        binding.tvMonthLabel.setText(String.format(Locale.CHINA,
                "%d年%d月", displayYear, displayMonth + 1));

        List<DateUtils.CalendarCell> cells = DateUtils.buildMonthCells(displayYear, displayMonth);
        fillCellData(cells, range);
        StatsCalendarAdapter adapter = new StatsCalendarAdapter(this::onDayClick);
        adapter.setCells(cells);
        binding.rvMonthCal.setAdapter(adapter);

        // 柱状图
        renderBarChart(range);
    }

    /** 饼图模式 */
    private void showPieCharts(String[] range) {
        binding.cardWeekCal.setVisibility(View.GONE);
        binding.cardMonthCal.setVisibility(View.GONE);
        binding.cardPie.setVisibility(View.VISIBLE);
        binding.cardBar.setVisibility(View.GONE);

        // 学习科目饼图
        List<StudyRecordDao.SubjectTotal> subjectTotals = studyDao.getMinutesBySubject(range[0], range[1]);
        List<String> sLabels = new ArrayList<>();
        List<Float> sValues = new ArrayList<>();
        int studyTotal = 0;
        for (StudyRecordDao.SubjectTotal st : subjectTotals) {
            sLabels.add(st.subject == null ? "未分类" : st.subject);
            sValues.add((float) st.total);
            studyTotal += st.total;
        }
        binding.pieStudy.setData(sLabels, sValues,
                studyTotal > 0 ? studyTotal + "分钟" : "");
    }

    /** 渲染柱状图 */
    private void renderBarChart(String[] range) {
        List<StudyRecordDao.SubjectTotal> subjectTotals = studyDao.getMinutesBySubject(range[0], range[1]);
        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        for (StudyRecordDao.SubjectTotal st : subjectTotals) {
            labels.add(st.subject == null ? "未分类" : st.subject);
            values.add((float) st.total);
        }
        binding.barChart.setData(labels, values);
        binding.tvChartEmpty.setVisibility(subjectTotals.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** 填充每个单元格的打卡、学习、支出数据 */
    private void fillCellData(List<DateUtils.CalendarCell> cells, String[] range) {
        // 任务统计
        List<TaskDao.DayStat> taskStats = taskDao.getDayStats(range[0], range[1]);
        Map<String, TaskDao.DayStat> taskMap = new HashMap<>();
        for (TaskDao.DayStat s : taskStats) taskMap.put(s.date, s);

        // 学习统计
        List<StudyRecordDao.DailyTotal> studyStats = studyDao.getDailyTotals(range[0], range[1]);
        Map<String, Integer> studyMap = new HashMap<>();
        for (StudyRecordDao.DailyTotal d : studyStats) studyMap.put(d.date, d.total);

        // 支出统计
        List<ExpenseRecordDao.DailyTotal> expenseStats = expenseDao.getDailyExpense(range[0], range[1]);
        Map<String, Double> expenseMap = new HashMap<>();
        for (ExpenseRecordDao.DailyTotal d : expenseStats) expenseMap.put(d.date, d.total);

        for (DateUtils.CalendarCell cell : cells) {
            if (cell.date == null) continue;
            TaskDao.DayStat ts = taskMap.get(cell.date);
            if (ts != null) {
                cell.hasData = true;
                cell.taskTotal = ts.total;
                cell.taskDone = ts.done;
            }
            Integer sm = studyMap.get(cell.date);
            if (sm != null) cell.studyMinutes = sm;
            Double ea = expenseMap.get(cell.date);
            if (ea != null) cell.expenseAmount = ea;
        }
    }

    /** 渲染支出分类明细 + 支出饼图 */
    private void renderExpenseBreakdown(String start, String end, double totalExpense) {
        LinearLayout container = binding.llExpenseBreakdown;
        container.removeAllViews();
        List<ExpenseRecordDao.CategoryTotal> categories = expenseDao.getExpenseByCategory(start, end);

        // 支出饼图
        List<String> eLabels = new ArrayList<>();
        List<Float> eValues = new ArrayList<>();
        for (ExpenseRecordDao.CategoryTotal ct : categories) {
            eLabels.add(ct.category);
            eValues.add((float) ct.total);
        }
        binding.pieExpense.setData(eLabels, eValues,
                totalExpense > 0 ? String.format(Locale.CHINA, "支出 %.2f", totalExpense) : "");

        if (categories.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("暂无支出记录");
            empty.setPadding(0, 24, 0, 0);
            container.addView(empty);
            return;
        }
        for (ExpenseRecordDao.CategoryTotal ct : categories) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(com.example.dailycheck.R.layout.item_category_row, container, false);
            ((TextView) row.findViewById(com.example.dailycheck.R.id.tv_cat_name)).setText(ct.category);
            ((TextView) row.findViewById(com.example.dailycheck.R.id.tv_cat_amount))
                    .setText(String.format(Locale.CHINA, "%.2f", ct.total));
            ProgressBar pb = row.findViewById(com.example.dailycheck.R.id.pb_cat);
            pb.setMax(100);
            int pct = totalExpense <= 0 ? 0 : (int) (ct.total * 100f / totalExpense);
            pb.setProgress(pct);
            ((TextView) row.findViewById(com.example.dailycheck.R.id.tv_cat_percent))
                    .setText(pct + "%");
            container.addView(row);
        }
    }

    /** 点击日历单元格显示详情 */
    private void onDayClick(DateUtils.CalendarCell cell) {
        if (cell.date == null) return;
        StringBuilder sb = new StringBuilder(cell.date + "\n");
        if (cell.taskTotal > 0) {
            sb.append(String.format(Locale.CHINA, "任务: %d/%d\n", cell.taskDone, cell.taskTotal));
        }
        if (cell.studyMinutes > 0) {
            sb.append(String.format(Locale.CHINA, "学习: %d分钟\n", cell.studyMinutes));
        }
        if (cell.expenseAmount > 0) {
            sb.append(String.format(Locale.CHINA, "支出: %.2f元\n", cell.expenseAmount));
        }
        if (cell.taskTotal == 0 && cell.studyMinutes == 0 && cell.expenseAmount == 0) {
            sb.append("无记录");
        }
        Toast.makeText(requireContext(), sb.toString().trim(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
