package com.example.dailycheck.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 任务实体 - 每日打卡任务
 * 一条记录 = 某一天的某一个任务
 */
@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 任务标题 */
    public String title;

    /** 归属日期 yyyy-MM-dd */
    public String date;

    /** 是否已完成 */
    public boolean isCompleted;

    /** 创建时间戳 */
    public long createdAt;

    @Ignore
    public Task(String title, String date) {
        this.title = title;
        this.date = date;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
    }

    /** Room 需要空构造函数（用于反序列化） */
    public Task() {
    }
}
