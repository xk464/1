package com.example.dailycheck.util;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 错误日志记录与导出
 * 所有报错都通过 log() 写入本地文件，用户可在设置页一键导出
 */
public class ErrorLogger {

    private static final String LOG_FILE = "error_log.txt";
    private static volatile ErrorLogger INSTANCE;
    private final Context context;

    private ErrorLogger(Context context) {
        this.context = context.getApplicationContext();
    }

    public static ErrorLogger get(Context context) {
        if (INSTANCE == null) {
            synchronized (ErrorLogger.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ErrorLogger(context);
                }
            }
        }
        return INSTANCE;
    }

    /** 记录一条错误（自动追加到文件） */
    public synchronized void log(String tag, Throwable t) {
        File file = new File(context.getFilesDir(), LOG_FILE);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
            pw.println("==== " + now() + "  tag=" + tag + " ====");
            if (t != null) {
                t.printStackTrace(pw);
            } else {
                pw.println("(no throwable)");
            }
            pw.println();
        } catch (Exception ignore) {
            // 自身出错时静默
        }
    }

    /** 通用错误记录（无 tag） */
    public void log(Throwable t) {
        log("app", t);
    }

    /** 导出错误日志，返回可分享 Uri；无日志时返回 null */
    public synchronized Uri export() {
        File file = new File(context.getFilesDir(), LOG_FILE);
        if (!file.exists() || file.length() == 0) return null;
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
    }

    /** 清空错误日志 */
    public synchronized void clear() {
        File file = new File(context.getFilesDir(), LOG_FILE);
        if (file.exists()) file.delete();
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
    }
}
