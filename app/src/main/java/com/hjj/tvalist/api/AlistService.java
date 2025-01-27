package com.hjj.tvalist.api;

import com.hjj.tvalist.model.AlistResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AlistService {
    @POST("/api/auth/login")
    Call<AlistResponse> login(@Body LoginRequest request);

    @POST("/api/fs/list")
    Call<AlistResponse> listFiles(@Body ListRequest request);

    @POST("/api/fs/get")
    Call<AlistResponse> getFile(@Body GetRequest request);

    class LoginRequest {
        public String username;
        public String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    class ListRequest {
        public String path;
        public String password;
        public int page;
        public int per_page;
        public boolean refresh;

        public ListRequest(String path) {
            this.path = path;
            this.password = "";
            this.page = 1;
            this.per_page = 30;
            this.refresh = false;
        }
    }

    class GetRequest {
        public String path;
        public String password;

        public GetRequest(String path) {
            this.path = path;
            this.password = "";
        }
    }
} 