package com.example.dailycheck.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.dailycheck.data.entity.Task;

import java.util.List;

/**
 * 任务数据访问对象
 */
@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    /** 获取指定日期的全部任务 */
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY isCompleted ASC, createdAt ASC")
    List<Task> getTasksByDate(String date);

    /** 切换任务完成状态 */
    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    void setCompleted(long id, boolean completed);

    /** 获取指定日期已完成的任务数 */
    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date AND isCompleted = 1")
    int getCompletedCount(String date);

    /** 获取指定日期任务总数 */
    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date")
    int getTotalCount(String date);

    /** 获取日期区间内已完成任务数 */
    @Query("SELECT COUNT(*) FROM tasks WHERE date BETWEEN :start AND :end AND isCompleted = 1")
    int getCompletedCountRange(String start, String end);

    /** 获取日期区间内任务总数 */
    @Query("SELECT COUNT(*) FROM tasks WHERE date BETWEEN :start AND :end")
    int getTotalCountRange(String start, String end);

    /** 获取指定日期范围内所有有打卡记录的日期（去重） */
    @Query("SELECT DISTINCT date FROM tasks ORDER BY date ASC")
    List<String> getAllTaskDates();

    /** 获取指定日期范围内已完成任务的日期（至少有一项完成） */
    @Query("SELECT DISTINCT date FROM tasks WHERE isCompleted = 1 ORDER BY date DESC")
    List<String> getCompletedDates();

    /** 获取日期区间内每日任务统计（按日期分组） */
    @Query("SELECT date, COUNT(*) AS total, " +
            "SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) AS done " +
            "FROM tasks WHERE date BETWEEN :start AND :end GROUP BY date")
    List<DayStat> getDayStats(String start, String end);

    /** 每日任务统计结果 */
    class DayStat {
        public String date;
        public int total;
        public int done;
    }
}
