package com.example.dailycheck.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * 收支记录实体
 * type=0 表示支出，type=1 表示收入
 * 每日结余 = 当日收入总额 - 当日支出总额
 */
@Entity(tableName = "expense_records")
public class ExpenseRecord {

    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** 类型：0=支出，1=收入 */
    public int type;

    /** 分类，如：餐饮、交通、工资 */
    public String category;

    /** 金额 */
    public double amount;

    /** 备注 */
    public String note;

    /** 归属日期 yyyy-MM-dd */
    public String date;

    /** 创建时间戳 */
    public long createdAt;

    @Ignore
    public ExpenseRecord(int type, String category, double amount, String note, String date) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.note = note;
        this.date = date;
        this.createdAt = System.currentTimeMillis();
    }

    public ExpenseRecord() {
    }
}
