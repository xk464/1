package com.example.dailycheck.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.dailycheck.data.entity.StudyRecord;

import java.util.List;

/**
 * 学习记录数据访问对象
 */
@Dao
public interface StudyRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StudyRecord record);

    @Delete
    void delete(StudyRecord record);

    /** 获取指定日期的全部学习记录 */
    @Query("SELECT * FROM study_records WHERE date = :date ORDER BY createdAt DESC")
    List<StudyRecord> getByDate(String date);

    /** 获取日期范围内的学习记录 */
    @Query("SELECT * FROM study_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt DESC")
    List<StudyRecord> getByDateRange(String startDate, String endDate);

    /** 统计日期范围内的学习总时长（分钟） */
    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM study_records WHERE date BETWEEN :startDate AND :endDate")
    int getTotalMinutes(String startDate, String endDate);

    /** 按科目统计时长 */
    @Query("SELECT subject, COALESCE(SUM(durationMinutes), 0) AS total FROM study_records WHERE date BETWEEN :startDate AND :endDate GROUP BY subject ORDER BY total DESC")
    List<SubjectTotal> getMinutesBySubject(String startDate, String endDate);

    /** 按科目聚合的时长结果 */
    class SubjectTotal {
        public String subject;
        public int total;
    }

    /** 按日期分组统计学习时长 */
    @Query("SELECT date, COALESCE(SUM(durationMinutes), 0) AS total FROM study_records WHERE date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date ASC")
    List<DailyTotal> getDailyTotals(String startDate, String endDate);

    /** 每日学习时长统计结果 */
    class DailyTotal {
        public String date;
        public int total;
    }
}
