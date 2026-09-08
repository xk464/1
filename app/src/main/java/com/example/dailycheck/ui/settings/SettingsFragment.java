package com.example.dailycheck.ui.settings;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dailycheck.R;
import com.example.dailycheck.backup.BackupManager;
import com.example.dailycheck.databinding.FragmentSettingsBinding;
import com.example.dailycheck.net.UdpBroadcastManager;
import com.example.dailycheck.notify.NotifyHelper;
import com.example.dailycheck.notify.ReminderScheduler;
import com.example.dailycheck.template.TemplateStore;
import com.example.dailycheck.ui.settings.TemplateAdapter.Listener;
import com.example.dailycheck.util.DateUtils;
import com.example.dailycheck.util.ErrorLogger;
import com.example.dailycheck.util.PrefsManager;

import java.util.List;

/**
 * 设置页：夜间模式（跟随系统/夜间/日间）、提醒时间、数据导出/导入、
 * 任务模板库、局域网广播、错误日志导出
 */
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private PrefsManager prefs;
    private UdpBroadcastManager udp;
    private TemplateAdapter templateAdapter;

    /** 通知权限申请 */
    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) Toast.makeText(requireContext(), "通知权限已授予", Toast.LENGTH_SHORT).show();
            });

    /** 备份导入文件选择 */
    private final ActivityResultLauncher<String[]> openFileLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) importBackup(uri);
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PrefsManager.get(requireContext());
        udp = new UdpBroadcastManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 夜间模式（三选一对话框）
        binding.rowNight.setOnClickListener(v -> showNightModeDialog());

        // 提醒开关
        binding.swReminder.setOnCheckedChangeListener((b, checked) -> {
            prefs.setReminderEnabled(checked);
            if (checked) {
                ensureNotifPermission();
                NotifyHelper.ensureChannel(requireContext());
                ReminderScheduler.schedule(requireContext());
            } else {
                ReminderScheduler.cancel(requireContext());
            }
        });

        // 提醒时间
        binding.rowReminderTime.setOnClickListener(v -> showTimePicker());

        // 数据导出
        binding.rowExport.setOnClickListener(v -> exportBackup());

        // 数据导入
        binding.rowImport.setOnClickListener(v -> openFileLauncher.launch(new String[]{"application/json", "application/octet-stream", "*/*"}));

        // 错误日志导出
        binding.rowErrorLog.setOnClickListener(v -> exportErrorLog());

        // 任务模板库
        templateAdapter = new TemplateAdapter(new Listener() {
            @Override
            public void onApply(TemplateStore.Template template) {
                applyTemplate(template);
            }
        });
        binding.rvTemplates.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTemplates.setAdapter(templateAdapter);
        templateAdapter.submit(TemplateStore.load(requireContext()));

        // 局域网广播
        binding.btnBroadcast.setOnClickListener(v -> broadcast());
        binding.btnListen.setOnClickListener(v -> startListening());

        refreshUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    private void refreshUi() {
        int mode = prefs.getNightMode();
        binding.tvNightMode.setText(PrefsManager.indexLabel(PrefsManager.modeToIndex(mode)));
        binding.swReminder.setChecked(prefs.isReminderEnabled());
        binding.tvReminderTime.setText(String.format(java.util.Locale.CHINA,
                "%02d:%02d", prefs.getReminderHour(), prefs.getReminderMin()));
    }

    /** 夜间模式三选一对话框 */
    private void showNightModeDialog() {
        String[] labels = {"跟随系统", "始终夜间", "始终日间"};
        int current = PrefsManager.modeToIndex(prefs.getNightMode());
        new AlertDialog.Builder(requireContext())
                .setTitle("夜间模式")
                .setSingleChoiceItems(labels, current, (d, which) -> {
                    prefs.setNightMode(PrefsManager.indexToMode(which));
                    AppCompatDelegate.setDefaultNightMode(prefs.getNightMode());
                    refreshUi();
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showTimePicker() {
        new TimePickerDialog(requireContext(), (TimePicker v, int h, int m) -> {
            prefs.setReminderTime(h, m);
            if (prefs.isReminderEnabled()) {
                ReminderScheduler.schedule(requireContext());
            }
            refreshUi();
            Toast.makeText(requireContext(), "提醒时间已更新", Toast.LENGTH_SHORT).show();
        }, prefs.getReminderHour(), prefs.getReminderMin(), true).show();
    }

    private void ensureNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    // ---- 备份 ----

    private void exportBackup() {
        new Thread(() -> {
            try {
                Uri uri = new BackupManager(requireContext()).export();
                requireActivity().runOnUiThread(() -> shareFile(uri));
            } catch (Exception e) {
                ErrorLogger.get(requireContext()).log("exportBackup", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "导出失败：" + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void shareFile(Uri uri) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/json");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "分享或保存备份文件"));
    }

    private void importBackup(Uri uri) {
        new Thread(() -> {
            try (java.io.InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                int[] counts = new BackupManager(requireContext()).importFromStream(in);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(),
                            String.format(java.util.Locale.CHINA,
                                    "导入完成：任务%d / 学习%d / 收支%d",
                                    counts[0], counts[1], counts[2]),
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                ErrorLogger.get(requireContext()).log("importBackup", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "导入失败：" + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ---- 错误日志 ----

    private void exportErrorLog() {
        new Thread(() -> {
            try {
                Uri uri = ErrorLogger.get(requireContext()).export();
                if (uri == null) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "暂无错误日志",
                                    Toast.LENGTH_SHORT).show());
                    return;
                }
                requireActivity().runOnUiThread(() -> shareFile(uri));
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "日志导出失败：" + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ---- 模板库 ----

    private void applyTemplate(TemplateStore.Template template) {
        String today = DateUtils.today();
        new AlertDialog.Builder(requireContext())
                .setTitle("导入模板")
                .setMessage("将「" + template.name + "」的 " + template.tasks.size()
                        + " 个任务添加到今天？")
                .setPositiveButton("导入", (d, w) -> {
                    new Thread(() -> {
                        try {
                            com.example.dailycheck.data.AppDatabase db =
                                    com.example.dailycheck.data.AppDatabase.getInstance(requireContext());
                            for (String title : template.tasks) {
                                db.taskDao().insert(new com.example.dailycheck.data.entity.Task(title, today));
                            }
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(),
                                            "已导入 " + template.tasks.size() + " 个任务",
                                            Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            ErrorLogger.get(requireContext()).log("applyTemplate", e);
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "导入失败：" + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ---- 局域网广播 ----

    private void broadcast() {
        new Thread(() -> {
            try {
                int n = udp.broadcastToday();
                String today = DateUtils.today();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                n > 0 ? "已广播 " + today + " 的 " + n + " 个任务到局域网"
                                        : "今天暂无任务可广播",
                                Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                ErrorLogger.get(requireContext()).log("broadcast", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "广播失败：" + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void startListening() {
        binding.tvListenStatus.setText("正在监听局域网广播...");
        binding.tvListenStatus.setVisibility(View.VISIBLE);
        udp.startListening((date, titles) -> requireActivity().runOnUiThread(() -> {
            binding.tvListenStatus.setVisibility(View.GONE);
            showReceivedTasks(date, titles);
        }));
        Toast.makeText(requireContext(), "已开启监听，约5秒内收到将弹窗提示",
                Toast.LENGTH_SHORT).show();
    }

    private void showReceivedTasks(String date, List<String> titles) {
        StringBuilder sb = new StringBuilder();
        sb.append("收到 ").append(date).append(" 的任务清单：\n");
        for (int i = 0; i < titles.size(); i++) {
            sb.append(i + 1).append(". ").append(titles.get(i)).append("\n");
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("收到局域网广播")
                .setMessage(sb.toString())
                .setPositiveButton("导入今天", (d, w) -> {
                    String today = DateUtils.today();
                    new Thread(() -> {
                        try {
                            com.example.dailycheck.data.AppDatabase db =
                                    com.example.dailycheck.data.AppDatabase.getInstance(requireContext());
                            for (String title : titles) {
                                db.taskDao().insert(
                                        new com.example.dailycheck.data.entity.Task(title, today));
                            }
                        } catch (Exception e) {
                            ErrorLogger.get(requireContext()).log("importReceivedTasks", e);
                        }
                    }).start();
                    Toast.makeText(requireContext(),
                            "已导入 " + titles.size() + " 个任务到今天",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("忽略", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 停止局域网监听，避免 Fragment 销毁后线程仍持有引用
        udp.stop();
        binding = null;
    }
}
