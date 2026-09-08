package com.example.dailycheck.notify;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * 每日提醒 Worker：到点触发一条打卡通知
 */
public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotifyHelper.showReminder(getApplicationContext());
        return Result.success();
    }
}
