package com.example.dailycheck;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.example.dailycheck.notify.ReminderScheduler;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 应用入口：初始化提醒任务，捕获全局异常并展示
 */
public class App extends Application {

    private static final String TAG = "DailyCheckApp";
    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.d(TAG, "App onCreate");

        // 全局异常捕获：将崩溃信息展示给用户，便于定位问题
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "Uncaught exception", throwable);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String crashInfo = "Thread: " + thread.getName() + "\n\n" + sw.toString();

            Intent intent = new Intent(this, CrashDisplayActivity.class);
            intent.putExtra(CrashDisplayActivity.EXTRA_CRASH_INFO, crashInfo);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // 杀掉当前进程，避免状态不一致
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });

        // 初始化提醒（失败不影响启动）
        try {
            ReminderScheduler.schedule(this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule reminder", e);
        }
    }

    public static App getInstance() {
        return instance;
    }
}
