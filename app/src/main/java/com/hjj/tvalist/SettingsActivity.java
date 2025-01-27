package com.hjj.tvalist;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import com.hjj.tvalist.util.SettingsManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends FragmentActivity {
    private SettingsManager settingsManager;
    private EditText usernameEdit;
    private EditText passwordEdit;
    private EditText extensionsEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        settingsManager = new SettingsManager(this);
        
        usernameEdit = findViewById(R.id.username_edit);
        passwordEdit = findViewById(R.id.password_edit);
        extensionsEdit = findViewById(R.id.extensions_edit);
        Button saveButton = findViewById(R.id.save_button);
        
        // 加载现有设置
        loadCurrentSettings();
        
        saveButton.setOnClickListener(v -> saveSettings());
    }
    
    private void loadCurrentSettings() {
        usernameEdit.setText(settingsManager.getUsername());
        passwordEdit.setText(settingsManager.getPassword());
        
        // 将扩展名集合转换为逗号分隔的字符串
        String extensions = String.join(",", settingsManager.getVideoExtensions());
        extensionsEdit.setText(extensions);
    }
    
    private void saveSettings() {
        String username = usernameEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();
        String extensionsStr = extensionsEdit.getText().toString().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 保存认证信息
        settingsManager.saveCredentials(username, password);
        
        // 处理并保存视频扩展名
        String[] extensionsArray = extensionsStr.split(",");
        Set<String> extensions = new HashSet<>();
        for (String ext : extensionsArray) {
            String trimmed = ext.trim();
            if (!trimmed.isEmpty()) {
                if (!trimmed.startsWith(".")) {
                    trimmed = "." + trimmed;
                }
                extensions.add(trimmed.toLowerCase());
            }
        }
        
        if (!extensions.isEmpty()) {
            settingsManager.saveVideoExtensions(extensions);
        }
        
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish();
    }
} 