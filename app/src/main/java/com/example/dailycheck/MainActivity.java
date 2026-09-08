package com.example.dailycheck;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.dailycheck.databinding.ActivityMainBinding;
import com.example.dailycheck.notify.NotifyHelper;

/**
 * 主界面：底部导航承载打卡 / 学习 / 记账 / 统计 / 日历 / 设置六个模块
 * 支持从通知点击后跳转到指定 Tab
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 使用 Navigation 组件管理底部导航
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        // 顶层目的地不显示返回箭头
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_tasks,
                R.id.navigation_study,
                R.id.navigation_expense,
                R.id.navigation_stats,
                R.id.navigation_calendar,
                R.id.navigation_settings
        ).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        handleOpenTabIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenTabIntent(intent);
    }

    /** 解析通知传入的 extra，自动切换到指定 Tab */
    private void handleOpenTabIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(NotifyHelper.EXTRA_OPEN_TAB)) return;
        int tab = intent.getIntExtra(NotifyHelper.EXTRA_OPEN_TAB, NotifyHelper.TAB_TASKS);
        if (tab == NotifyHelper.TAB_TASKS) {
            navController.navigate(R.id.navigation_tasks);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
