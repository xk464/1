package com.example.dailycheck.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.dailycheck.data.entity.ExpenseRecord;

import java.util.List;

/**
 * 收支记录数据访问对象
 */
@Dao
public interface ExpenseRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ExpenseRecord record);

    @Delete
    void delete(ExpenseRecord record);

    /** 获取指定日期的收支记录 */
    @Query("SELECT * FROM expense_records WHERE date = :date ORDER BY createdAt DESC")
    List<ExpenseRecord> getByDate(String date);

    /** 获取日期范围内的收支记录 */
    @Query("SELECT * FROM expense_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt DESC")
    List<ExpenseRecord> getByDateRange(String startDate, String endDate);

    /** 统计日期范围内支出总额 */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expense_records WHERE type = 0 AND date BETWEEN :startDate AND :endDate")
    double getTotalExpense(String startDate, String endDate);

    /** 统计日期范围内收入总额 */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expense_records WHERE type = 1 AND date BETWEEN :startDate AND :endDate")
    double getTotalIncome(String startDate, String endDate);

    /** 按分类统计支出 */
    @Query("SELECT category, COALESCE(SUM(amount), 0) AS total FROM expense_records WHERE type = 0 AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    List<CategoryTotal> getExpenseByCategory(String startDate, String endDate);

    /** 按分类聚合的金额结果 */
    class CategoryTotal {
        public String category;
        public double total;
    }

    /** 按日期分组统计支出 */
    @Query("SELECT date, COALESCE(SUM(amount), 0) AS total FROM expense_records WHERE type = 0 AND date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date ASC")
    List<DailyTotal> getDailyExpense(String startDate, String endDate);

    /** 每日支出统计结果 */
    class DailyTotal {
        public String date;
        public double total;
    }
}
