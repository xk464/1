package com.example.dailycheck.ui.study;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dailycheck.R;
import com.example.dailycheck.data.AppDatabase;
import com.example.dailycheck.data.dao.StudyRecordDao;
import com.example.dailycheck.data.entity.StudyRecord;
import com.example.dailycheck.databinding.DialogAddStudyBinding;
import com.example.dailycheck.databinding.FragmentStudyBinding;
import com.example.dailycheck.util.DateUtils;

import java.util.List;
import java.util.Locale;

/**
 * 学习页：按日期记录学习内容、科目、时长，并汇总当日学习时长
 * 内置学习计时器（秒表），支持暂停/恢复，停止时保存为学习记录
 */
public class StudyFragment extends Fragment {

    private static final String TIMER_PREFS = "study_timer";
    private static final String KEY_STATE = "state";        // 0=空闲, 1=运行, 2=暂停
    private static final String KEY_START_TIME = "start_time"; // 计时开始的绝对时间戳
    private static final String KEY_ACCUM_MS = "accum_ms";    // 已累计毫秒数（暂停时用）
    private static final String KEY_GOAL_MIN = "goal_min";   // 目标时长（分钟），0 = 无目标
    private static final String KEY_GOAL_REACHED = "goal_reached"; // 本轮目标是否已提醒过

    private static final int STATE_IDLE = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_PAUSED = 2;

    private FragmentStudyBinding binding;
    private AppDatabase db;
    private StudyRecordDao studyDao;
    private StudyAdapter adapter;
    private String selectedDate;

    // 计时器
    private SharedPreferences timerSP;
    private Handler timerHandler;
    /** 上次更新通知的墙钟时间戳，用于显式控制通知刷新频率（>= 1000ms 才更一次） */
    private long lastNotifWallMs = 0L;
    /** 是否在等待通知权限回调 */
    private boolean pendingStartAfterPermission = false;

    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            updateTimerDisplay();
            updateGoalProgress();
            checkGoalReached();
            // 每 >= 1 秒更一次通知，节省电量
            long now = System.currentTimeMillis();
            if (now - lastNotifWallMs >= 1000L) {
                TimerNotificationHelper.show(requireContext(), getElapsedMs(), true, getGoalMinutes());
                lastNotifWallMs = now;
            }
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        studyDao = db.studyRecordDao();
        selectedDate = DateUtils.today();
        timerSP = requireContext().getSharedPreferences(TIMER_PREFS, 0);
        timerHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new StudyAdapter(record -> {
            new Thread(() -> {
                try {
                    studyDao.delete(record);
                    requireActivity().runOnUiThread(() -> {
                        loadData();
                        Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("studyDelete", e);
                }
            }).start();
        });
        binding.rvStudy.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStudy.setAdapter(adapter);

        binding.tvDate.setOnClickListener(v -> showDatePicker());
        binding.btnPrevDay.setOnClickListener(v -> changeDay(-1));
        binding.btnNextDay.setOnClickListener(v -> changeDay(1));
        binding.fabAdd.setOnClickListener(v -> showAddDialog());

        // 计时按钮
        binding.btnTimerStart.setOnClickListener(v -> startOrResumeTimer());
        binding.btnTimerPause.setOnClickListener(v -> pauseTimer());
        binding.btnTimerStop.setOnClickListener(v -> stopTimerAndSave());
        binding.btnTimerDiscard.setOnClickListener(v -> discardTimer());
        binding.tvGoalSetting.setOnClickListener(v -> showGoalPicker());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
        restoreTimerState();
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerTick);
    }

    // ==================== 计时功能 ====================

