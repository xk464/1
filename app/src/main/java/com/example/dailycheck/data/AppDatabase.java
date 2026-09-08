package com.example.dailycheck.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.dailycheck.data.dao.ExpenseRecordDao;
import com.example.dailycheck.data.dao.StudyRecordDao;
import com.example.dailycheck.data.dao.TaskDao;
import com.example.dailycheck.data.entity.ExpenseRecord;
import com.example.dailycheck.data.entity.StudyRecord;
import com.example.dailycheck.data.entity.Task;

/**
 * 应用数据库（Room）单例
 */
@Database(
        entities = {Task.class, StudyRecord.class, ExpenseRecord.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TaskDao taskDao();

    public abstract StudyRecordDao studyRecordDao();

    public abstract ExpenseRecordDao expenseRecordDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "daily_check.db"
                            )
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries() // 简单示例：允许主线程查询；生产环境建议使用后台线程
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
