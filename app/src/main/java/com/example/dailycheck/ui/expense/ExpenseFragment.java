package com.example.dailycheck.ui.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.dailycheck.R;
import com.example.dailycheck.data.AppDatabase;
import com.example.dailycheck.data.dao.ExpenseRecordDao;
import com.example.dailycheck.data.entity.ExpenseRecord;
import com.example.dailycheck.databinding.DialogAddExpenseBinding;
import com.example.dailycheck.databinding.FragmentExpenseBinding;
import com.example.dailycheck.util.DateUtils;

import java.util.List;
import java.util.Locale;

/**
 * 记账页：记录每日收支，自动计算当日结余
 */
public class ExpenseFragment extends Fragment {

    private FragmentExpenseBinding binding;
    private AppDatabase db;
    private ExpenseRecordDao expenseDao;
    private ExpenseAdapter adapter;
    private String selectedDate;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = AppDatabase.getInstance(requireContext());
        expenseDao = db.expenseRecordDao();
        selectedDate = DateUtils.today();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExpenseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ExpenseAdapter(record -> {
            new Thread(() -> {
                try {
                    expenseDao.delete(record);
                    requireActivity().runOnUiThread(() -> {
                        loadData();
                        Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("expenseDelete", e);
                }
            }).start();
        });
        binding.rvExpense.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExpense.setAdapter(adapter);

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
        DialogAddExpenseBinding d = DialogAddExpenseBinding.inflate(LayoutInflater.from(requireContext()));
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("添加收支记录")
                .setView(d.getRoot())
                .setPositiveButton("保存", (dialog, which) -> {
                    int type = d.rgType.getCheckedRadioButtonId() == R.id.rb_expense
                            ? ExpenseRecord.TYPE_EXPENSE : ExpenseRecord.TYPE_INCOME;
                    String category = d.etCategory.getText().toString().trim();
                    String amountStr = d.etAmount.getText().toString().trim();
                    String note = d.etNote.getText().toString().trim();
                    if (category.isEmpty() || amountStr.isEmpty()) {
                        Toast.makeText(requireContext(), "请填写分类与金额", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount;
                    try {
                        amount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "金额需为数字", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ExpenseRecord record = new ExpenseRecord(type, category, amount, note, selectedDate);
                    new Thread(() -> {
                        try {
                            expenseDao.insert(record);
                            requireActivity().runOnUiThread(() -> loadData());
                        } catch (Exception e) {
                            com.example.dailycheck.util.ErrorLogger.get(requireContext()).log("expenseInsert", e);
                        }
                    }).start();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadData() {
        binding.tvDate.setText(selectedDate + " " + DateUtils.weekdayChinese(selectedDate));
        List<ExpenseRecord> list = expenseDao.getByDate(selectedDate);
        adapter.submit(list);
        binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

        double expense = expenseDao.getTotalExpense(selectedDate, selectedDate);
        double income = expenseDao.getTotalIncome(selectedDate, selectedDate);
        double balance = income - expense;
        binding.tvIncome.setText(String.format(Locale.CHINA, "+%.2f", income));
        binding.tvExpense.setText(String.format(Locale.CHINA, "-%.2f", expense));
        binding.tvBalance.setText(String.format(Locale.CHINA, "%.2f", balance));
        // 结余为负时标红
        int color = balance >= 0
                ? getResources().getColor(R.color.income_green, null)
                : getResources().getColor(R.color.expense_red, null);
        binding.tvBalance.setTextColor(color);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
