package com.example.dailycheck.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 学习记录实体
 * 记录每天学习的内容、科目与时长
 */
@Entity(tableName = "study_records")
public class StudyRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 科目，如：英语、数学 */
    public String subject;

    /** 学习内容描述 */
    public String content;

    /** 学习时长（分钟） */
    public int durationMinutes;

    /** 归属日期 yyyy-MM-dd */
    public String date;

    /** 创建时间戳 */
    public long createdAt;

    @Ignore
    public StudyRecord(String subject, String content, int durationMinutes, String date) {
        this.subject = subject;
        this.content = content;
        this.durationMinutes = durationMinutes;
        this.date = date;
        this.createdAt = System.currentTimeMillis();
    }

    public StudyRecord() {
    }
}
