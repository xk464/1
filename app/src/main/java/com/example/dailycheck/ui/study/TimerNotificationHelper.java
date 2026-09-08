package com.example.dailycheck.ui.study;

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
import com.example.dailycheck.notify.NotifyHelper;

import java.util.Locale;

/**
 * 学习计时通知：运行时在通知栏显示实时累计时间，点击回到应用
 */
public final class TimerNotificationHelper {

    public static final String CHANNEL_ID = "study_timer";
    public static final int NOTIF_ID = 2001;

    private TimerNotificationHelper() {}

    /** 创建计时通知渠道（常驻通知，低优先级避免打扰） */
    public static void ensureChannel(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "学习计时",
                NotificationManager.IMPORTANCE_LOW   // 低优先级，不弹窗、不震动
        );
        channel.setDescription("学习计时器运行时在通知栏显示累计时长");
        nm.createNotificationChannel(channel);
    }

    /** 显示/更新通知 */
    public static void show(Context context, long elapsedMs, boolean running) {
        ensureChannel(context);

        // 点击通知回到主界面（暂不精确跳转到学习 Tab，后续可扩展）
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getActivity(context, NOTIF_ID, intent, flags);

        String timeText = formatMs(elapsedMs);
        String title = running ? "学习计时中" : "学习计时（已暂停）";
        int color = running ? 0xFF5B6CFF : 0xFF9A9A9A; // primary / gray

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_study)
                .setContentTitle(title)
                .setContentText(timeText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("已累计学习 " + timeText))
                .setOngoing(true)                    // 常驻通知，不可手动划掉
                .setContentIntent(pi)
                .setOnlyAlertOnce(true)              // 只在首次显示时提示，后续更新不再发声
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(color);

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, b.build());
        } catch (SecurityException ignore) {
            // 没有通知权限则静默
        }
    }

    /** 取消通知 */
    public static void cancel(Context context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID);
    }

    private static String formatMs(long ms) {
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) {
            return String.format(Locale.CHINA, "%d 小时 %02d 分 %02d 秒", h, m, s);
        } else if (m > 0) {
            return String.format(Locale.CHINA, "%d 分 %02d 秒", m, s);
        } else {
            return String.format(Locale.CHINA, "%d 秒", s);
        }
    }
}
