package com.example.dailycheck;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.dailycheck.util.PrefsManager;

/**
 * 应用入口：初始化夜间模式与提醒任务
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 应用保存的夜间模式
        AppCompatDelegate.setDefaultNightMode(PrefsManager.get(this).getNightMode());
        // 初始化提醒
        com.example.dailycheck.notify.ReminderScheduler.schedule(this);
    }
}
