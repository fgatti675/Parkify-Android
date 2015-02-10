package com.bahpps.cahue.util;

import android.content.Context;
import android.net.Uri;

import com.android.volley.AuthFailureError;
import com.bahpps.cahue.login.AuthUtils;
import com.bahpps.cahue.login.GCMUtil;

import org.apache.http.client.methods.HttpPost;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by francesco on 14.01.2015.
 */
public class CommUtil {

    public static final String AUTH_HEADER = "Authentication";
    public static final String DEVICE_HEADER = "Device";


    @Deprecated
    public static HttpPost createHttpPost(Context context, String url) {

        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        for (Map.Entry<String, String> header : generateHeaders(context).entrySet()) {
            httpPost.addHeader(header.getKey(), header.getValue());
        }

        return httpPost;
    }


    public static Map<String, String> generateHeaders(Context context) {

        Map<String, String> headers = new HashMap<>();

        String oauthToken = AuthUtils.getOauthToken(context);

        if (oauthToken != null)
            headers.put(AUTH_HEADER, oauthToken);

        String regId = GCMUtil.getRegistrationId(context);
        if (regId != null)
            headers.put(DEVICE_HEADER, regId);

        return headers;
    }
}
