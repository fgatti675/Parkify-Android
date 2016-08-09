package com.cahue.iweco.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.login.LoginActivity;

import java.util.concurrent.ExecutionException;

/**
* Created by Francesco on 25/02/2015.
*/
public class Authenticator extends AbstractAccountAuthenticator {


    public static final String AUTH_TOKEN_TYPE = "iweco_cars";
    public static final String USER_ID = "user_id";
    public static final String LOGIN_TYPE = "login_type";
    public static final String LOGIN_DATE = "login_time";
    private static final String ARG_ACCOUNT_TYPE = "arg_account_type";
    private static final String ARG_AUTH_TOKEN_TYPE = "arg_auth_token_type";
    private final ParkifyAccountService parkifyAccountService;
    private final Context mContext;

    public Authenticator(ParkifyAccountService parkifyAccountService, Context context) {
        super(context);
        this.parkifyAccountService = parkifyAccountService;
        this.mContext = context;
    }

    @NonNull
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                 String s) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType,
                             String authTokenType,
                             String[] requiredFeatures,
                             Bundle options) throws NetworkErrorException {

        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(ARG_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;

    }

    @Nullable
    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                     Account account, Bundle bundle)
            throws NetworkErrorException {
        return null;
    }

    @NonNull
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, @NonNull Account account, String authTokenType, Bundle options) throws NetworkErrorException {

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);
        String userId = am.getUserData(account, USER_ID);

        String authToken = am.peekAuthToken(account, authTokenType);

        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            final String password = am.getPassword(account);
            if (password != null) {
                authToken = refresh(userId, password);
            }
        }

        // If we get an authToken - we return it. Everything is fine
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(ARG_AUTH_TOKEN_TYPE, authTokenType);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    /**
     * Send a refresh request in order to get a new auth token
     * @param userId
     * @param refreshToken
     * @return
     */
    private String refresh(String userId, String refreshToken) {

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("users")
                .appendPath("refresh")
                .appendQueryParameter("user", userId)
                .appendQueryParameter("refreshToken", refreshToken);

        RequestFuture<String> future = RequestFuture.newFuture();
        StringRequest request = new StringRequest(builder.build().toString(), future, future);
        ParkifyApp.getParkifyApp().getRequestQueue().add(request);

        try {
            return future.get(); // this will block
        } catch (@NonNull InterruptedException | ExecutionException e) {
            // exception handling
            return null;
        }
    }

    @NonNull
    @Override
    public String getAuthTokenLabel(String s) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse accountAuthenticatorResponse,
                                    Account account, String s, Bundle bundle)
            throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse accountAuthenticatorResponse,
                              Account account, String[] strings)
            throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }
}
