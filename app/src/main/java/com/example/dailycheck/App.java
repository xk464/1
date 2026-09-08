package com.example.dailycheck;

import android.app.Application;
import android.util.Log;

import com.example.dailycheck.notify.ReminderScheduler;

/**
 * 应用入口：初始化提醒任务
 */
public class App extends Application {

    private static final String TAG = "DailyCheckApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App onCreate");
        // 初始化提醒（失败不影响启动）
        try {
            ReminderScheduler.schedule(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule reminder", e);
        }
    }
}
