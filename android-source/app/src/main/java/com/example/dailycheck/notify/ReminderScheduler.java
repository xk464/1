package com.example.dailycheck.notify;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * 提醒调度：使用 WorkManager 每日在指定时间触发提醒
 * 由于 PeriodicWorkRequest 最短周期为 15 分钟，采用「下次目标时刻」作为初始延迟
 */
public final class ReminderScheduler {

    public static final String WORK_NAME = "daily_check_reminder";

    private ReminderScheduler() {
    }

    /** 计划或更新每日提醒任务 */
    public static void schedule(Context context) {
        com.example.dailycheck.util.PrefsManager prefs = com.example.dailycheck.util.PrefsManager.get(context);
        if (!prefs.isReminderEnabled()) {
            cancel(context);
            return;
        }
        int hour = prefs.getReminderHour();
        int min = prefs.getReminderMin();

        long now = System.currentTimeMillis();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, min);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        if (target.getTimeInMillis() <= now) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        long initialDelay = target.getTimeInMillis() - now;

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                1, TimeUnit.DAYS
        )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
        );
    }

    /** 取消提醒 */
    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
    }
}
