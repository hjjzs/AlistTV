package com.hjj.tvalist;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class CheckForUpdatesActivity extends AppCompatActivity {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/hjjzs/AlistTV/releases/latest";
    private final String currentVersion = "v1.0.8"; // 当前版本

    // 可以用版本
    private TextView latestVersionText;
    private ProgressBar loadingIndicator;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_for_updates);
        TextView versionText = findViewById(R.id.update_status);
        loadingIndicator = findViewById(R.id.update_indicator);
        latestVersionText = findViewById(R.id.latest_version);
        versionText.setText("当前版本: " + currentVersion);

        // 设置背景图片半透明
        findViewById(R.id.check_for_updates).getBackground().setAlpha(240);

        checkForUpdates();
    }

    private void checkForUpdates() {
        new CheckForUpdatesTask().execute();
    }

    private class CheckForUpdatesTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    StringBuilder response = new StringBuilder();
                    int byteRead;
                    while ((byteRead = inputStream.read()) != -1) {
                        response.append((char) byteRead);
                    }
                    JSONObject jsonObject = new JSONObject(response.toString());
                    return jsonObject.getString("tag_name"); // 获取最新版本标签
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String latestVersion) {
            if (latestVersion != null && !latestVersion.equals(currentVersion)) {
                latestVersionText.setText("最新版本: " + latestVersion);
                // 显示下载按钮
                findViewById(R.id.start_updates).setVisibility(View.VISIBLE);
                findViewById(R.id.start_updates).setOnClickListener(v -> {
                    downloadApk("https://www.ghproxy.cn/https://github.com/hjjzs/AlistTV/releases/download/" + latestVersion + "/app-debug.apk");
                });

                // 版本不一致，提示用户下载
                //  new AlertDialog.Builder(CheckForUpdatesActivity.this)
                //          .setTitle("检查更新")
                //          .setMessage("发现新版本 " + latestVersion + "，是否下载？")
                //          .setPositiveButton("下载", (dialog, which) -> {
                //              downloadApk("https://github.com/hjjzs/AlistTV/releases/download/" + latestVersion + "/app-debug.apk");
                //          })
                //          .setNegativeButton("取消", null)
                //          .show();
            } else {
                latestVersionText.setText("最新版本: " + currentVersion);
                Toast.makeText(CheckForUpdatesActivity.this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadApk(String apkUrl) {
        // 显示提示对话框
        new AlertDialog.Builder(this)
            .setTitle("安装提示")
            .setMessage("请确保已在系统设置中允许安装未知来源应用")
            .setPositiveButton("继续", (dialog, which) -> {
                // 开始下载
                new DownloadApkTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, apkUrl);
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private class DownloadApkTask extends AsyncTask<String, Integer, byte[]> {
        @Override
        protected byte[] doInBackground(String... urls) {
            byte[] apkData = null;
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    int totalBytesRead = 0;
                    int contentLength = connection.getContentLength();

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        publishProgress((int) ((totalBytesRead / (float) contentLength) * 100)); // 更新进度
                    }
                    outputStream.close();
                    inputStream.close();
                    apkData = outputStream.toByteArray(); // 返回 APK 的字节数组
                    Log.d(TAG, "下载成功，字节数: " + apkData.length);
                } else {
                    Log.e(TAG, "下载失败，响应码: " + connection.getResponseCode());
                }
            } catch (Exception e) {
                Log.e(TAG, "下载过程中发生错误", e);
            }
            return apkData; // 返回 APK 的字节数组
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            loadingIndicator.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(byte[] apkData) {
            loadingIndicator.setVisibility(View.GONE);
            if (apkData != null) {
                installApk(apkData);
            } else {
                Toast.makeText(CheckForUpdatesActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void installApk(byte[] apkData) {
        try {
            Toast.makeText(CheckForUpdatesActivity.this, "开始安装", Toast.LENGTH_SHORT).show();
            File apkFile = new File(getCacheDir(), "app-debug.apk"); // 使用缓存目录
            FileOutputStream fos = new FileOutputStream(apkFile);
            fos.write(apkData);
            fos.close();

            // 确保文件存在且可读
            if (!apkFile.exists() || apkFile.length() == 0) {
                Toast.makeText(this, "APK 文件不存在或为空", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri apkUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (IOException e) {
            Toast.makeText(this, "安装失败", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
} 