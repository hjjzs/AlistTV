package com.hjj.tvalist.model;

import java.util.List;

public class AlistResponse {
    public int code;
    public String message;
    public Data data;

    public static class Data {
        // 用于登录响应
        public String token;
        // 用于文件列表响应
        public List<Content> content;
        public String total;
        public String raw_url;    // 文件的真实URL
        public String created;
        public String modified;
        public String provider;
        public long size;
        public String thumb;
        public int type;
        public String sign;
    }

    public static class Content {
        public String name;
        public String size;
        public boolean is_dir;
        public String modified;
        public String sign;
        public String thumb;
        public String type;
    }
} 

