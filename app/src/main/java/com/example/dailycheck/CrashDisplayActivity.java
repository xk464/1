package com.example.dailycheck;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 崩溃展示页：当应用发生未捕获异常时，将异常信息显示在此页面
 * 便于用户截图反馈具体的崩溃堆栈
 */
public class CrashDisplayActivity extends AppCompatActivity {

    public static final String EXTRA_CRASH_INFO = "crash_info";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        String info = getIntent().getStringExtra(EXTRA_CRASH_INFO);
        TextView tv = findViewById(R.id.tv_crash_info);
        tv.setText(info != null ? info : "(无错误信息)");
    }
}
