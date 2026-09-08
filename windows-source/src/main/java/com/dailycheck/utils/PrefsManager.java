package com.dailycheck.utils;

import java.util.prefs.Preferences;

/**
 * 偏好设置管理（桌面端）
 * 使用 java.util.prefs.Preferences 替代 Android SharedPreferences，
 * 保存：每日学习目标、每日消费上限、提醒开关与提醒时间。
 * 与 Android 端 PrefsManager 对应的字段名保持一致以便理解。
 */
public class PrefsManager {

    private static final String KEY_DAILY_STUDY_GOAL = "daily_study_goal_min"; // 每日学习目标（分钟），0=未设置
    private static final String KEY_DAILY_EXPENSE_LIMIT = "daily_expense_limit"; // 每日消费上限（元），0=未设置
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MIN = "reminder_min";

    private static volatile PrefsManager INSTANCE;
    private final Preferences prefs;

    private PrefsManager() {
        // 节点路径对应 Android 端的 "daily_check_prefs"
        prefs = Preferences.userRoot().node("daily_check_prefs");
    }

    public static PrefsManager get() {
        if (INSTANCE == null) {
            synchronized (PrefsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PrefsManager();
                }
            }
        }
        return INSTANCE;
    }

    // ==================== 每日目标 ====================

    /** 每日学习目标（分钟），0 表示未设置 */
    public int getDailyStudyGoal() {
        return prefs.getInt(KEY_DAILY_STUDY_GOAL, 0);
    }

    public void setDailyStudyGoal(int minutes) {
        prefs.putInt(KEY_DAILY_STUDY_GOAL, Math.max(0, minutes));
    }

    /** 每日消费上限（元），0 表示未设置 */
    public double getDailyExpenseLimit() {
        return prefs.getDouble(KEY_DAILY_EXPENSE_LIMIT, 0d);
    }

    public void setDailyExpenseLimit(double limit) {
        prefs.putDouble(KEY_DAILY_EXPENSE_LIMIT, Math.max(0d, limit));
    }

    // ==================== 提醒（仅保存配置，桌面端不强制启用） ====================

    public boolean isReminderEnabled() {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, true);
    }

    public void setReminderEnabled(boolean enabled) {
        prefs.putBoolean(KEY_REMINDER_ENABLED, enabled);
    }

    public int getReminderHour() {
        return prefs.getInt(KEY_REMINDER_HOUR, 21);
    }

    public int getReminderMin() {
        return prefs.getInt(KEY_REMINDER_MIN, 0);
    }

    public void setReminderTime(int hour, int min) {
        prefs.putInt(KEY_REMINDER_HOUR, hour);
        prefs.putInt(KEY_REMINDER_MIN, min);
    }
}
