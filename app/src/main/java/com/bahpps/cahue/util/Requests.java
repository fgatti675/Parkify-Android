package com.bahpps.cahue.util;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by Francesco on 09/02/2015.
 */
public class Requests {

    public static final int RETRIES = 5;
    public static final int BACKOFF_MULTIPLIER = 2;


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
            return Singleton.generateHeaders(context);
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
            return Singleton.generateHeaders(context);
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
            return Singleton.generateHeaders(context);
        }
    }

}
