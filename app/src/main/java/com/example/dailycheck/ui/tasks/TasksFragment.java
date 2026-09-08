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
import com.example.dailycheck.data.dao.TaskDao;
import com.example.dailycheck.data.entity.Task;
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
    private TaskAdapter adapter;

    private String selectedDate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        taskDao = db.taskDao();
        selectedDate = DateUtils.today();
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
        binding.tvDate.setText(selectedDate + " " + DateUtils.weekdayChinese(selectedDate));
        List<Task> tasks = taskDao.getTasksByDate(selectedDate);
        adapter.submit(tasks);
        binding.tvEmpty.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        refreshStats();
    }

    private void refreshStats() {
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
