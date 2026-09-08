package com.dailycheck.models;

/**
 * 任务实体 - 每日打卡任务
 * 一条记录 = 某一天的某一个任务
 * 与 Android Room 的 Task 实体保持一致的字段与表结构。
 */
public class Task {

    /** 主键，自增 */
    public long id;

    /** 任务标题 */
    public String title;

    /** 归属日期 yyyy-MM-dd */
    public String date;

    /** 是否已完成 */
    public boolean isCompleted;

    /** 创建时间戳 */
    public long createdAt;

    public Task() {
        // 空构造，用于反序列化
    }

    public Task(String title, String date) {
        this.title = title;
        this.date = date;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
    }

    public Task(long id, String title, String date, boolean isCompleted, long createdAt) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }
}
