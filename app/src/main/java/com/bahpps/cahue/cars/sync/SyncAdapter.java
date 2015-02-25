package com.bahpps.cahue.cars.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import com.bahpps.cahue.cars.CarsSync;
import com.bahpps.cahue.cars.database.CarDatabase;
import com.bahpps.cahue.login.AuthUtils;

/**
 * Sync cars with the server state
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        if(!AuthUtils.isLoggedIn(getContext()))
            return;

        CarsSync.retrieveFromServer(getContext(), CarDatabase.getInstance(getContext()));

        Log.i(TAG, "Sync in progress");

    }


}
