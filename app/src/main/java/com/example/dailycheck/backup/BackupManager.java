package com.example.dailycheck.backup;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.dailycheck.data.AppDatabase;
import com.example.dailycheck.data.entity.ExpenseRecord;
import com.example.dailycheck.data.entity.StudyRecord;
import com.example.dailycheck.data.entity.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 备份管理：将三张表导出为 JSON，并支持从 JSON 恢复
 * JSON 结构：{ meta:{...}, tasks:[...], studyRecords:[...], expenseRecords:[...] }
 */
public class BackupManager {

    private final Context context;
    private final AppDatabase db;

    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(this.context);
    }

    /** 导出全部数据，返回可分享的 FileProvider Uri */
    public Uri export() throws JSONException {
        JSONObject root = new JSONObject();
        JSONObject meta = new JSONObject();
        meta.put("app", "com.example.dailycheck");
        meta.put("exportedAt", System.currentTimeMillis());
        meta.put("version", 1);
        root.put("meta", meta);

        // 任务：按已知日期逐日取数
        List<String> allDates = db.taskDao().getAllTaskDates();
        JSONArray taskArr = new JSONArray();
        for (String date : allDates) {
            for (Task t : db.taskDao().getTasksByDate(date)) {
                taskArr.put(taskToJson(t));
            }
        }
        root.put("tasks", taskArr);

        // 学习记录：按日期范围取全部
        String far = "1970-01-01";
        String future = "2100-01-01";
        JSONArray studyArr = new JSONArray();
        for (StudyRecord s : db.studyRecordDao().getByDateRange(far, future)) {
            studyArr.put(studyToJson(s));
        }
        root.put("studyRecords", studyArr);

        // 收支记录
        JSONArray expArr = new JSONArray();
        for (ExpenseRecord e : db.expenseRecordDao().getByDateRange(far, future)) {
            expArr.put(expenseToJson(e));
        }
        root.put("expenseRecords", expArr);

        String json = root.toString(2);
        String fileName = "daily_check_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date()) + ".json";
        File dir = new File(context.getFilesDir(), "backups");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
    }

    /** 从输入流读取 JSON 并恢复数据，返回各表插入条数 */
    public int[] importFromStream(InputStream in) throws JSONException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JSONObject root = new JSONObject(sb.toString());
        int tCount = 0, sCount = 0, eCount = 0;

        JSONArray taskArr = root.optJSONArray("tasks");
        if (taskArr != null) {
            for (int i = 0; i < taskArr.length(); i++) {
                Task t = jsonToTask(taskArr.getJSONObject(i));
                db.taskDao().insert(t);
                tCount++;
            }
        }
        JSONArray studyArr = root.optJSONArray("studyRecords");
        if (studyArr != null) {
            for (int i = 0; i < studyArr.length(); i++) {
                StudyRecord s = jsonToStudy(studyArr.getJSONObject(i));
                db.studyRecordDao().insert(s);
                sCount++;
            }
        }
        JSONArray expArr = root.optJSONArray("expenseRecords");
        if (expArr != null) {
            for (int i = 0; i < expArr.length(); i++) {
                ExpenseRecord e = jsonToExpense(expArr.getJSONObject(i));
                db.expenseRecordDao().insert(e);
                eCount++;
            }
        }
        return new int[]{tCount, sCount, eCount};
    }

    // ---- 序列化 ----

    private JSONObject taskToJson(Task t) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("title", t.title);
        o.put("date", t.date);
        o.put("isCompleted", t.isCompleted);
        o.put("createdAt", t.createdAt);
        return o;
    }

    private Task jsonToTask(JSONObject o) throws JSONException {
        Task t = new Task();
        t.title = o.getString("title");
        t.date = o.getString("date");
        t.isCompleted = o.optBoolean("isCompleted", false);
        t.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        return t;
    }

    private JSONObject studyToJson(StudyRecord s) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("subject", s.subject);
        o.put("content", s.content);
        o.put("durationMinutes", s.durationMinutes);
        o.put("date", s.date);
        o.put("createdAt", s.createdAt);
        return o;
    }

    private StudyRecord jsonToStudy(JSONObject o) throws JSONException {
        StudyRecord s = new StudyRecord();
        s.subject = o.getString("subject");
        s.content = o.optString("content", "");
        s.durationMinutes = o.optInt("durationMinutes", 0);
        s.date = o.getString("date");
        s.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        return s;
    }

    private JSONObject expenseToJson(ExpenseRecord e) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("type", e.type);
        o.put("category", e.category);
        o.put("amount", e.amount);
        o.put("note", e.note);
        o.put("date", e.date);
        o.put("createdAt", e.createdAt);
        return o;
    }

    private ExpenseRecord jsonToExpense(JSONObject o) throws JSONException {
        ExpenseRecord e = new ExpenseRecord();
        e.type = o.optInt("type", 0);
        e.category = o.getString("category");
        e.amount = o.optDouble("amount", 0);
        e.note = o.optString("note", "");
        e.date = o.getString("date");
        e.createdAt = o.optLong("createdAt", System.currentTimeMillis());
        return e;
    }
}
