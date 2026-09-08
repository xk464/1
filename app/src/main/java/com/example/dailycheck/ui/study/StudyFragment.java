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
    private static final String KEY_ACCUM_MS = "accum_ms";    // 已累计毫秒数（暂停时使用）

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
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            updateTimerDisplay();
            // 每 1 秒更新一次通知（不要每 500ms，节省电量）
            long ms = getElapsedMs();
            if (ms % 1000 < 600) {
                TimerNotificationHelper.show(requireContext(), ms, true);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2001);
            }
        }
        int state = timerSP.getInt(KEY_STATE, STATE_IDLE);
        SharedPreferences.Editor ed = timerSP.edit();
        long now = System.currentTimeMillis();

        if (state == STATE_IDLE) {
            ed.putLong(KEY_START_TIME, now);
            ed.putLong(KEY_ACCUM_MS, 0L);
        } else if (state == STATE_PAUSED) {
            long accum = timerSP.getLong(KEY_ACCUM_MS, 0L);
            ed.putLong(KEY_START_TIME, now);
            ed.putLong(KEY_ACCUM_MS, accum);
        }
        ed.putInt(KEY_STATE, STATE_RUNNING).apply();
        updateTimerUI();
        timerHandler.post(timerTick);
        TimerNotificationHelper.show(requireContext(), getElapsedMs(), true);
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
        TimerNotificationHelper.show(requireContext(), accum, false);
    }

    private void stopTimerAndSave() {
        long elapsedMs = getElapsedMs();
        timerSP.edit().clear().apply();
        timerHandler.removeCallbacks(timerTick);
        TimerNotificationHelper.cancel(requireContext());
        updateTimerDisplay();
        updateTimerUI();

        if (elapsedMs < 1000) {
            Toast.makeText(requireContext(), "计时太短，未保存", Toast.LENGTH_SHORT).show();
            return;
        }
        int minutes = (int) ((elapsedMs + 59999) / 60000);
        showSaveDialog(minutes);
    }

    /** 应用/界面重新打开时恢复计时状态 */
    private void restoreTimerState() {
        int state = timerSP.getInt(KEY_STATE, STATE_IDLE);
        updateTimerDisplay();
        updateTimerUI();
        if (state == STATE_RUNNING) {
            timerHandler.post(timerTick);
            TimerNotificationHelper.show(requireContext(), getElapsedMs(), true);
        } else if (state == STATE_PAUSED) {
            TimerNotificationHelper.show(requireContext(), getElapsedMs(), false);
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

    /** 根据计时状态更新三个按钮的启用/禁用和文字 */
    private void updateTimerUI() {
        int state = timerSP.getInt(KEY_STATE, STATE_IDLE);
        switch (state) {
            case STATE_RUNNING:
                binding.btnTimerStart.setText("运行中");
                binding.btnTimerStart.setEnabled(false);
                binding.btnTimerPause.setText("暂停");
                binding.btnTimerPause.setEnabled(true);
                binding.btnTimerStop.setEnabled(true);
                break;
            case STATE_PAUSED:
                binding.btnTimerStart.setText("继续");
                binding.btnTimerStart.setEnabled(true);
                binding.btnTimerPause.setText("暂停");
                binding.btnTimerPause.setEnabled(false);
                binding.btnTimerStop.setEnabled(true);
                break;
            case STATE_IDLE:
            default:
                binding.btnTimerStart.setText("开始");
                binding.btnTimerStart.setEnabled(true);
                binding.btnTimerPause.setText("暂停");
                binding.btnTimerPause.setEnabled(false);
                binding.btnTimerStop.setEnabled(false);
                break;
        }
    }

    private void showSaveDialog(int minutes) {
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
                                loadData();
                                Toast.makeText(requireContext(), "已保存：" + subject, Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("studyTimerInsert", e);
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerTick);
        binding = null;
    }
}
