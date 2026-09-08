package com.example.dailycheck;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.dailycheck.databinding.ActivityMainBinding;
import com.example.dailycheck.notify.NotifyHelper;

/**
 * 主界面：底部导航承载打卡 / 学习 / 记账 / 统计 / 日历 / 设置六个模块
 * 支持从通知点击后跳转到指定 Tab
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 通过 NavHostFragment 获取 NavController（FragmentContainerView 的推荐方式）
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        } else {
            Log.e("MainActivity", "NavHostFragment not found, cannot setup navigation");
            return;
        }

        // 仅将底部导航与 NavController 绑定（主题为 NoActionBar，无需 setupActionBarWithNavController）
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
        if (intent == null || navController == null) return;
        if (!intent.hasExtra(NotifyHelper.EXTRA_OPEN_TAB)) return;
        int tab = intent.getIntExtra(NotifyHelper.EXTRA_OPEN_TAB, NotifyHelper.TAB_TASKS);
        if (tab == NotifyHelper.TAB_TASKS) {
            navController.navigate(R.id.navigation_tasks);
        }
    }
}
