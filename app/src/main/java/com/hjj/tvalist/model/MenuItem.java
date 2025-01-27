package com.hjj.tvalist.model;

public class MenuItem {
    private String title;
    private int iconResId;
    private Runnable action;

    public MenuItem(String title, int iconResId, Runnable action) {
        this.title = title;
        this.iconResId = iconResId;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void performAction() {
        if (action != null) {
            action.run();
        }
    }
} 