    private void startOrResumeTimer() {
        // Android 13+ 需要运行时通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingStartAfterPermission = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
            return;
        }
        doStartOrResumeTimer();
    }

    /** 真正执行启动/恢复计时（假定权限已满足） */
    private void doStartOrResumeTimer() {
        int state = timerSP.getInt(KEY_STATE, STATE_IDLE);
        SharedPreferences.Editor ed = timerSP.edit();
        long now = System.currentTimeMillis();

        if (state == STATE_IDLE) {
            ed.putLong(KEY_START_TIME, now);
            ed.putLong(KEY_ACCUM_MS, 0L);
            ed.putBoolean(KEY_GOAL_REACHED, false); // 新一轮计时重置达成标记
        } else if (state == STATE_PAUSED) {
            long accum = timerSP.getLong(KEY_ACCUM_MS, 0L);
            ed.putLong(KEY_START_TIME, now);
            ed.putLong(KEY_ACCUM_MS, accum);
        }
        ed.putInt(KEY_STATE, STATE_RUNNING).apply();
        lastNotifWallMs = 0L; // 重置通知节流
        updateTimerUI();
        updateGoalProgress();
        timerHandler.post(timerTick);
        TimerNotificationHelper.show(requireContext(), getElapsedMs(), true, getGoalMinutes());
    }

    private void pauseTimer() {
        long accum = getElapsedMs();
        timerSP.edit()
                .putInt(KEY_STATE, STATE_PAUSED)
                .putLong(KEY_ACCUM_MS, accum)
                .apply();
        timerHandler.removeCallbacks(timerTick);
        updateTimerUI();
        updateTimerDisplay();
        updateGoalProgress();
        lastNotifWallMs = 0L;
        TimerNotificationHelper.show(requireContext(), accum, false, getGoalMinutes());
    }

    private void stopTimerAndSave() {
        long elapsedMs = getElapsedMs();
        if (elapsedMs < 1000) {
            clearTimerState();
            Toast.makeText(requireContext(), "计时太短，未保存", Toast.LENGTH_SHORT).show();
            return;
        }
        int minutes = (int) ((elapsedMs + 59999) / 60000);
        // 不先清状态：保存对话框取消或校验失败时计时数据得以保留
        showSaveDialog(minutes, elapsedMs);
    }

    /** 二次确认后丢弃本次计时（不保存） */
    private void discardTimer() {
        long elapsedMs = getElapsedMs();
        if (elapsedMs < 1000) {
            clearTimerState();
            Toast.makeText(requireContext(), "已放弃计时", Toast.LENGTH_SHORT).show();
            return;
        }
        String timeText = formatMsShort(elapsedMs);
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("放弃计时")
                .setMessage("本次已计时 " + timeText + "，放弃后不会保存为学习记录。确认放弃吗？")
                .setPositiveButton("放弃", (d, w) -> {
                    clearTimerState();
                    Toast.makeText(requireContext(), "已放弃计时", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("继续计时", null)
                .show();
    }

    /** 清除所有计时状态（持久化 + Handler + 通知 + UI） */
    private void clearTimerState() {
        timerSP.edit().clear().apply();
        timerHandler.removeCallbacks(timerTick);
        TimerNotificationHelper.cancel(requireContext());
        lastNotifWallMs = 0L;
        updateTimerDisplay();
        updateTimerUI();
    }

    /** 格式化毫秒为紧凑字符串，如 "23 分 15 秒" */
    private static String formatMsShort(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return String.format(Locale.CHINA, "%d 小时 %02d 分 %02d 秒", h, m, s);
        return String.format(Locale.CHINA, "%d 分 %02d 秒", m, s);
    }

    // ==================== 计时目标 ====================

    /** 当前目标分钟数，0 = 无目标 */
    private int getGoalMinutes() {
        return timerSP.getInt(KEY_GOAL_MIN, 0);
    }

    /** 弹出目标时长选择对话框 */
    private void showGoalPicker() {
        String[] labels = {"无目标", "25 分钟", "45 分钟", "60 分钟", "90 分钟", "120 分钟"};
        final int[] values = {0, 25, 45, 60, 90, 120};
        int current = getGoalMinutes();
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) { checked = i; break; }
        }
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("设置计时目标")
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    timerSP.edit().putInt(KEY_GOAL_MIN, values[which])
                            .putBoolean(KEY_GOAL_REACHED, false).apply();
                    updateGoalProgress();
                    updateTimerUI();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 刷新目标进度条与目标设置按钮文字 */
    private void updateGoalProgress() {
        int goalMin = getGoalMinutes();
        if (goalMin <= 0) {
            binding.pbGoalProgress.setVisibility(View.GONE);
            binding.tvGoalSetting.setText("目标：无");
            return;
        }
        binding.pbGoalProgress.setVisibility(View.VISIBLE);
        long goalMs = goalMin * 60_000L;
        long elapsed = getElapsedMs();
        int progress = (int) Math.min(100L, elapsed * 100L / goalMs);
        binding.pbGoalProgress.setProgress(progress);
        binding.tvGoalSetting.setText(String.format(Locale.CHINA, "目标：%d 分钟", goalMin));
    }

    /** 检测是否到达目标，到达后弹 Toast 提醒一次（不自动停止） */
    private void checkGoalReached() {
        int goalMin = getGoalMinutes();
        if (goalMin <= 0) return;
        boolean reached = timerSP.getBoolean(KEY_GOAL_REACHED, false);
        if (reached) return;
        long goalMs = goalMin * 60_000L;
        if (getElapsedMs() >= goalMs) {
            timerSP.edit().putBoolean(KEY_GOAL_REACHED, true).apply();
            // 震动反馈
            android.os.Vibrator v = (android.os.Vibrator) requireContext()
                    .getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(new long[]{0, 300, 200, 300}, -1);
            }
            Toast.makeText(requireContext(),
                    "目标达成！已学习 " + goalMin + " 分钟，可继续或保存",
                    Toast.LENGTH_LONG).show();
        }
    }

    /** 应用/界面重新打开时恢复计时状态 */
    private void restoreTimerState() {
        int state = timerSP.getInt(KEY_STATE, STATE_IDLE);
        lastNotifWallMs = 0L;
        updateTimerDisplay();
        updateGoalProgress();
        updateTimerUI();
        if (state == STATE_RUNNING) {
            timerHandler.post(timerTick);
            TimerNotificationHelper.show(requireContext(), getElapsedMs(), true, getGoalMinutes());
        } else if (state == STATE_PAUSED) {
            TimerNotificationHelper.show(requireContext(), getElapsedMs(), false, getGoalMinutes());
        } else {
            TimerNotificationHelper.cancel(requireContext());
        }
    }

    /** 当前已累计毫秒数（统一用这个方法计算，避免 now 不一致） */
    private long getElapsedMs() {
        int state = timerSP.getInt(KEY_STATE, STATE_IDLE);
        long accum = timerSP.getLong(KEY_ACCUM_MS, 0L);
        if (state == STATE_RUNNING) {
            long start = timerSP.getLong(KEY_START_TIME, 0L);
            accum += System.currentTimeMillis() - start;
        }
        return Math.max(0L, accum);
    }

    private void updateTimerDisplay() {
        long ms = getElapsedMs();
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        String text = String.format(Locale.CHINA, "%02d:%02d:%02d", h, m, s);
        binding.tvTimerDisplay.setText(text);
    }

    /** 根据计时状态更新三个按钮的启用/禁用和文字 + 底部提示行 */
    private void updateTimerUI() {
        int state = timerSP.getInt(KEY_STATE, STATE_IDLE);
        boolean isToday = selectedDate.equals(DateUtils.today());
        String dateLabel = isToday ? "今日" : selectedDate;
        switch (state) {
            case STATE_RUNNING:
                binding.btnTimerStart.setText("运行中");
                binding.btnTimerStart.setEnabled(false);
                binding.btnTimerPause.setText("暂停");
                binding.btnTimerPause.setEnabled(true);
                binding.btnTimerStop.setEnabled(true);
                binding.btnTimerDiscard.setVisibility(View.VISIBLE);
                binding.tvTimerHint.setText("计时进行中 · 保存后归属 " + dateLabel);
                break;
            case STATE_PAUSED:
                binding.btnTimerStart.setText("继续");
                binding.btnTimerStart.setEnabled(true);
                binding.btnTimerPause.setText("暂停");
                binding.btnTimerPause.setEnabled(false);
                binding.btnTimerStop.setEnabled(true);
                binding.btnTimerDiscard.setVisibility(View.VISIBLE);
                binding.tvTimerHint.setText("已暂停 · 保存后归属 " + dateLabel);
                break;
            case STATE_IDLE:
            default:
                binding.btnTimerStart.setText("开始");
                binding.btnTimerStart.setEnabled(true);
                binding.btnTimerPause.setText("暂停");
                binding.btnTimerPause.setEnabled(false);
                binding.btnTimerStop.setEnabled(false);
                binding.btnTimerDiscard.setVisibility(View.GONE);
                binding.tvTimerHint.setText("未开始 · 保存后归属 " + dateLabel);
                break;
        }
    }

    private void showSaveDialog(int minutes, long elapsedMs) {
        DialogAddStudyBinding d = DialogAddStudyBinding.inflate(LayoutInflater.from(requireContext()));
        d.etDuration.setText(String.valueOf(minutes));
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("保存学习记录（" + minutes + " 分钟）")
                .setView(d.getRoot())
                .setPositiveButton("保存", (dialog, which) -> {
                    String subject = d.etSubject.getText().toString().trim();
                    String content = d.etContent.getText().toString().trim();
                    String durStr = d.etDuration.getText().toString().trim();
                    if (subject.isEmpty()) {
                        Toast.makeText(requireContext(), "请填写科目", Toast.LENGTH_SHORT).show();
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
                                // 保存成功后才清计时状态
                                clearTimerState();
                                loadData();
                                Toast.makeText(requireContext(), "已保存：" + subject, Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("studyTimerInsert", e);
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "保存失败，计时未清除", Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    // 取消保存：计时状态保留，用户可继续编辑或重新点保存
                    Toast.makeText(requireContext(), "已取消保存，计时仍在", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // ==================== 原有的手动添加 / 日期切换 ====================

    private void changeDay(int delta) {
        selectedDate = DateUtils.plusDays(selectedDate, delta);
        loadData();
    }

    private void showDatePicker() {
        DateUtils.DateParts dp = DateUtils.split(selectedDate);
        new DatePickerDialog(requireContext(), (v, y, m, d) -> {
            selectedDate = DateUtils.from(y, m, d);
            loadData();
        }, dp.year, dp.month, dp.day).show();
    }

    private void showAddDialog() {
        DialogAddStudyBinding d = DialogAddStudyBinding.inflate(LayoutInflater.from(requireContext()));
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("添加学习记录")
                .setView(d.getRoot())
                .setPositiveButton("保存", (dialog, which) -> {
                    String subject = d.etSubject.getText().toString().trim();
                    String content = d.etContent.getText().toString().trim();
                    String durStr = d.etDuration.getText().toString().trim();
                    if (subject.isEmpty() || durStr.isEmpty()) {
                        Toast.makeText(requireContext(), "请填写科目与时长", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int duration;
                    try {
                        duration = Integer.parseInt(durStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "时长需为数字", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    StudyRecord record = new StudyRecord(subject, content, duration, selectedDate);
                    new Thread(() -> {
                        try {
                            studyDao.insert(record);
                            requireActivity().runOnUiThread(() -> loadData());
                        } catch (Exception e) {
                            com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("studyInsert", e);
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadData() {
        binding.tvDate.setText(selectedDate + " " + DateUtils.weekdayChinese(selectedDate));
        List<StudyRecord> list = studyDao.getByDate(selectedDate);
        adapter.submit(list);
        binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        // 当日学习总时长
        String[] range = new String[]{selectedDate, selectedDate};
        int total = studyDao.getTotalMinutes(range[0], range[1]);
        binding.tvTotalTime.setText(String.format(Locale.CHINA, "当日学习 %d 分钟", total));

        // 每日学习目标进度
        int dailyGoal = com.example.dailycheck.util.PrefsManager.get(requireContext())
                .getDailyStudyGoal();
        if (dailyGoal > 0) {
            binding.llDailyGoal.setVisibility(View.VISIBLE);
            int progress = Math.min(100, total * 100 / dailyGoal);
            binding.pbDailyGoal.setProgress(progress);
            boolean reached = total >= dailyGoal;
            binding.tvDailyGoal.setText(String.format(Locale.CHINA,
                    "目标 %d 分钟 · 已完成 %d%%%s",
                    dailyGoal, progress, reached ? " · 已达成" : ""));
            binding.tvDailyGoal.setTextColor(reached
                    ? getResources().getColor(R.color.income_green, null)
                    : getResources().getColor(R.color.text_secondary, null));
        } else {
            binding.llDailyGoal.setVisibility(View.GONE);
        }

        // 日期切换后，计时归属提示也要同步刷新
        updateTimerUI();
    }

    /** 通知权限请求回调：授权通过则自动启动计时，拒绝则清除等待状态 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2001 && pendingStartAfterPermission) {
            pendingStartAfterPermission = false;
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                doStartOrResumeTimer();
            } else {
                Toast.makeText(requireContext(),
                        "未授予通知权限，计时将继续运行但不会显示通知",
                        Toast.LENGTH_LONG).show();
                // 即便没有通知权限，计时本身仍然要启动
                doStartOrResumeTimer();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerTick);
        binding = null;
    }
}
