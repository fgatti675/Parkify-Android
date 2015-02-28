package com.bahpps.cahue.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.bahpps.cahue.auth.Authenticator;
import com.bahpps.cahue.login.GCMUtil;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Francesco on 09/02/2015.
 */
public class Requests {

    public static final int RETRIES = 5;
    public static final int BACKOFF_MULTIPLIER = 2;

    public static final String AUTH_HEADER = "Authorization";
    public static final String DEVICE_HEADER = "Device";

    /**
     * Post a JSONObject
     */
    public static class JsonPostRequest extends JsonObjectRequest {

        private final Context context;

        public JsonPostRequest(Context context, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(url, jsonRequest, listener, errorListener);
            this.context = context;
            setRetryPolicy(new DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    RETRIES,
                    BACKOFF_MULTIPLIER));
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return generateHeaders(context);
        }
    }

    /**
     * Post a JSONArray and receive an array too
     */
    public static class JsonArrayPostRequest extends JsonRequest<JSONArray> {

        private final Context context;

        public JsonArrayPostRequest(Context context, String url, JSONArray jsonArray, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
            super(Method.POST, url, jsonArray.toString(), listener, errorListener);
            this.context = context;
            setRetryPolicy(new DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    RETRIES,
                    BACKOFF_MULTIPLIER));
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return generateHeaders(context);
        }

        // Need this cause we cant extend a standard class, because the cant get json arrays as a parameter
        @Override
        protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString =
                        new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                return Response.success(new JSONArray(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }
    }

    /**
     * Post a JSONArray and receive an array too
     */
    public static class JsonArrayGetRequest extends JsonRequest<JSONArray> {

        private final Context context;

        public JsonArrayGetRequest(Context context, String url, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
            super(Method.GET, url, null, listener, errorListener);
            this.context = context;
            setRetryPolicy(new DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    RETRIES,
                    BACKOFF_MULTIPLIER));
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return generateHeaders(context);
        }

        // Need this cause we cant extend a standard class, because the cant get json arrays as a parameter
        @Override
        protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString =
                        new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                return Response.success(new JSONArray(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }
    }

    public static class DeleteRequest extends StringRequest {

        private final Context context;

        public DeleteRequest(Context context, String url, Response.Listener<String> listener,
                             Response.ErrorListener errorListener) {
            super(Method.DELETE, url, listener, errorListener);
            this.context = context;
            setRetryPolicy(new DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    RETRIES,
                    BACKOFF_MULTIPLIER));
        }

        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return generateHeaders(context);
        }
    }

    public static Map<String, String> generateHeaders(Context context) {

        Map<String, String> headers = new HashMap<>();

        AccountManager accountManager = AccountManager.get(context);
        final Account availableAccounts[] = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);

        String authToken = null;
        try {
            if (availableAccounts.length > 0)
                authToken = accountManager.blockingGetAuthToken(availableAccounts[0], Authenticator.AUTH_TOKEN_TYPE, false);
        } catch (OperationCanceledException | IOException | AuthenticatorException e) {
            e.printStackTrace();
        }

        if (authToken != null)
            headers.put(AUTH_HEADER, authToken);

        String regId = GCMUtil.getRegistrationId(context);
        if (regId != null)
            headers.put(DEVICE_HEADER, regId);

        return headers;
    }

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

}
