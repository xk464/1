package com.example.dailycheck.ui.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dailycheck.data.AppDatabase;
import com.example.dailycheck.data.dao.ExpenseRecordDao;
import com.example.dailycheck.data.dao.StudyRecordDao;
import com.example.dailycheck.data.dao.TaskDao;
import com.example.dailycheck.databinding.FragmentStatsBinding;
import com.example.dailycheck.util.DateUtils;
import com.example.dailycheck.widget.BarChartView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 统计页：按周/月维度汇总打卡完成率、学习时长、收支结余，并绘制柱状图与分类明细
 */
public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private AppDatabase db;
    private TaskDao taskDao;
    private StudyRecordDao studyDao;
    private ExpenseRecordDao expenseDao;

    private boolean monthly = false; // false=周 true=月

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        taskDao = db.taskDao();
        studyDao = db.studyRecordDao();
        expenseDao = db.expenseRecordDao();
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
        binding.rgPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            monthly = checkedId == binding.rbMonth.getId();
            loadData();
        });
        binding.rbWeek.setChecked(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        String[] range = monthly
                ? DateUtils.monthRange(DateUtils.today())
                : DateUtils.weekRange(DateUtils.today());
        binding.tvRange.setText(range[0] + " ~ " + range[1]);

        // 1. 打卡完成率
        int totalTasks = taskDao.getTotalCountRange(range[0], range[1]);
        int completedTasks = taskDao.getCompletedCountRange(range[0], range[1]);
        int rate = totalTasks == 0 ? 0 : (int) (completedTasks * 100f / totalTasks);
        binding.tvTaskRate.setText(String.format(Locale.CHINA,
                "完成 %d / %d 项 (%d%%)", completedTasks, totalTasks, rate));

        // 2. 学习总时长
        int totalMinutes = studyDao.getTotalMinutes(range[0], range[1]);
        binding.tvStudyTime.setText(String.format(Locale.CHINA,
                "%d 分钟 (%.1f 小时)", totalMinutes, totalMinutes / 60f));

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

        // 4. 各科目学习时长柱状图
        List<StudyRecordDao.SubjectTotal> subjectTotals = studyDao.getMinutesBySubject(range[0], range[1]);
        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        for (StudyRecordDao.SubjectTotal st : subjectTotals) {
            labels.add(st.subject == null ? "未分类" : st.subject);
            values.add((float) st.total);
        }
        binding.barChart.setData(labels, values);
        binding.tvChartEmpty.setVisibility(subjectTotals.isEmpty() ? View.VISIBLE : View.GONE);

        // 5. 支出分类明细
        renderExpenseBreakdown(range[0], range[1], expense);
    }

    /** 渲染支出分类明细列表 */
    private void renderExpenseBreakdown(String start, String end, double totalExpense) {
        LinearLayout container = binding.llExpenseBreakdown;
        container.removeAllViews();
        List<ExpenseRecordDao.CategoryTotal> categories = expenseDao.getExpenseByCategory(start, end);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
