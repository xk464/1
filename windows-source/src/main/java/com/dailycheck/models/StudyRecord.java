package com.dailycheck.models;

/**
 * 学习记录实体
 * 记录每天学习的内容、科目与时长
 * 与 Android Room 的 StudyRecord 实体保持一致的字段与表结构。
 */
public class StudyRecord {

    /** 主键，自增 */
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

    public StudyRecord() {
        // 空构造，用于反序列化
    }

    public StudyRecord(String subject, String content, int durationMinutes, String date) {
        this.subject = subject;
        this.content = content;
        this.durationMinutes = durationMinutes;
        this.date = date;
        this.createdAt = System.currentTimeMillis();
    }

    public StudyRecord(long id, String subject, String content, int durationMinutes, String date, long createdAt) {
        this.id = id;
        this.subject = subject;
        this.content = content;
        this.durationMinutes = durationMinutes;
        this.date = date;
        this.createdAt = createdAt;
    }
}
