package com.example.dailycheck.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.dailycheck.MainActivity;
import com.example.dailycheck.R;

/**
 * 通知工具：渠道创建与提醒通知发送
 * 通知点击后跳转到打卡页（MainActivity 解析 extra 自动切换 Tab）
 */
public class NotifyHelper {

    public static final String CHANNEL_ID = "daily_reminder";
    public static final String EXTRA_OPEN_TAB = "extra_open_tab";
    public static final int TAB_TASKS = 0;
    private static final int NOTIF_ID = 1001;

    /** 创建提醒通知渠道（需在发送通知前调用） */
    public static void ensureChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "每日打卡提醒",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("每日固定时间提醒你完成打卡任务");
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(channel);
        }
    }

    /** 弹出打卡提醒通知 */
    public static void showReminder(Context context) {
        ensureChannel(context);
        // 点击跳转到打卡 Tab
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_OPEN_TAB, TAB_TASKS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, flags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_check)
                .setContentTitle("每日打卡")
                .setContentText("今天还没完成打卡，快来记录任务与学习吧")
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, b.build());
        } catch (SecurityException ignore) {
            // 缺少通知权限时静默忽略
        }
    }
}
