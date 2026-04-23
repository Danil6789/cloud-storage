package com.example.cloud_storage.constant;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class ApiPath {
    public static final String AUTH_SIGN_UP_URL = "/sign-up";
    public static final String AUTH_SIGN_IN_URL = "/sign-in";
    public static final String AUTH_SIGN_OUT_URL = "/sign-out";

    public static final String DOWNLOAD_RESOURCE = "/download";
    public static final String MOVE_RESOURCE = "/move";
    public static final String FIND_RESOURCE = "/search";
}
