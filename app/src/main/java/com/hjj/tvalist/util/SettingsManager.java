package com.hjj.tvalist.util;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SettingsManager {
    private static final String PREFS_NAME = "AlistSettings";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_VIDEO_EXTENSIONS = "video_extensions";
    
    private final SharedPreferences prefs;
    
    public SettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveCredentials(String username, String password) {
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply();
    }
    
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "tv");
    }
    
    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, "Abc@1234!");
    }
    
    public void saveVideoExtensions(Set<String> extensions) {
        prefs.edit().putStringSet(KEY_VIDEO_EXTENSIONS, extensions).apply();
    }
    
    public Set<String> getVideoExtensions() {
        Set<String> defaultExtensions = new HashSet<>(Arrays.asList(
            ".mkv", ".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".mpeg", ".mpg"
        ));
        return prefs.getStringSet(KEY_VIDEO_EXTENSIONS, defaultExtensions);
    }
} 