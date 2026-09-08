package com.example.dailycheck.ui.tasks;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dailycheck.R;
import com.example.dailycheck.data.AppDatabase;
import com.example.dailycheck.data.dao.ExpenseRecordDao;
import com.example.dailycheck.data.dao.StudyRecordDao;
import com.example.dailycheck.data.dao.TaskDao;
import com.example.dailycheck.data.entity.ExpenseRecord;
import com.example.dailycheck.data.entity.StudyRecord;
import com.example.dailycheck.data.entity.Task;
import com.example.dailycheck.databinding.DialogAddExpenseBinding;
import com.example.dailycheck.databinding.DialogAddStudyBinding;
import com.example.dailycheck.databinding.DialogAddTaskBinding;
import com.example.dailycheck.databinding.FragmentTasksBinding;
import com.example.dailycheck.util.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 打卡页：按日期管理任务清单，勾选完成，统计连续打卡天数
 */
public class TasksFragment extends Fragment {

    private FragmentTasksBinding binding;
    private AppDatabase db;
    private TaskDao taskDao;
    private StudyRecordDao studyDao;
    private ExpenseRecordDao expenseDao;
    private TaskAdapter adapter;

    private String selectedDate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            db = AppDatabase.getInstance(requireContext());
            taskDao = db.taskDao();
            studyDao = db.studyRecordDao();
            expenseDao = db.expenseRecordDao();
            selectedDate = DateUtils.today();
        } catch (Exception e) {
            com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("taskFragmentCreate", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new TaskAdapter(new TaskAdapter.Listener() {
            @Override
            public void onToggle(Task task, boolean completed) {
                new Thread(() -> {
                    try {
                        taskDao.setCompleted(task.id, completed);
                        task.isCompleted = completed;
                        requireActivity().runOnUiThread(() -> loadData());
                    } catch (Exception e) {
                        com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("taskToggle", e);
                    }
                }).start();
            }

            @Override
            public void onDelete(Task task) {
                new Thread(() -> {
                    try {
                        taskDao.delete(task);
                        requireActivity().runOnUiThread(() -> {
                            loadData();
                            Toast.makeText(requireContext(), "已删除任务", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("taskDelete", e);
                    }
                }).start();
            }
        });
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(adapter);

        binding.tvDate.setOnClickListener(v -> showDatePicker());
        binding.btnPrevDay.setOnClickListener(v -> changeDay(-1));
        binding.btnNextDay.setOnClickListener(v -> changeDay(1));

        binding.fabAdd.setOnClickListener(v -> showAddTaskDialog());

        // 快捷记录学习/消费
        binding.cardQuickStudy.setOnClickListener(v -> showQuickStudyDialog());
        binding.cardQuickExpense.setOnClickListener(v -> showQuickExpenseDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void changeDay(int delta) {
        selectedDate = DateUtils.plusDays(selectedDate, delta);
        loadData();
    }

    private void showDatePicker() {
        DateUtils.DateParts dp = DateUtils.split(selectedDate);
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            selectedDate = DateUtils.from(year, month, day);
            loadData();
        }, dp.year, dp.month, dp.day).show();
    }

    private void showAddTaskDialog() {
        DialogAddTaskBinding d = DialogAddTaskBinding.inflate(LayoutInflater.from(requireContext()));
        EditText input = d.etTitle;
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("添加任务")
                .setView(d.getRoot())
                .setPositiveButton("添加", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入任务名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new Thread(() -> {
                        try {
                            taskDao.insert(new Task(title, selectedDate));
                            requireActivity().runOnUiThread(() -> loadData());
                        } catch (Exception e) {
                            com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("taskInsert", e);
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadData() {
        if (taskDao == null || binding == null) return;
        try {
            binding.tvDate.setText(selectedDate + " " + DateUtils.weekdayChinese(selectedDate));
            List<Task> tasks = taskDao.getTasksByDate(selectedDate);
            adapter.submit(tasks);
            binding.tvEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
            refreshStats();
        } catch (Exception e) {
            com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("taskLoadData", e);
        }
    }

    private void refreshStats() {
        if (taskDao == null || binding == null) return;
        try {
            int total = taskDao.getTotalCount(selectedDate);
            int completed = taskDao.getCompletedCount(selectedDate);
            binding.tvProgress.setText(completed + " / " + total);

            // 连续打卡天数：基于已完成记录的日期列表
            List<String> completedDates = taskDao.getCompletedDates();
            int streak = DateUtils.calculateStreak(completedDates);
            binding.tvStreak.setText(String.format(java.util.Locale.CHINA, "连续打卡 %d 天", streak));

            // 进度条
            int percent = total == 0 ? 0 : (int) (completed * 100f / total);
            binding.progress.setMax(100);
            binding.progress.setProgress(percent);

            // 快捷入口今日数据
            if (studyDao != null) {
                int studyMin = studyDao.getTotalMinutes(selectedDate, selectedDate);
                binding.tvQuickStudy.setText(String.format(java.util.Locale.CHINA,
                        "今日 %d 分钟", studyMin));
            }
            if (expenseDao != null) {
                double expense = expenseDao.getTotalExpense(selectedDate, selectedDate);
                binding.tvQuickExpense.setText(String.format(java.util.Locale.CHINA,
                        "今日支出 %.0f", expense));
            }
        } catch (Exception e) {
            com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("taskRefreshStats", e);
        }
    }

    /** 快捷添加学习记录 */
    private void showQuickStudyDialog() {
        DialogAddStudyBinding d = DialogAddStudyBinding.inflate(LayoutInflater.from(requireContext()));
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("记录学习（" + selectedDate + "）")
                .setView(d.getRoot())
                .setPositiveButton("保存", (dialog, which) -> {
                    String subject = d.etSubject.getText().toString().trim();
                    String content = d.etContent.getText().toString().trim();
                    String durStr = d.etDuration.getText().toString().trim();
                    if (subject.isEmpty()) {
                        Toast.makeText(requireContext(), "请填写科目", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (durStr.isEmpty()) {
                        Toast.makeText(requireContext(), "请填写时长", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int duration;
                    try {
                        duration = Integer.parseInt(durStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "时长无效", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    StudyRecord record = new StudyRecord(subject, content, duration, selectedDate);
                    new Thread(() -> {
                        try {
                            studyDao.insert(record);
                            requireActivity().runOnUiThread(() -> {
                                refreshStats();
                                Toast.makeText(requireContext(),
                                        "已记录：" + subject + " " + duration + "分钟",
                                        Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            com.example.dailycheck.util.ErrorLogger.get(requireContext())
                                    .log("quickStudyInsert", e);
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 快捷添加消费记录 */
    private void showQuickExpenseDialog() {
        DialogAddExpenseBinding d = DialogAddExpenseBinding.inflate(LayoutInflater.from(requireContext()));
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("记一笔账（" + selectedDate + "）")
                .setView(d.getRoot())
                .setPositiveButton("保存", (dialog, which) -> {
                    String category = d.etCategory.getText().toString().trim();
                    String amountStr = d.etAmount.getText().toString().trim();
                    String note = d.etNote.getText().toString().trim();
                    if (category.isEmpty()) {
                        Toast.makeText(requireContext(), "请填写分类", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amountStr.isEmpty()) {
                        Toast.makeText(requireContext(), "请填写金额", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "金额无效", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int type = d.rbExpense.isChecked()
                            ? ExpenseRecord.TYPE_EXPENSE : ExpenseRecord.TYPE_INCOME;
                    ExpenseRecord record = new ExpenseRecord(type, category, amount, note, selectedDate);
                    new Thread(() -> {
                        try {
                            expenseDao.insert(record);
                            requireActivity().runOnUiThread(() -> {
                                refreshStats();
                                Toast.makeText(requireContext(),
                                        "已记录：" + category + " " + amount + "元",
                                        Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            com.example.dailycheck.util.ErrorLogger.get(requireContext())
                                    .log("quickExpenseInsert", e);
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
