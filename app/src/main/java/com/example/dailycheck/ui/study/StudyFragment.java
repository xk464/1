package com.example.dailycheck.ui.study;

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
import com.example.dailycheck.data.dao.StudyRecordDao;
import com.example.dailycheck.data.entity.StudyRecord;
import com.example.dailycheck.databinding.DialogAddStudyBinding;
import com.example.dailycheck.databinding.FragmentStudyBinding;
import com.example.dailycheck.util.DateUtils;

import java.util.List;
import java.util.Locale;

/**
 * 学习页：按日期记录学习内容、科目、时长，并汇总当日学习时长
 */
public class StudyFragment extends Fragment {

    private FragmentStudyBinding binding;
    private AppDatabase db;
    private StudyRecordDao studyDao;
    private StudyAdapter adapter;
    private String selectedDate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        studyDao = db.studyRecordDao();
        selectedDate = DateUtils.today();
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
        binding = null;
    }
}
