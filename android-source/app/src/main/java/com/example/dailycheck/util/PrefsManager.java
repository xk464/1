package com.example.dailycheck.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * 偏好设置管理（夜间模式、提醒时间、提醒开关）
 * 夜间模式有三种选择：跟随系统 / 始终夜间 / 始终日间
 */
public class PrefsManager {

    private static final String PREFS = "daily_check_prefs";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MIN = "reminder_min";
    private static final String KEY_DAILY_STUDY_GOAL = "daily_study_goal_min"; // 每日学习目标（分钟），0=未设置
    private static final String KEY_DAILY_EXPENSE_LIMIT = "daily_expense_limit"; // 每日消费上限（元），0=未设置

    /** 夜间模式可选项的稳定索引，便于在设置页三选一对话框中定位 */
    public static final int NIGHT_INDEX_SYSTEM = 0;
    public static final int NIGHT_INDEX_DARK = 1;
    public static final int NIGHT_INDEX_LIGHT = 2;

    private static volatile PrefsManager INSTANCE;
    private final SharedPreferences sp;

    private PrefsManager(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static PrefsManager get(Context context) {
        if (INSTANCE == null) {
            synchronized (PrefsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PrefsManager(context);
                }
            }
        }
        return INSTANCE;
    }

    /** 夜间模式：AppCompatDelegate.MODE_NIGHT_* 之一 */
    public int getNightMode() {
        return sp.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setNightMode(int mode) {
        sp.edit().putInt(KEY_NIGHT_MODE, mode).apply();
    }

    /** 由索引转 AppCompatDelegate 模式常量 */
    public static int indexToMode(int index) {
        switch (index) {
            case NIGHT_INDEX_DARK: return AppCompatDelegate.MODE_NIGHT_YES;
            case NIGHT_INDEX_LIGHT: return AppCompatDelegate.MODE_NIGHT_NO;
            case NIGHT_INDEX_SYSTEM:
            default: return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    /** 由 AppCompatDelegate 模式常量转索引 */
    public static int modeToIndex(int mode) {
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_YES: return NIGHT_INDEX_DARK;
            case AppCompatDelegate.MODE_NIGHT_NO: return NIGHT_INDEX_LIGHT;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
            default: return NIGHT_INDEX_SYSTEM;
        }
    }

    /** 用于显示当前选择的本地化文案 */
    public static String indexLabel(int index) {
        switch (index) {
            case NIGHT_INDEX_DARK: return "始终夜间";
            case NIGHT_INDEX_LIGHT: return "始终日间";
            case NIGHT_INDEX_SYSTEM:
            default: return "跟随系统";
        }
    }

    public boolean isReminderEnabled() {
        return sp.getBoolean(KEY_REMINDER_ENABLED, true);
    }

    public void setReminderEnabled(boolean enabled) {
        sp.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    public int getReminderHour() {
        return sp.getInt(KEY_REMINDER_HOUR, 21);
    }

    public int getReminderMin() {
        return sp.getInt(KEY_REMINDER_MIN, 0);
    }

    public void setReminderTime(int hour, int min) {
        sp.edit().putInt(KEY_REMINDER_HOUR, hour)
                .putInt(KEY_REMINDER_MIN, min)
                .apply();
    }

    // ==================== 每日目标 ====================

    /** 每日学习目标（分钟），0 表示未设置 */
    public int getDailyStudyGoal() {
        return sp.getInt(KEY_DAILY_STUDY_GOAL, 0);
    }

    public void setDailyStudyGoal(int minutes) {
        sp.edit().putInt(KEY_DAILY_STUDY_GOAL, Math.max(0, minutes)).apply();
    }

    /** 每日消费上限（元），0 表示未设置 */
    public float getDailyExpenseLimit() {
        return sp.getFloat(KEY_DAILY_EXPENSE_LIMIT, 0f);
    }

    public void setDailyExpenseLimit(float limit) {
        sp.edit().putFloat(KEY_DAILY_EXPENSE_LIMIT, Math.max(0f, limit)).apply();
    }
}
