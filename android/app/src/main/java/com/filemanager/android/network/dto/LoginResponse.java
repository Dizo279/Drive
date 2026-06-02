package com.filemanager.android.network.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("accessToken")
    private String token;

    @SerializedName("username")
    private String username;

    public String getToken() { return token; }
    public String getUsername() { return username; }
}