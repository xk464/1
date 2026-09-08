package com.example.dailycheck;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dailycheck.databinding.ActivityLoginBinding;

/**
 * 登录页：应用入口，验证用户身份后进入主界面
 * 使用 SharedPreferences 存储登录状态，首次登录即注册
 */
public class LoginActivity extends AppCompatActivity {

    private static final String PREFS = "daily_check_login";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_LOGGED_IN = "logged_in";

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 已登录则直接进入主界面
        if (isLoggedIn()) {
            goMain();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private boolean isLoggedIn() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        return sp.getBoolean(KEY_LOGGED_IN, false);
    }

    private void attemptLogin() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            binding.etUsername.setError("请输入用户名");
            binding.etUsername.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("请输入密码");
            binding.etPassword.requestFocus();
            return;
        }
        if (password.length() < 4) {
            binding.etPassword.setError("密码至少4位");
            binding.etPassword.requestFocus();
            return;
        }

        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedUser = sp.getString(KEY_USERNAME, null);
        String savedPass = sp.getString(KEY_PASSWORD, null);

        if (savedUser == null) {
            // 首次登录：注册
            sp.edit()
                    .putString(KEY_USERNAME, username)
                    .putString(KEY_PASSWORD, password)
                    .putBoolean(KEY_LOGGED_IN, true)
                    .apply();
            Toast.makeText(this, "注册成功，已登录", Toast.LENGTH_SHORT).show();
            goMain();
        } else {
            // 已有账号：验证
            if (username.equals(savedUser) && password.equals(savedPass)) {
                sp.edit().putBoolean(KEY_LOGGED_IN, true).apply();
                goMain();
            } else {
                Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
