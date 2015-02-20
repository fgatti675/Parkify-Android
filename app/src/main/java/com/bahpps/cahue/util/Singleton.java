package com.bahpps.cahue.util;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bahpps.cahue.login.AuthUtils;
import com.bahpps.cahue.login.GCMUtil;

import org.apache.http.client.methods.HttpPost;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by francesco on 14.01.2015.
 */
public class Singleton {

    public static final String AUTH_HEADER = "Authorization";
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

    private static Singleton mInstance;

    private RequestQueue mRequestQueue;
    private static Context mContext;

    private Singleton(Context context) {
        mContext = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized Singleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Singleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }
        return mRequestQueue;
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
