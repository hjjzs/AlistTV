package com.hjj.tvalist.util;

import android.content.Context;
import java.util.Arrays;
import java.util.List;

public class FileUtils {
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
        ".mkv", ".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".mpeg", ".mpg"
    );

    private static SettingsManager settingsManager;
    
    public static void init(Context context) {
        settingsManager = new SettingsManager(context);
    }

    public static boolean isVideoFile(String filename) {
        if (settingsManager == null) {
            throw new IllegalStateException("FileUtils not initialized");
        }
        String lowerCase = filename.toLowerCase();
        return settingsManager.getVideoExtensions().stream()
            .anyMatch(ext -> lowerCase.endsWith(ext));
    }

    public static String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
        return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
    
    
} 