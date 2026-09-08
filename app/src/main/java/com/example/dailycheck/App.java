package com.example.dailycheck;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.dailycheck.util.ErrorLogger;
import com.example.dailycheck.util.PrefsManager;

/**
 * 应用入口：初始化夜间模式与提醒任务
 */
public class App extends Application {

    private static final String TAG = "DailyCheckApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // 全局未捕获异常处理器：将崩溃信息写入日志文件，避免用户只看到闪退
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception in " + thread.getName(), throwable);
            try {
                ErrorLogger.get(this).log("FATAL_" + thread.getName(), throwable);
            } catch (Exception ignore) {
            }
            // 继续默认行为（退出应用）
            System.exit(2);
        });

        // 应用保存的夜间模式
        try {
            AppCompatDelegate.setDefaultNightMode(PrefsManager.get(this).getNightMode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to set night mode", e);
            ErrorLogger.get(this).log("nightMode", e);
        }
        // 初始化提醒
        try {
            com.example.dailycheck.notify.ReminderScheduler.schedule(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule reminder", e);
            ErrorLogger.get(this).log("reminderSchedule", e);
        }
    }
}
