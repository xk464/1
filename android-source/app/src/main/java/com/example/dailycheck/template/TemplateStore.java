package com.example.dailycheck.template;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务模板库：从 assets/task_templates.json 读取预设模板
 * 每个模板包含名称与若干任务标题
 */
public class TemplateStore {

    /** 模板项 */
    public static class Template {
        public final String name;
        public final List<String> tasks;

        public Template(String name, List<String> tasks) {
            this.name = name;
            this.tasks = tasks;
        }
    }

    /** 加载全部模板 */
    public static List<Template> load(Context context) {
        List<Template> list = new ArrayList<>();
        try (InputStream is = context.getAssets().open("task_templates.json")) {
            byte[] buf = new byte[is.available()];
            int n = is.read(buf);
            String json = new String(buf, 0, Math.max(0, n), StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.getJSONArray("templates");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject t = arr.getJSONObject(i);
                String name = t.getString("name");
                JSONArray taskArr = t.getJSONArray("tasks");
                List<String> tasks = new ArrayList<>();
                for (int j = 0; j < taskArr.length(); j++) {
                    tasks.add(taskArr.getString(j));
                }
                list.add(new Template(name, tasks));
            }
        } catch (Exception e) {
            // 解析失败返回空列表
        }
        return list;
    }
}
