package com.bahpps.cahue.util;

import android.content.Context;

import com.android.volley.Request;
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

    private static CommUtil mInstance;

    private RequestQueue mRequestQueue;
    private static Context mContext;

    private CommUtil(Context context) {
        mContext = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized CommUtil getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CommUtil(context);
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

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
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
