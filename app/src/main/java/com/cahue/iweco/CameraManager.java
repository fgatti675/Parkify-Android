package com.cahue.iweco;

import com.google.android.gms.maps.CameraUpdate;

/**
 * Interface used by delegates to update the camera location
 */
public interface CameraManager {


    void registerCameraUpdater(CameraUpdateRequester cameraUpdateRequester);


    void unregisterCameraUpdater(CameraUpdateRequester cameraUpdateRequester);

    /**
     *
     * @param cameraUpdate
     * @param cameraUpdateRequester
     */
    void onCameraUpdateRequest(CameraUpdate cameraUpdate, CameraUpdateRequester cameraUpdateRequester);

}
