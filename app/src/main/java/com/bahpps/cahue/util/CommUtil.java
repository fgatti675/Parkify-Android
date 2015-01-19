package com.bahpps.cahue.util;

import android.content.Context;
import android.net.Uri;

import org.apache.http.client.methods.HttpPost;

/**
 * Created by francesco on 14.01.2015.
 */
public class CommUtil {

    public static final String GOOGLE_AUTH_HEADER = "GoogleAuth";

    public static HttpPost createHttpPost(Context context, String url) {

        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");

        String oauthToken = Util.getOauthToken(context);
        if (oauthToken != null)
            httpPost.setHeader(GOOGLE_AUTH_HEADER, oauthToken);

        return httpPost;
    }
}
