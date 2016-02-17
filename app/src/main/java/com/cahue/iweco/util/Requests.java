package com.cahue.iweco.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.cahue.iweco.R;
import com.cahue.iweco.auth.Authenticator;
import com.cahue.iweco.login.GCMUtil;

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

    private static final int RETRIES = 5;
    private static final int BACKOFF_MULTIPLIER = 2;

    private static final String AUTH_HEADER = "Authorization";
    private static final String DEVICE_HEADER = "Device";

    @NonNull
    private static Map<String, String> generateHeaders(@NonNull Context context) {

        Map<String, String> headers = new HashMap<>();

        AccountManager accountManager = AccountManager.get(context);
        final Account availableAccounts[] = accountManager.getAccountsByType(context.getString(R.string.account_type));

        String authToken = null;
        try {
            if (availableAccounts.length > 0)
                authToken = accountManager.blockingGetAuthToken(availableAccounts[0], Authenticator.AUTH_TOKEN_TYPE, false);
        } catch (@NonNull OperationCanceledException | IOException | AuthenticatorException e) {
            e.printStackTrace();
        }

        if (authToken != null)
            headers.put(AUTH_HEADER, authToken);

        String regId = GCMUtil.getRegistrationId(context);
        if (regId != null)
            headers.put(DEVICE_HEADER, regId);

        return headers;
    }

    /**
     * Post a Form
     */
    public static class JsonPostFormRequest extends JsonObjectRequest {

        private final Context context;

        public JsonPostFormRequest(Context context, String url, String body, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(Method.POST, url, body, listener, errorListener);
            this.context = context;
            setRetryPolicy(new DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    RETRIES,
                    BACKOFF_MULTIPLIER));
        }

        @NonNull
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return generateHeaders(context);
        }
    }

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

        @NonNull
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

        public JsonArrayPostRequest(Context context, String url, @NonNull JSONArray jsonArray, Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
            super(Method.POST, url, jsonArray.toString(), listener, errorListener);
            this.context = context;
            setRetryPolicy(new DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    RETRIES,
                    BACKOFF_MULTIPLIER));
        }

        @NonNull
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return generateHeaders(context);
        }

        // Need this cause we cant extend a standard class, because the cant get json arrays as a parameter
        @NonNull
        @Override
        protected Response<JSONArray> parseNetworkResponse(@NonNull NetworkResponse response) {
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

        @NonNull
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return generateHeaders(context);
        }

        // Need this cause we cant extend a standard class, because the cant get json arrays as a parameter
        @NonNull
        @Override
        protected Response<JSONArray> parseNetworkResponse(@NonNull NetworkResponse response) {
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

        @NonNull
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            return generateHeaders(context);
        }
    }



}
