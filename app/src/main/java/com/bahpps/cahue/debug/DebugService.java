package com.bahpps.cahue.debug;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.bahpps.cahue.locationServices.CarMovedService;

import org.apache.http.client.methods.HttpPost;

/**
* Created by Francesco on 17/10/2014.
*/
public class DebugService extends CarMovedService {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    ServiceListener serviceListener;

    @Override
    public void onLocationPolled(Context context, Location location, String id, String name) {
        super.onLocationPolled(context,location,id, name);
        serviceListener.onNewLocation(location);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        DebugService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DebugService.this;
        }
    }

    public void setServiceListener(ServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("debug", "onBind");
        start();
        return mBinder;
    }

    @Override
    protected boolean checkPreconditions(String id) {
        return true;
    }

    @Override
    public void onLocationPost(HttpPost post){
        serviceListener.onLocationPost(post);
    }

    public interface ServiceListener{
        public void onNewLocation(Location location);
        public void onLocationPost(HttpPost post);
    }
}
