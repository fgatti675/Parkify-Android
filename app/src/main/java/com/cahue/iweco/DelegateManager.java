package com.cahue.iweco;

import com.google.android.gms.maps.CameraUpdate;

/**
 * Interface used by delegates to update the camera location
 */
public interface DelegateManager {

    void registerDelegate(AbstractMarkerDelegate delegate);

    void unregisterDelegate(AbstractMarkerDelegate delegate);

    void registerCameraUpdateRequester(CameraUpdateRequester cameraUpdateRequester);


    void unregisterCameraUpdateRequester(CameraUpdateRequester cameraUpdateRequester);

    /**
     *
     * @param cameraUpdate
     * @param cameraUpdateRequester
     */
    void doCameraUpdate(CameraUpdate cameraUpdate, CameraUpdateRequester cameraUpdateRequester);

}
